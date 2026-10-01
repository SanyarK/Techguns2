package techguns.campaign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.BiomeDictionary;
import techguns.TGBlocks;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.TGPackets;
import techguns.Techguns;
import techguns.campaign.CampaignMission.Anchor;
import techguns.campaign.CampaignMission.Destination;
import techguns.campaign.CampaignMission.ObjectiveType;
import techguns.capabilities.TGCampaignData;
import techguns.entities.npcs.CapturedScientist;
import techguns.entities.npcs.CommanderNPC;
import techguns.entities.npcs.LegionPrisoner;
import techguns.entities.npcs.MilitaryJet;
import techguns.events.MilitaryExpansionEventHandler;
import techguns.packets.PacketCampaignOpenGui;
import techguns.packets.PacketCampaignSync;
import techguns.world.wasteland.ApocalypseWorldData;

/**
 * Server side logic of the story campaign: accepting missions, objective points and their
 * structures, the checks of the ten objective types, turning missions in, rewards and the
 * commander dialog. What a mission is about comes from the table in {@link CampaignMissions}.
 */
public class TGCampaign {

	/** dialog actions sent by the client GUI */
	public static final int ACTION_ACCEPT = 0;
	public static final int ACTION_TURN_IN = 1;
	/** after the campaign: the commander hands out a military contract */
	public static final int ACTION_CONTRACT = 2;

	/** structures are placed when the player gets this close to the objective point */
	public static final double PLACE_DISTANCE = 96.0D;
	/** enemies, captives and targets appear when the player gets this close */
	public static final double ARM_DISTANCE = 72.0D;
	/** talking to the commander in person */
	public static final double COMMANDER_RADIUS = 6.0D;
	/** entity data flag of mobs spawned by the campaign */
	public static final String MOB_TAG = "techguns_campaign";

	/** running defense fights, only in memory */
	protected static final Map<UUID, CampaignBattle> BATTLES = new HashMap<>();
	/** how often the radio in the start bunker already called the player */
	protected static final Map<UUID, Integer> RADIO_CALLS = new HashMap<>();

	protected static final Random RND = new Random();

	/*
	 * ------------------------------------------------- helpers
	 */

	public static boolean isEnabled() {
		return TGConfig.campaignEnabled;
	}

	/**
	 * Campaign objectives always live in the overworld, no matter where the player is right now
	 */
	public static World overworld(EntityPlayerMP player) {
		return player.getServer() != null ? player.getServer().getWorld(0) : player.world;
	}

	public static CampaignWorldData overworldData(EntityPlayerMP player) {
		return CampaignWorldData.get(overworld(player));
	}

	public static void sync(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data != null) {
			TGPackets.network.sendTo(new PacketCampaignSync(data), player);
		}
	}

	public static void chat(EntityPlayer player, String key, Object... args) {
		player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args));
	}

	public static void commanderSays(EntityPlayer player, String key, Object... args) {
		ITextComponent name = new TextComponentTranslation(Techguns.MODID + ".campaign.commander.name");
		name.setStyle(new Style().setColor(TextFormatting.GOLD));
		ITextComponent text = new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args);
		player.sendMessage(name.appendSibling(new TextComponentTranslation(Techguns.MODID + ".campaign.chat.separator")).appendSibling(text));
	}

	public static void radioSays(EntityPlayer player, String key, Object... args) {
		ITextComponent name = new TextComponentTranslation(Techguns.MODID + ".campaign.radio.name");
		name.setStyle(new Style().setColor(TextFormatting.DARK_AQUA));
		ITextComponent text = new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args);
		text.setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
		player.sendMessage(name.appendSibling(text));
	}

	/**
	 * a line of doctor Volkov over the radio
	 */
	public static void volkovSays(EntityPlayer player, String key, Object... args) {
		ITextComponent name = new TextComponentTranslation(Techguns.MODID + ".campaign.volkov.name");
		name.setStyle(new Style().setColor(TextFormatting.AQUA));
		ITextComponent text = new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args);
		player.sendMessage(name.appendSibling(new TextComponentTranslation(Techguns.MODID + ".campaign.chat.separator")).appendSibling(text));
	}

	public static void giveOrDrop(EntityPlayer player, ItemStack stack) {
		if (!player.addItemStackToInventory(stack)) {
			player.world.spawnEntity(new EntityItem(player.world, player.posX, player.posY, player.posZ, stack));
		}
	}

	public static int countItem(EntityPlayer player, @Nullable Item item) {
		if (item == null) {
			return 0;
		}
		int n = 0;
		for (ItemStack stack : player.inventory.mainInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				n += stack.getCount();
			}
		}
		for (ItemStack stack : player.inventory.offHandInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				n += stack.getCount();
			}
		}
		return n;
	}

	public static void consumeItems(EntityPlayer player, Item item, int count) {
		List<ItemStack> stacks = new ArrayList<>(player.inventory.mainInventory);
		stacks.addAll(player.inventory.offHandInventory);
		for (ItemStack stack : stacks) {
			if (count <= 0) {
				return;
			}
			if (!stack.isEmpty() && stack.getItem() == item) {
				int n = Math.min(count, stack.getCount());
				stack.shrink(n);
				count -= n;
			}
		}
	}

	@Nullable
	public static CommanderNPC findCommanderNear(Entity around, double radius) {
		List<CommanderNPC> list = around.world.getEntitiesWithinAABB(CommanderNPC.class, around.getEntityBoundingBox().grow(radius));
		return list.isEmpty() ? null : list.get(0);
	}

	public static double horizontalDistSq(Entity e, BlockPos pos) {
		double dx = e.posX - (pos.getX() + 0.5D);
		double dz = e.posZ - (pos.getZ() + 0.5D);
		return dx * dx + dz * dz;
	}

	/** mission distances times the config percentage */
	public static int scaled(int distance) {
		return Math.max(16, distance * TGConfig.campaignDistancePercent / 100);
	}

	public static boolean isNight(World world) {
		long t = world.getWorldTime() % 24000L;
		return t >= 13000L && t < 23000L;
	}

	/**
	 * a random free spot on the surface around the center
	 */
	public static BlockPos randomSurface(World world, BlockPos center, int minDist, int maxDist) {
		BlockPos best = null;
		for (int i = 0; i < 12; i++) {
			double angle = world.rand.nextDouble() * Math.PI * 2.0D;
			double dist = minDist + world.rand.nextDouble() * (maxDist - minDist);
			int x = MathHelper.floor(center.getX() + 0.5D + Math.cos(angle) * dist);
			int z = MathHelper.floor(center.getZ() + 0.5D + Math.sin(angle) * dist);
			BlockPos top = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
			if (!world.getBlockState(top.down()).getMaterial().isLiquid() && world.isAirBlock(top) && world.isAirBlock(top.up())) {
				return top;
			}
			best = top;
		}
		return best != null ? best : world.getTopSolidOrLiquidBlock(center);
	}

	/**
	 * Spawns a mob of the category, persistent mobs never despawn (garrisons, waves)
	 */
	@Nullable
	public static EntityLiving spawnMob(World world, CampaignTarget target, double x, double y, double z, @Nullable EntityLivingBase attackTarget, boolean persistent) {
		EntityLiving e = target.create(world, world.rand);
		if (e == null) {
			return null;
		}
		e.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0f, 0.0f);
		e.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(e)), null);
		if (persistent) {
			e.enablePersistence();
		}
		e.getEntityData().setBoolean(MOB_TAG, true);
		world.spawnEntity(e);
		if (attackTarget != null) {
			e.setAttackTarget(attackTarget);
		}
		return e;
	}

	/*
	 * ------------------------------------------------- places
	 */

	/**
	 * The start bunker, or the bed / first position of the player in normal worlds
	 */
	public static BlockPos getHome(EntityPlayerMP player) {
		CampaignWorldData wsd = overworldData(player);
		if (wsd.hasHome(player)) {
			return wsd.getHome(player);
		}
		BlockPos home = ApocalypseWorldData.get(overworld(player)).getBunkerPos();
		if (home == null) {
			home = player.getBedLocation(0);
		}
		if (home == null) {
			home = player.world.provider.getDimension() == 0 ? new BlockPos(player) : overworld(player).getSpawnPoint();
		}
		wsd.setHome(player, home);
		return home;
	}

	public static BlockPos anchorPos(EntityPlayerMP player, Anchor anchor) {
		CampaignWorldData wsd = overworldData(player);
		switch (anchor) {
		case POST:
			if (wsd.hasCommandPost(player)) {
				return wsd.getCommandPost(player);
			}
			return getHome(player);
		case HOME:
			return getHome(player);
		case PLAYER:
		default:
			return new BlockPos(player);
		}
	}

	/**
	 * Picks a point in the given direction and distance range around the anchor, avoiding oceans and rivers.
	 * Y is a placeholder, the real height is determined when the structure is placed.
	 */
	public static BlockPos pickPoint(World world, BlockPos anchor, int min, int max, double preferredAngle) {
		int x = anchor.getX();
		int z = anchor.getZ();
		for (int i = 0; i < 24; i++) {
			double angle = i == 0 ? preferredAngle : preferredAngle + (RND.nextDouble() - 0.5D) * Math.PI * Math.min(2.0D, 0.25D * i);
			double dist = min + RND.nextDouble() * Math.max(max - min, 1);
			x = MathHelper.floor(anchor.getX() + Math.cos(angle) * dist);
			z = MathHelper.floor(anchor.getZ() + Math.sin(angle) * dist);
			Biome biome = world.getBiome(new BlockPos(x, 64, z));
			if (biome.getBaseHeight() >= 0.0f && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
					&& !BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
				break;
			}
		}
		return new BlockPos(x, 64, z);
	}

	/**
	 * chat line with distance, direction and coordinates of the point
	 */
	protected static void announcePoint(EntityPlayerMP player, BlockPos pos) {
		double dx = pos.getX() + 0.5D - player.posX;
		double dz = pos.getZ() + 0.5D - player.posZ;
		int dist = (int) Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (-MathHelper.atan2(dx, dz) * (180D / Math.PI));
		int dirIdx = Math.floorMod(Math.round(yaw / 45.0f), 8);
		chat(player, "msg.objective_set", dist, new TextComponentTranslation(Techguns.MODID + ".campaign.gps.dir." + dirIdx), pos.getX(), pos.getZ());
	}

	/*
	 * ------------------------------------------------- dialog
	 */

	public static void openDialog(EntityPlayerMP player, boolean atCommander) {
		if (!isEnabled()) {
			player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.disabled").setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		//a lost quest item that was handed out with the mission is replaced
		if (m != null && m.giveOnAccept && data.getState() == TGCampaignData.STATE_ACTIVE && m.getQuestItem() != null
				&& countItem(player, m.getQuestItem()) == 0) {
			giveOrDrop(player, new ItemStack(m.getQuestItem()));
			commanderSays(player, "msg.resupply");
		}
		sync(player);
		//the objective is only told once in the chat, the radio repeats it (e.g. after logging in again)
		if (data.getState() == TGCampaignData.STATE_ACTIVE && data.hasObjective()) {
			announcePoint(player, data.getObjective());
		}
		TGPackets.network.sendTo(new PacketCampaignOpenGui(data.getMission(), data.getState(), data.getProgress(), atCommander), player);
	}

	public static void handleDialogAction(EntityPlayerMP player, int action) {
		if (!isEnabled()) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		if (data.isFinished()) {
			if (action == ACTION_CONTRACT) {
				giveContract(player);
				openDialog(player, findCommanderNear(player, COMMANDER_RADIUS) != null);
			}
			return;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null) {
			return;
		}
		boolean atCommander = findCommanderNear(player, COMMANDER_RADIUS) != null;

		if (action == ACTION_ACCEPT && data.getState() == TGCampaignData.STATE_OFFERED) {
			if (!m.playable) {
				commanderSays(player, "msg.to_be_continued");
			} else if (player.world.provider.getDimension() != 0) {
				//campaign objectives only exist in the overworld
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.overworld_only").setStyle(new Style().setColor(TextFormatting.RED)));
				return;
			} else {
				acceptMission(player, data, m);
			}
		} else if (action == ACTION_TURN_IN) {
			tryTurnIn(player, data, m, atCommander);
		}
		//refresh the dialog on the client
		openDialog(player, findCommanderNear(player, COMMANDER_RADIUS) != null);
	}

	/**
	 * free play after the campaign: the colonel hands out military contracts in person, one at a time
	 */
	protected static void giveContract(EntityPlayerMP player) {
		if (findCommanderNear(player, COMMANDER_RADIUS) == null) {
			commanderSays(player, "msg.contract_in_person");
		} else if (countItem(player, TGItems.MILITARY_CONTRACT) > 0) {
			commanderSays(player, "msg.contract_have");
		} else {
			giveOrDrop(player, new ItemStack(TGItems.MILITARY_CONTRACT));
			commanderSays(player, "msg.contract_given");
		}
	}

	/*
	 * ------------------------------------------------- mission flow
	 */

	protected static void acceptMission(EntityPlayerMP player, TGCampaignData data, CampaignMission m) {
		stopBattle(player, true);
		data.setState(TGCampaignData.STATE_ACTIVE);
		data.setProgress(0);
		data.setProgress2(0);
		createObjectives(player, m);

		Item item = m.getQuestItem();
		if (m.giveOnAccept && item != null && countItem(player, item) < m.count) {
			giveOrDrop(player, new ItemStack(item, m.count - countItem(player, item)));
		}
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.6f, 1.0f);
		chat(player, "msg.accepted", new TextComponentTranslation(m.getTitleKey()));

		if (m.type == ObjectiveType.TALK && !m.inPerson) {
			//answering the radio call is the whole mission
			data.setState(TGCampaignData.STATE_READY);
			tryTurnIn(player, data, m, true);
			return;
		}
		updateMarker(player, data, m);
		if (data.hasObjective()) {
			announcePoint(player, data.getObjective());
		}
		sync(player);
	}

	/**
	 * Plans the objective points of the mission in the world data, structures are built later on approach
	 */
	protected static void createObjectives(EntityPlayerMP player, CampaignMission m) {
		CampaignWorldData wsd = overworldData(player);
		World world = overworld(player);

		//a failed attempt keeps its places, the enemies and captives come back
		if (wsd.getPointsMission(player) == m.id && !wsd.getPoints(player).isEmpty()) {
			for (CampaignPoint p : wsd.getPoints(player)) {
				if (p.site != CampaignSite.HOME && p.site != CampaignSite.COMMAND_POST) {
					p.armed = false;
				}
			}
			wsd.savePoints(player);
			return;
		}

		List<CampaignPoint> points = new ArrayList<>();
		BlockPos anchor = anchorPos(player, m.anchor);

		if (m.isDefense() && m.site == CampaignSite.NONE) {
			CampaignPoint p = new CampaignPoint(anchor, m.anchor == Anchor.POST ? CampaignSite.COMMAND_POST : CampaignSite.HOME);
			p.placed = true;
			p.armed = true;
			points.add(p);
		} else if (m.site == CampaignSite.COMMAND_POST) {
			CampaignPoint p;
			if (wsd.hasCommandPost(player)) {
				p = new CampaignPoint(wsd.getCommandPost(player), CampaignSite.COMMAND_POST);
				p.placed = true;
				p.armed = true;
			} else {
				p = new CampaignPoint(pickPoint(world, anchor, scaled(m.minDistance), scaled(m.maxDistance), RND.nextDouble() * Math.PI * 2.0D), CampaignSite.COMMAND_POST);
			}
			points.add(p);
		} else if (m.site != CampaignSite.NONE) {
			CampaignPoint remembered = m.siteKey != null ? wsd.getSite(player, m.siteKey) : null;
			if (remembered != null) {
				remembered.armed = false;
				remembered.done = false;
				remembered.targets.clear();
				points.add(remembered);
			} else {
				int n = Math.max(1, m.getSiteCount());
				double base = RND.nextDouble() * Math.PI * 2.0D;
				for (int i = 0; i < n; i++) {
					CampaignSite site = m.getSite(i);
					//missions with several places come back to known places, e.g. the lab
					CampaignPoint known = n > 1 && site.memory != null ? wsd.getSite(player, site.memory) : null;
					if (known != null) {
						known.armed = false;
						known.done = false;
						known.targets.clear();
						points.add(known);
						continue;
					}
					double angle = base + i * Math.PI * 2.0D / n + (RND.nextDouble() - 0.5D) * Math.PI / (n + 1);
					points.add(new CampaignPoint(pickPoint(world, anchor, scaled(m.minDistance), scaled(m.maxDistance), angle), site));
				}
			}
			if (m.type == ObjectiveType.ESCORT && m.destination == Destination.EVAC) {
				//extraction point on the way from the camp back to the post
				BlockPos camp = points.get(0).pos;
				BlockPos post = anchorPos(player, Anchor.POST);
				double dx = post.getX() - camp.getX();
				double dz = post.getZ() - camp.getZ();
				double len = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
				double d = Math.min(scaled(120), len * 0.5D);
				points.add(new CampaignPoint(new BlockPos(camp.getX() + dx / len * d, 64, camp.getZ() + dz / len * d), CampaignSite.EVAC));
			}
		}
		wsd.setPoints(player, m.id, points);
	}

	public static List<CampaignPoint> currentPoints(EntityPlayerMP player, CampaignMission m) {
		CampaignWorldData wsd = overworldData(player);
		return wsd.getPointsMission(player) == m.id ? wsd.getPoints(player) : Collections.<CampaignPoint>emptyList();
	}

	/**
	 * Sets the current mission to ready and tells the player where to turn it in
	 */
	public static void setReady(EntityPlayerMP player, TGCampaignData data, CampaignMission m) {
		data.setState(TGCampaignData.STATE_READY);
		stopBattle(player, false);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.3f);
		commanderSays(player, m.inPerson ? "msg.ready_commander" : "msg.ready_radio");
		updateMarker(player, data, m);
		sync(player);
	}

	protected static void tryTurnIn(EntityPlayerMP player, TGCampaignData data, CampaignMission m, boolean atCommander) {
		if (data.getState() != TGCampaignData.STATE_READY) {
			return;
		}
		if (m.inPerson && !atCommander) {
			commanderSays(player, "msg.come_in_person");
			return;
		}
		if (m.hasComponents()) {
			for (int i = 0; i < m.components.size(); i++) {
				Item component = m.getComponent(i);
				if (component != null && countItem(player, component) == 0) {
					commanderSays(player, "msg.need_item", new TextComponentTranslation(component.getUnlocalizedName() + ".name"), 1);
					data.setState(TGCampaignData.STATE_ACTIVE);
					sync(player);
					return;
				}
			}
			for (int i = 0; i < m.components.size(); i++) {
				Item component = m.getComponent(i);
				if (component != null) {
					consumeItems(player, component, 1);
				}
			}
		}
		Item item = m.getQuestItem();
		int need = 0;
		if (item != null && (m.type == ObjectiveType.COLLECT || m.type == ObjectiveType.ITEM || m.type == ObjectiveType.BOSS)) {
			need = m.type == ObjectiveType.COLLECT ? m.count : 1;
		}
		if (need > 0 && countItem(player, item) < need) {
			commanderSays(player, "msg.need_item", new TextComponentTranslation(item.getUnlocalizedName() + ".name"), need);
			data.setState(TGCampaignData.STATE_ACTIVE);
			sync(player);
			return;
		}
		if (need > 0) {
			consumeItems(player, item, need);
		}
		completeMission(player, data, m);
	}

	/**
	 * rewards and the next mission
	 */
	protected static void completeMission(EntityPlayerMP player, TGCampaignData data, CampaignMission m) {
		stopBattle(player, false);
		giveRewards(player, data, m);
		overworldData(player).clearPoints(player);
		data.clearObjective();
		chat(player, "msg.mission_complete", new TextComponentTranslation(m.getTitleKey()), new TextComponentTranslation(m.getRewardKey()));

		CampaignMission next = CampaignMissions.byId(m.id + 1);
		if (next == null) {
			data.setMission(TGCampaignData.LAST_MISSION + 1, TGCampaignData.STATE_OFFERED);
			player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.act_complete", new TextComponentTranslation(m.getActKey()))
					.setStyle(new Style().setColor(TextFormatting.GOLD).setBold(true)));
			CampaignFinale.start(player);
		} else {
			data.setMission(next.id, TGCampaignData.STATE_OFFERED);
			if (next.act != m.act) {
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.act_complete", new TextComponentTranslation(m.getActKey()))
						.setStyle(new Style().setColor(TextFormatting.GOLD).setBold(true)));
			}
			if (next.playable) {
				commanderSays(player, "msg.next_mission", new TextComponentTranslation(next.getTitleKey()));
			} else {
				commanderSays(player, "msg.to_be_continued");
			}
		}
		sync(player);
	}

	/**
	 * the whole server learns about the new Wasteland Hero
	 */
	public static void finishCampaign(EntityPlayerMP player) {
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		if (player.getServer() != null) {
			ITextComponent msg = new TextComponentTranslation(Techguns.MODID + ".campaign.msg.title_awarded", player.getName());
			msg.setStyle(new Style().setColor(TextFormatting.GOLD).setBold(true));
			player.getServer().getPlayerList().sendMessage(msg);
			ITextComponent title = new TextComponentTranslation(Techguns.MODID + ".campaign.msg.hero_title", player.getName(),
					new TextComponentTranslation(Techguns.MODID + ".campaign.hero.title"));
			title.setStyle(new Style().setColor(TextFormatting.YELLOW));
			player.getServer().getPlayerList().sendMessage(title);
		}
	}

	protected static void giveRewards(EntityPlayerMP player, TGCampaignData data, CampaignMission m) {
		CampaignMission.Reward r = m.reward;
		if (r.xp > 0) {
			player.addExperience(r.xp);
		}
		if (r.loot != null && r.lootRolls > 0) {
			WorldServer ws = player.getServerWorld();
			LootTable table = ws.getLootTableManager().getLootTableFromLocation(r.loot);
			LootContext context = new LootContext.Builder(ws).withPlayer(player).build();
			for (int i = 0; i < r.lootRolls; i++) {
				for (ItemStack loot : table.generateLootForPools(ws.rand, context)) {
					giveOrDrop(player, loot);
				}
			}
		}
		for (Supplier<ItemStack> supplier : r.items) {
			ItemStack stack = supplier.get();
			if (stack.isEmpty()) {
				continue;
			}
			//one radio is enough
			if (stack.getItem() == TGItems.RADIO) {
				data.setRadioGiven(true);
				if (countItem(player, TGItems.RADIO) > 0) {
					continue;
				}
			}
			giveOrDrop(player, stack);
		}
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	/*
	 * ------------------------------------------------- per second update of a player in the overworld
	 */

	public static void tickPlayer(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		World world = player.world;
		CampaignWorldData wsd = CampaignWorldData.get(world);
		if (data.isFinished()) {
			//free play: the colonel and the settlers stay at the post
			respawnCommanderCheck(player, wsd);
			CampaignFinale.settlersCheck(player, wsd);
			return;
		}

		if (data.getMission() == TGCampaignData.FIRST_MISSION && data.getState() == TGCampaignData.STATE_OFFERED) {
			radioCall(player);
		}

		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null || !m.playable) {
			respawnCommanderCheck(player, wsd);
			return;
		}
		List<CampaignPoint> points = currentPoints(player, m);

		//build the structures and man them when the player gets close
		boolean dirty = false;
		for (int i = 0; i < points.size(); i++) {
			CampaignPoint p = points.get(i);
			double d = horizontalDistSq(player, p.pos);
			if (!p.placed && d < PLACE_DISTANCE * PLACE_DISTANCE) {
				CampaignSiteBuilder.place(world, wsd, player, p, RND);
				String key = m.siteKey != null ? m.siteKey : p.site.memory;
				if (key != null && p.site.persistent) {
					wsd.setSite(player, key, p);
				}
				chat(player, "msg.objective_spotted");
				dirty = true;
				break;
			}
			if (p.placed && !p.armed && data.getState() == TGCampaignData.STATE_ACTIVE && d < ARM_DISTANCE * ARM_DISTANCE) {
				CampaignSiteBuilder.arm(world, player, m, p, i, RND);
				dirty = true;
			}
		}
		if (dirty) {
			wsd.savePoints(player);
		}

		if (data.getState() == TGCampaignData.STATE_ACTIVE) {
			if (m.type == ObjectiveType.ESCORT && m.destination == Destination.SITE) {
				caravanCheck(player, wsd);
			}
			checkObjective(player, data, m, points);
			//the caravan is only ambushed on the way
			boolean onTheWay = m.type != ObjectiveType.ESCORT || m.destination != Destination.SITE || hasFollowingCaptive(player);
			if (m.raids != null && !m.isDefense() && onTheWay && data.getState() == TGCampaignData.STATE_ACTIVE
					&& player.ticksExisted % (20 * Math.max(m.raidInterval, 5)) == 0) {
				spawnRaid(player, m);
			}
		}
		if (player.ticksExisted % 60 == 0) {
			updateMarker(player, data, m);
		}
		respawnCommanderCheck(player, wsd);
	}

	protected static void checkObjective(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		World world = player.world;
		switch (m.type) {
		case TALK:
			if (findCommanderNear(player, 8.0D) != null) {
				setReady(player, data, m);
			}
			break;
		case REACH:
			for (CampaignPoint p : points) {
				if (p.placed && horizontalDistSq(player, p.pos) < p.site.reachRadius * p.site.reachRadius
						&& (p.reachY == Integer.MIN_VALUE || player.posY <= p.reachY + 0.5D)) {
					setReady(player, data, m);
					return;
				}
			}
			break;
		case KILL:
			if (m.site != CampaignSite.NONE && m.target != null && player.ticksExisted % 600 == 0) {
				reinforce(player, points, m.target, m.count - data.getProgress());
			}
			break;
		case COLLECT:
			if (m.hasComponents()) {
				checkComponents(player, data, m, points);
			} else if (m.getQuestItem() != null) {
				int have = Math.min(countItem(player, m.getQuestItem()), m.count);
				if (have != data.getProgress()) {
					data.setProgress(have);
					sync(player);
				}
				if (have >= m.count) {
					setReady(player, data, m);
				} else if (m.dropFrom != null && m.site != CampaignSite.NONE && player.ticksExisted % 600 == 0) {
					//new carriers of the samples come when the zone ran empty
					reinforce(player, points, m.dropFrom, Math.min(6, (m.count - have) * 2));
				}
			}
			break;
		case DELIVER:
			checkDelivery(player, data, m, points);
			break;
		case ESCORT:
			checkEscort(player, data, m, points);
			break;
		case DEFEND:
		case SURVIVE:
			tickBattle(player, data, m, points);
			break;
		case DESTROY:
			checkTargets(player, data, m, points);
			if (m.target2 != null && data.getProgress2() < m.count2 && player.ticksExisted % 600 == 0) {
				reinforce(player, points, m.target2, m.count2 - data.getProgress2());
			}
			break;
		case BOSS:
			checkBoss(player, data, m, points);
			break;
		case ITEM:
			if (countItem(player, m.getQuestItem()) > 0) {
				setReady(player, data, m);
			}
			break;
		default:
			break;
		}
	}

	/**
	 * COLLECT with components: one quest item per place, progress is the number of different items the player carries
	 */
	protected static void checkComponents(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		int have = 0;
		boolean changed = false;
		for (int i = 0; i < m.components.size(); i++) {
			Item item = m.getComponent(i);
			boolean found = item != null && countItem(player, item) > 0;
			if (found) {
				have++;
			}
			if (i < points.size() && points.get(i).done != found) {
				points.get(i).done = found;
				changed = true;
			}
		}
		if (changed) {
			overworldData(player).savePoints(player);
		}
		if (have != data.getProgress()) {
			if (have > data.getProgress() && have < m.count) {
				commanderSays(player, "msg.component_found", have, m.count);
			}
			data.setProgress(have);
			updateMarker(player, data, m);
			sync(player);
		}
		if (have >= m.count) {
			setReady(player, data, m);
		}
	}

	protected static void checkDelivery(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		Item item = m.getQuestItem();
		for (CampaignPoint p : points) {
			if (!p.placed || p.done || p.poi == null) {
				continue;
			}
			if (horizontalDistSq(player, p.poi) < 3.0D * 3.0D && player.posY >= p.poi.getY() - 0.5D && player.posY < p.poi.getY() + 4.0D) {
				if (item != null && countItem(player, item) == 0) {
					player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.need_parts").setStyle(new Style().setColor(TextFormatting.RED)), true);
					continue;
				}
				if (item != null) {
					consumeItems(player, item, 1);
				}
				p.done = true;
				CampaignSiteBuilder.onDelivered(player.world, p);
				overworldData(player).savePoints(player);
				data.setProgress(data.getProgress() + 1);
				commanderSays(player, "msg.delivered");
				if (data.getProgress() >= m.count) {
					setReady(player, data, m);
				} else {
					sync(player);
				}
			}
		}
	}

	protected static void checkEscort(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		List<CapturedScientist> list = player.world.getEntitiesWithinAABB(CapturedScientist.class, player.getEntityBoundingBox().grow(48.0D));
		for (CapturedScientist c : list) {
			if (!c.isRescuer(player) || !c.isEntityAlive() || c.isDelivered()) {
				continue;
			}
			boolean arrived;
			switch (m.destination) {
			case EVAC:
				arrived = points.size() > 1 && points.get(1).placed && horizontalDistSq(c, points.get(1).pos) < 10.0D * 10.0D;
				break;
			case SITE:
				arrived = !points.isEmpty() && horizontalDistSq(c, points.get(0).pos) < 12.0D * 12.0D;
				break;
			case POST:
			default:
				arrived = findCommanderNear(c, 10.0D) != null;
				break;
			}
			if (!arrived) {
				continue;
			}
			c.setDelivered();
			c.stayHere();
			if (m.destination == Destination.SITE) {
				//doctor Volkov prepares the Purifier and stays at the launch pad
				c.protect();
			}
			data.setProgress(data.getProgress() + 1);
			if (m.destination == Destination.EVAC) {
				//picked up by the Resistance
				if (player.world instanceof WorldServer) {
					((WorldServer) player.world).spawnParticle(EnumParticleTypes.CLOUD, c.posX, c.posY + 1.0D, c.posZ, 20, 0.4D, 0.8D, 0.4D, 0.02D);
				}
				c.setDead();
				commanderSays(player, "msg.captive_evacuated", data.getProgress(), m.count);
			} else if (m.destination == Destination.SITE) {
				volkovSays(player, "msg.volkov_arrived");
			} else {
				commanderSays(player, "msg.captive_delivered");
			}
			if (data.getProgress() >= m.count) {
				setReady(player, data, m);
				return;
			}
			sync(player);
		}
	}

	protected static void checkTargets(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		int destroyed = 0;
		boolean known = false;
		for (CampaignPoint p : points) {
			if (!p.armed || p.targets.isEmpty() || horizontalDistSq(player, p.pos) > 192.0D * 192.0D) {
				continue;
			}
			known = true;
			for (BlockPos t : p.targets) {
				if (player.world.isBlockLoaded(t) && player.world.getBlockState(t).getBlock() != TGBlocks.CAMPAIGN_TARGET) {
					destroyed++;
				}
			}
		}
		if (known && destroyed > data.getProgress()) {
			data.setProgress(Math.min(destroyed, m.count));
			commanderSays(player, "msg.target_destroyed", data.getProgress(), m.count);
			sync(player);
		}
		if (data.getProgress() >= m.count && (m.target2 == null || data.getProgress2() >= m.count2)) {
			setReady(player, data, m);
		}
	}

	protected static void checkBoss(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		Item item = m.getQuestItem();
		if (data.getProgress() >= m.count) {
			if (item == null || countItem(player, item) > 0) {
				setReady(player, data, m);
				return;
			}
			//the trophy was not picked up (lava, despawned...), the player finds it on the body after a while
			data.setProgress2(data.getProgress2() + 1);
			if (data.getProgress2() >= 30) {
				giveOrDrop(player, new ItemStack(item));
				commanderSays(player, "msg.boss_item_found");
			}
			return;
		}
		//the boss has to be there while the player is near his lair
		if (player.ticksExisted % 100 != 0 || m.target == null || points.isEmpty()) {
			return;
		}
		CampaignPoint p = points.get(0);
		if (!p.armed) {
			return;
		}
		BlockPos spot = p.poi != null ? p.poi : p.pos.up();
		if (horizontalDistSq(player, spot) > 48.0D * 48.0D) {
			return;
		}
		List<EntityLivingBase> list = player.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(spot).grow(64.0D, 32.0D, 64.0D), e -> m.target.matches(e));
		if (list.isEmpty()) {
			CampaignSiteBuilder.spawnBoss(player.world, m.target, spot);
		}
	}

	/**
	 * sends the missing enemies of a kill objective when the site ran empty
	 */
	protected static void reinforce(EntityPlayerMP player, List<CampaignPoint> points, CampaignTarget target, int missing) {
		if (missing <= 0 || points.isEmpty() || !points.get(0).armed) {
			return;
		}
		BlockPos center = points.get(0).pos;
		if (horizontalDistSq(player, center) > 48.0D * 48.0D) {
			return;
		}
		List<EntityLivingBase> list = player.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(center).grow(64.0D, 32.0D, 64.0D), e -> target.matches(e));
		if (!list.isEmpty()) {
			return;
		}
		for (int i = 0; i < missing; i++) {
			BlockPos pos = randomSurface(player.world, center, 4, 12);
			spawnMob(player.world, target, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, null, true);
		}
		chat(player, "msg.reinforcements");
	}

	/**
	 * raiders and jets hunt the player during some missions
	 */
	protected static void spawnRaid(EntityPlayerMP player, CampaignMission m) {
		World world = player.world;
		if (m.area > 0 && horizontalDistSq(player, anchorPos(player, m.anchor)) > m.area * m.area) {
			return;
		}
		if (m.raids == CampaignTarget.JET) {
			if (!world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ))) {
				return;
			}
			if (!world.getEntitiesWithinAABB(MilitaryJet.class, player.getEntityBoundingBox().grow(160.0D, 128.0D, 160.0D)).isEmpty()) {
				return;
			}
			MilitaryExpansionEventHandler.startAirRaid(world, player);
			return;
		}
		List<EntityLivingBase> hostiles = world.getEntitiesWithinAABB(EntityLivingBase.class, player.getEntityBoundingBox().grow(32.0D, 16.0D, 32.0D), e -> e instanceof IMob);
		if (hostiles.size() >= m.raidSize * 2) {
			return;
		}
		for (int i = 0; i < m.raidSize; i++) {
			BlockPos pos = randomSurface(world, new BlockPos(player), 20, 30);
			spawnMob(world, m.raids, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player, false);
		}
		player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.raid").setStyle(new Style().setColor(TextFormatting.RED)), true);
	}

	/*
	 * ------------------------------------------------- defense fights
	 */

	protected static void tickBattle(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		if (points.isEmpty()) {
			return;
		}
		BlockPos center = points.get(0).pos;
		CampaignBattle battle = BATTLES.get(player.getUniqueID());
		if (battle != null && (battle.world != player.world || battle.mission != m)) {
			battle.stop(player, true);
			BATTLES.remove(player.getUniqueID());
			battle = null;
		}
		double d = horizontalDistSq(player, center);
		if (battle == null) {
			if (d > CampaignBattle.START_RADIUS * CampaignBattle.START_RADIUS) {
				return;
			}
			if (m.atNight && !isNight(player.world)) {
				if (player.ticksExisted % 200 == 0) {
					player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.wait_night").setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
				}
				return;
			}
			battle = new CampaignBattle(m, player.world, center);
			BATTLES.put(player.getUniqueID(), battle);
		}
		if (d > CampaignBattle.LEAVE_RADIUS * CampaignBattle.LEAVE_RADIUS) {
			loseBattle(player, data, "battle.left");
			return;
		}
		if (battle.tick(player, data)) {
			if (m.type == ObjectiveType.DEFEND && m.seconds > 0) {
				//the Purifier is charged: its first pulse burns the attackers, the beam turns green
				battle.purge();
				if (points.get(0).poi != null) {
					CampaignStructures.openPurifier(player.world, points.get(0).poi);
				}
				volkovSays(player, "msg.purifier_charged");
			}
			setReady(player, data, m);
		}
	}

	public static void stopBattle(EntityPlayerMP player, boolean removeAttackers) {
		CampaignBattle battle = BATTLES.remove(player.getUniqueID());
		if (battle != null) {
			battle.stop(player, removeAttackers);
		}
	}

	protected static void loseBattle(EntityPlayerMP player, TGCampaignData data, String key) {
		if (BATTLES.containsKey(player.getUniqueID())) {
			stopBattle(player, true);
			data.setProgress(0);
			commanderSays(player, key);
			sync(player);
		}
	}

	/*
	 * ------------------------------------------------- events
	 */

	public static void onEntityKilled(EntityPlayerMP player, EntityLivingBase victim) {
		if (!isEnabled()) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished() || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null || !m.playable) {
			return;
		}
		Item item = m.getQuestItem();
		if (m.dropFrom != null && item != null && m.dropFrom.matches(victim) && victim.world.rand.nextFloat() < m.dropChance) {
			victim.world.spawnEntity(new EntityItem(victim.world, victim.posX, victim.posY + 0.5D, victim.posZ, new ItemStack(item)));
		}

		switch (m.type) {
		case KILL:
			if (m.target != null && m.target.matches(victim)
					&& (m.area <= 0 || horizontalDistSq(victim, anchorPos(player, m.anchor)) < m.area * m.area)) {
				data.setProgress(data.getProgress() + 1);
				if (data.getProgress() >= m.count) {
					setReady(player, data, m);
				} else {
					progressMessage(player, data.getProgress(), m.count);
				}
			}
			break;
		case BOSS:
			if (m.target != null && m.target.matches(victim) && data.getProgress() < m.count) {
				data.setProgress(data.getProgress() + 1);
				data.setProgress2(0);
				if (item != null) {
					EntityItem drop = new EntityItem(victim.world, victim.posX, victim.posY + 0.5D, victim.posZ, new ItemStack(item));
					drop.setGlowing(true);
					victim.world.spawnEntity(drop);
					commanderSays(player, "msg.boss_item");
				}
				if (data.getProgress() >= m.count && item == null) {
					setReady(player, data, m);
				} else {
					sync(player);
				}
			}
			break;
		case DESTROY:
			if (m.target2 != null && m.target2.matches(victim) && data.getProgress2() < m.count2) {
				data.setProgress2(data.getProgress2() + 1);
				player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.garrison", data.getProgress2(), m.count2)
						.setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
				sync(player);
			}
			break;
		default:
			break;
		}
	}

	protected static void progressMessage(EntityPlayerMP player, int progress, int required) {
		player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.progress", progress, required)
				.setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
		sync(player);
	}

	public static void onPlayerDied(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data != null) {
			loseBattle(player, data, "battle.lost");
		}
	}

	/**
	 * right click on a block: survivor stashes are searched by opening them
	 */
	public static void onBlockInteract(EntityPlayerMP player, BlockPos pos) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished() || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null || !m.playable || m.type != ObjectiveType.COLLECT || m.getQuestItem() != null || m.hasComponents()) {
			return;
		}
		List<CampaignPoint> points = currentPoints(player, m);
		for (CampaignPoint p : points) {
			if (!p.done && p.poi != null && p.poi.equals(pos)) {
				p.done = true;
				overworldData(player).savePoints(player);
				data.setProgress(data.getProgress() + 1);
				commanderSays(player, "msg.stash_found", data.getProgress(), m.count);
				if (data.getProgress() >= m.count) {
					setReady(player, data, m);
				} else {
					updateMarker(player, data, m);
					sync(player);
				}
				return;
			}
		}
	}

	public static boolean canFreeCaptive(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return false;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		return m != null && m.playable && m.type == ObjectiveType.ESCORT;
	}

	public static void onCaptiveFreed(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		CampaignMission m = data == null ? null : CampaignMissions.byId(data.getMission());
		if (m != null) {
			updateMarker(player, data, m);
			if (data.hasObjective()) {
				announcePoint(player, data.getObjective());
			}
		}
	}

	/**
	 * A captive died during the escort: the mission starts over, the places stay
	 */
	public static void onCaptiveDied(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null || m.type != ObjectiveType.ESCORT) {
			return;
		}
		//the others are taken back by the Legion
		for (CapturedScientist c : player.world.getEntitiesWithinAABB(CapturedScientist.class, player.getEntityBoundingBox().grow(64.0D))) {
			if (c.isRescuer(player) && !c.isDelivered()) {
				c.setDead();
			}
		}
		data.setMission(m.id, TGCampaignData.STATE_OFFERED);
		data.clearObjective();
		commanderSays(player, "msg.captive_died");
		sync(player);
	}

	/*
	 * ------------------------------------------------- GPS marker
	 */

	public static void updateMarker(EntityPlayerMP player, TGCampaignData data, CampaignMission m) {
		BlockPos target = markerFor(player, data, m, currentPoints(player, m));
		boolean changed = target != null ? data.setObjective(target) : data.clearObjective();
		if (changed) {
			sync(player);
		}
	}

	@Nullable
	protected static BlockPos markerFor(EntityPlayerMP player, TGCampaignData data, CampaignMission m, List<CampaignPoint> points) {
		CampaignWorldData wsd = overworldData(player);
		BlockPos post = wsd.hasCommandPost(player) ? wsd.getCommandPost(player) : null;
		if (data.getState() == TGCampaignData.STATE_OFFERED) {
			return null;
		}
		if (data.getState() == TGCampaignData.STATE_READY) {
			return m.inPerson ? post : null;
		}
		switch (m.type) {
		case TALK:
			return post;
		case KILL:
			if (m.area > 0) {
				return anchorPos(player, m.anchor);
			}
			break;
		case COLLECT:
			if (m.hasComponents()) {
				BlockPos best = null;
				double bestDist = Double.MAX_VALUE;
				for (int i = 0; i < points.size(); i++) {
					Item component = m.getComponent(i);
					if (component != null && countItem(player, component) > 0) {
						continue;
					}
					double d = horizontalDistSq(player, points.get(i).pos);
					if (d < bestDist) {
						bestDist = d;
						best = points.get(i).pos;
					}
				}
				return best != null ? best : (m.inPerson ? post : null);
			}
			if (m.getQuestItem() == null) {
				BlockPos best = null;
				double bestDist = Double.MAX_VALUE;
				for (CampaignPoint p : points) {
					double d = horizontalDistSq(player, p.pos);
					if (!p.done && d < bestDist) {
						bestDist = d;
						best = p.pos;
					}
				}
				return best;
			}
			if (data.getProgress() >= m.count && m.inPerson) {
				return post;
			}
			break;
		case ESCORT:
			if (hasFollowingCaptive(player)) {
				if (m.destination == Destination.EVAC && points.size() > 1) {
					return points.get(1).pos;
				}
				if (m.destination == Destination.POST) {
					return post;
				}
			} else if (m.destination == Destination.SITE) {
				return post;
			}
			break;
		default:
			break;
		}
		return points.isEmpty() ? null : points.get(0).pos;
	}

	/**
	 * ESCORT to the site: doctor Volkov waits with the components at the command post. The Volkov
	 * rescued in act II joins the caravan when he is still there, otherwise he arrives.
	 */
	protected static void caravanCheck(EntityPlayerMP player, CampaignWorldData wsd) {
		if (player.ticksExisted % 100 != 0 || !wsd.hasCommandPost(player)) {
			return;
		}
		BlockPos post = wsd.getCommandPost(player);
		if (horizontalDistSq(player, post) > 48.0D * 48.0D) {
			return;
		}
		CapturedScientist waiting = null;
		for (CapturedScientist c : player.world.getEntitiesWithinAABB(CapturedScientist.class, new AxisAlignedBB(post).grow(128.0D, 48.0D, 128.0D))) {
			if (c instanceof LegionPrisoner || !c.isEntityAlive()) {
				continue;
			}
			if (!c.isDelivered() && (c.getRescuerPlayer() == null || c.isRescuer(player))) {
				//already waiting or following
				return;
			}
			if (c.isDelivered() && waiting == null && horizontalDistSq(c, post) < 24.0D * 24.0D) {
				waiting = c;
			}
		}
		if (waiting == null) {
			waiting = new CapturedScientist(player.world);
			BlockPos spot = randomSurface(player.world, post, 2, 4);
			waiting.setLocationAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0f, 0.0f);
			waiting.onInitialSpawn(player.world.getDifficultyForLocation(spot), null);
			player.world.spawnEntity(waiting);
		}
		waiting.joinCaravan(post);
		volkovSays(player, "msg.volkov_ready");
	}

	protected static boolean hasFollowingCaptive(EntityPlayerMP player) {
		for (CapturedScientist c : player.world.getEntitiesWithinAABB(CapturedScientist.class, player.getEntityBoundingBox().grow(48.0D))) {
			if (c.isRescuer(player) && c.isFollowing() && !c.isDelivered()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * ------------------------------------------------- start
	 */

	/**
	 * The radio in the start bunker comes alive and calls the player until he answers
	 */
	protected static void radioCall(EntityPlayerMP player) {
		int calls = RADIO_CALLS.getOrDefault(player.getUniqueID(), 0);
		if (calls >= 5 || player.ticksExisted < 100 + calls * 1200) {
			return;
		}
		RADIO_CALLS.put(player.getUniqueID(), calls + 1);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.8f, 0.6f);
		radioSays(player, calls == 0 ? "radio.call" : "radio.call_again");
		BlockPos bunker = ApocalypseWorldData.get(player.world).getBunkerPos();
		boolean inBunker = bunker != null && player.getDistanceSq(bunker) < 24.0D * 24.0D;
		chat(player, inBunker ? "radio.hint_bunker" : "radio.hint_item");
	}

	public static boolean isAtStartBunker(EntityPlayerMP player) {
		BlockPos bunker = ApocalypseWorldData.get(overworld(player)).getBunkerPos();
		return bunker != null && player.world.provider.getDimension() == 0 && player.getDistanceSq(bunker) < 24.0D * 24.0D;
	}

	/*
	 * ------------------------------------------------- commander
	 */

	public static CommanderNPC spawnCommander(World world, int x, int y, int z) {
		CommanderNPC commander = new CommanderNPC(world);
		commander.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0f, 0.0f);
		commander.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
		commander.setHomePosAndDistance(new BlockPos(x, y, z), 8);
		world.spawnEntity(commander);
		return commander;
	}

	/**
	 * The commander must always be available at the command post
	 */
	protected static void respawnCommanderCheck(EntityPlayerMP player, CampaignWorldData wsd) {
		if (player.ticksExisted % 200 != 0 || !wsd.hasCommandPost(player)) {
			return;
		}
		BlockPos post = wsd.getCommandPost(player);
		if (horizontalDistSq(player, post) > 32.0D * 32.0D) {
			return;
		}
		List<CommanderNPC> list = player.world.getEntitiesWithinAABB(CommanderNPC.class, new AxisAlignedBB(post).grow(32.0D, 16.0D, 32.0D));
		if (list.isEmpty()) {
			spawnCommander(player.world, post.getX(), post.getY() + 1, post.getZ());
		}
	}

	/*
	 * ------------------------------------------------- test commands
	 */

	public static void setMission(EntityPlayerMP player, int missionId) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		stopBattle(player, true);
		CampaignWorldData wsd = overworldData(player);
		wsd.clearPoints(player);
		data.setMission(missionId, TGCampaignData.STATE_OFFERED);
		data.clearObjective();
		data.setRadioGiven(true);
		if (countItem(player, TGItems.RADIO) == 0) {
			giveOrDrop(player, new ItemStack(TGItems.RADIO));
		}
		//missions after "road to friends" need the command post
		if (missionId > 6 && !wsd.hasCommandPost(player) && player.world.provider.getDimension() == 0) {
			double yaw = Math.toRadians(player.rotationYaw);
			BlockPos front = new BlockPos(player.posX - Math.sin(yaw) * 20.0D, 64, player.posZ + Math.cos(yaw) * 20.0D);
			CampaignPoint p = new CampaignPoint(front, CampaignSite.COMMAND_POST);
			CampaignSiteBuilder.place(player.world, wsd, player, p, RND);
			chat(player, "msg.test_post", p.pos.getX(), p.pos.getY(), p.pos.getZ());
		}
		sync(player);
	}

	public static void reset(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		stopBattle(player, true);
		CampaignWorldData wsd = overworldData(player);
		wsd.clearPoints(player);
		data.setMission(TGCampaignData.FIRST_MISSION, TGCampaignData.STATE_OFFERED);
		data.clearObjective();
		RADIO_CALLS.remove(player.getUniqueID());
		sync(player);
	}

	/**
	 * finishes the current mission at once: rewards and the next mission, ignores items and the in person rule
	 * @return false when there is no playable mission
	 */
	public static boolean completeCurrent(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished()) {
			return false;
		}
		CampaignMission m = CampaignMissions.byId(data.getMission());
		if (m == null || !m.playable) {
			return false;
		}
		completeMission(player, data, m);
		return true;
	}

	public static void onPlayerLoggedOut(EntityPlayerMP player) {
		stopBattle(player, true);
	}
}
