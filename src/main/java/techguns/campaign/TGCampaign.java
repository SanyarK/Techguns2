package techguns.campaign;

import java.util.List;
import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import techguns.TGArmors;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.TGPackets;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGCampaignData;
import techguns.entities.npcs.CapturedScientist;
import techguns.entities.npcs.CommanderNPC;
import techguns.entities.npcs.General;
import techguns.entities.npcs.MilitaryJet;
import techguns.entities.npcs.PrototypeBoss;
import techguns.packets.PacketCampaignOpenGui;
import techguns.packets.PacketCampaignSync;
import techguns.util.BlockUtils;
import techguns.world.structures.Airfield;
import techguns.world.structures.BunkerComplex;
import techguns.world.structures.CommandPost;
import techguns.world.structures.MutagenLabStructure;
import techguns.world.structures.WorldgenStructure;
import techguns.world.structures.WorldgenStructure.BiomeColorType;

/**
 * Server side logic of the story campaign: mission chain, objective points,
 * structure placement, rewards and the commander dialog.
 */
public class TGCampaign {

	/** dialog actions sent by the client GUI */
	public static final int ACTION_ACCEPT = 0;
	public static final int ACTION_TURN_IN = 1;

	/** structures are placed when the player gets this close to the objective point */
	public static final double PLACE_DISTANCE = 96.0D;

	/** how close the player must get for "reach" objectives, indexed checks in isReached() */
	protected static final double REACH_RADIUS_POST = 14.0D;
	protected static final double REACH_RADIUS_AIRFIELD = 40.0D;
	protected static final double REACH_RADIUS_LAB = 32.0D;

	protected static final int[] REWARD_XP = {0, 50, 100, 150, 250, 300, 350, 250, 300, 500, 1000};
	protected static final int[] REWARD_CONTRACTS = {0, 0, 1, 1, 1, 2, 0, 0, 1, 2, 2};

	/*
	 * ------------------------------------------------- helpers
	 */

	public static boolean isEnabled() {
		return TGConfig.campaignEnabled;
	}

	/**
	 * Campaign world data always lives in the overworld, no matter where the player is right now
	 */
	protected static CampaignWorldData overworldData(EntityPlayerMP player) {
		World world = player.getServer() != null ? player.getServer().getWorld(0) : player.world;
		return CampaignWorldData.get(world);
	}

	public static void sync(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data != null) {
			TGPackets.network.sendTo(new PacketCampaignSync(data), player);
		}
	}

	protected static void chat(EntityPlayer player, String key, Object... args) {
		player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args));
	}

	protected static void commanderSays(EntityPlayer player, String key, Object... args) {
		ITextComponent name = new TextComponentTranslation(Techguns.MODID + ".campaign.commander.name");
		name.setStyle(new Style().setColor(TextFormatting.GOLD));
		ITextComponent text = new TextComponentTranslation(Techguns.MODID + ".campaign." + key, args);
		player.sendMessage(name.appendSibling(new TextComponentTranslation("techguns.campaign.chat.separator")).appendSibling(text));
	}

	public static void giveOrDrop(EntityPlayer player, ItemStack stack) {
		if (!player.addItemStackToInventory(stack)) {
			player.world.spawnEntity(new EntityItem(player.world, player.posX, player.posY, player.posZ, stack));
		}
	}

	protected static boolean hasItem(EntityPlayer player, Item item) {
		for (ItemStack stack : player.inventory.mainInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				return true;
			}
		}
		for (ItemStack stack : player.inventory.offHandInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				return true;
			}
		}
		return false;
	}

	protected static boolean consumeItem(EntityPlayer player, Item item) {
		for (ItemStack stack : player.inventory.mainInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				stack.shrink(1);
				return true;
			}
		}
		for (ItemStack stack : player.inventory.offHandInventory) {
			if (!stack.isEmpty() && stack.getItem() == item) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	public static CommanderNPC findCommanderNear(Entity around, double radius) {
		List<CommanderNPC> list = around.world.getEntitiesWithinAABB(CommanderNPC.class, around.getEntityBoundingBox().grow(radius));
		return list.isEmpty() ? null : list.get(0);
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
		sync(player);
		TGPackets.network.sendTo(new PacketCampaignOpenGui(data.getMission(), data.getState(), data.getProgress(), atCommander), player);
	}

	public static void handleDialogAction(EntityPlayerMP player, int action) {
		if (!isEnabled()) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished()) {
			return;
		}
		boolean atCommander = findCommanderNear(player, 6.0D) != null;

		if (action == ACTION_ACCEPT && data.getState() == TGCampaignData.STATE_OFFERED) {
			//campaign objectives only exist in the overworld
			if (player.world.provider.getDimension() != 0) {
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.overworld_only").setStyle(new Style().setColor(TextFormatting.RED)));
				return;
			}
			acceptMission(player, data);
		} else if (action == ACTION_TURN_IN) {
			tryTurnIn(player, data, atCommander);
		}
		//refresh the dialog on the client
		openDialog(player, findCommanderNear(player, 6.0D) != null);
	}

	/*
	 * ------------------------------------------------- mission flow
	 */

	protected static void acceptMission(EntityPlayerMP player, TGCampaignData data) {
		CampaignMission mission = CampaignMission.byId(data.getMission());
		if (mission == null) {
			return;
		}
		data.setState(TGCampaignData.STATE_ACTIVE);
		data.setProgress(0);
		createObjective(player, data, mission);

		if (mission == CampaignMission.CONTACT && !hasItem(player, TGItems.GPS_NAVIGATOR)) {
			giveOrDrop(player, new ItemStack(TGItems.GPS_NAVIGATOR));
			commanderSays(player, "msg.gps_given");
		}
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.6f, 1.0f);
		chat(player, "msg.accepted", new TextComponentTranslation(mission.getTitleKey()));
		if (data.hasObjective()) {
			BlockPos o = data.getObjective();
			chat(player, "msg.objective_set", o.getX(), o.getZ());
		}
		sync(player);
	}

	protected static void createObjective(EntityPlayerMP player, TGCampaignData data, CampaignMission mission) {
		CampaignWorldData wsd = CampaignWorldData.get(player.world);
		switch (mission) {
		case CONTACT:
			//reuse an existing command post (e.g. after a reset) instead of building a second one
			if (wsd.hasCommandPost(player)) {
				data.setObjective(wsd.getCommandPost(player));
				wsd.clearObjective(player);
			} else {
				newObjectivePoint(player, data, wsd, mission);
			}
			break;
		case RECON:
		case RESCUE:
		case SNAKE_HEAD:
		case FIND_LAB:
			newObjectivePoint(player, data, wsd, mission);
			break;
		case SAMPLE:
		case PROTOTYPE:
			//these play inside the mutagen lab, keep the previous objective marker.
			//when there is none (mission skipped via command), plan a new lab
			if (!data.hasObjective()) {
				newObjectivePoint(player, data, wsd, CampaignMission.FIND_LAB);
			}
			break;
		case TRIUMPH:
			if (wsd.hasCommandPost(player)) {
				data.setObjective(wsd.getCommandPost(player));
			} else {
				data.clearObjective();
			}
			break;
		default:
			data.clearObjective();
			break;
		}
	}

	protected static void newObjectivePoint(EntityPlayerMP player, TGCampaignData data, CampaignWorldData wsd, CampaignMission mission) {
		BlockPos pos = pickObjectivePoint(player.world, player);
		wsd.setObjective(player, mission.id, pos);
		data.setObjective(pos);
	}

	/**
	 * Picks a random point 300-600 blocks (configurable) away from the player, avoiding oceans.
	 * Y is a placeholder, the real height is determined when the structure is placed.
	 */
	public static BlockPos pickObjectivePoint(World world, EntityPlayer player) {
		Random rnd = world.rand;
		int min = TGConfig.campaignMinDistance;
		int max = Math.max(TGConfig.campaignMaxDistance, min + 1);
		int x = MathHelper.floor(player.posX);
		int z = MathHelper.floor(player.posZ);
		for (int i = 0; i < 24; i++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double dist = min + rnd.nextDouble() * (max - min);
			x = MathHelper.floor(player.posX + Math.cos(angle) * dist);
			z = MathHelper.floor(player.posZ + Math.sin(angle) * dist);
			Biome biome = world.getBiome(new BlockPos(x, 64, z));
			if (biome.getBaseHeight() >= 0.0f && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
					&& !BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
				break;
			}
		}
		return new BlockPos(x, 64, z);
	}

	protected static BiomeColorType colorTypeFor(World world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
			return BiomeColorType.SNOW;
		}
		if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA)) {
			return BiomeColorType.DESERT;
		}
		return BiomeColorType.WOODLAND;
	}

	/**
	 * Called from the tick handler when the player is close to an unplaced objective.
	 * Builds the structure of the current objective and spawns its quest NPCs.
	 */
	public static void placeObjectiveStructure(EntityPlayerMP player) {
		World world = player.world;
		CampaignWorldData wsd = CampaignWorldData.get(world);
		int missionId = wsd.getObjectiveMission(player);
		CampaignMission mission = CampaignMission.byId(missionId);
		if (mission == null) {
			wsd.clearObjective(player);
			return;
		}
		BlockPos target = wsd.getObjectivePos(player);
		Random rnd = new Random();

		WorldgenStructure structure;
		switch (mission) {
		case CONTACT:
			structure = new CommandPost();
			break;
		case RECON:
			structure = new Airfield();
			break;
		case RESCUE:
		case SNAKE_HEAD:
			structure = new BunkerComplex();
			break;
		case FIND_LAB:
			structure = new MutagenLabStructure();
			break;
		default:
			wsd.clearObjective(player);
			return;
		}

		int sizeX = structure.getSizeX(rnd);
		int sizeZ = structure.getSizeZ(rnd);
		int x = target.getX() - sizeX / 2;
		int z = target.getZ() - sizeZ / 2;
		int y = BlockUtils.getValidSpawnYArea(world, x, z, sizeX, sizeZ, 6, 4);
		if (y < 20) {
			y = world.getHeight(target.getX(), target.getZ());
		}
		if (y < 20) {
			y = 64;
		}
		structure.setBlocks(world, x, y - 1, z, sizeX, structure.getSizeY(rnd), sizeZ, 0, colorTypeFor(world, target), rnd);

		//ground level (the "posY" the structures build on) is y-1, walking level is y
		BlockPos placed = new BlockPos(target.getX(), y - 1, target.getZ());
		wsd.setObjectivePlaced(player, placed);

		TGCampaignData data = TGCampaignData.get(player);
		if (data != null && data.hasObjective()) {
			data.setObjective(placed);
		}

		int cx = x + sizeX / 2;
		int cz = z + sizeZ / 2;
		switch (mission) {
		case CONTACT:
			//the commander is spawned by the CommandPost structure itself
			wsd.setCommandPost(player, placed);
			break;
		case RESCUE:
			//inside the bunker hall, see BunkerComplex layout: walking level is ground+1-8
			spawnScientist(world, cx - 4, y - 8, cz - 13);
			break;
		case SNAKE_HEAD:
			spawnGeneral(world, cx, y - 8, cz - 11);
			break;
		default:
			break;
		}
		chat(player, "msg.objective_spotted");
		sync(player);
	}

	public static CommanderNPC spawnCommander(World world, int x, int y, int z) {
		CommanderNPC commander = new CommanderNPC(world);
		commander.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0f, 0.0f);
		commander.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
		commander.setHomePosAndDistance(new BlockPos(x, y, z), 8);
		world.spawnEntity(commander);
		return commander;
	}

	public static CapturedScientist spawnScientist(World world, int x, int y, int z) {
		CapturedScientist scientist = new CapturedScientist(world);
		scientist.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0f, 0.0f);
		scientist.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
		scientist.setHomePosAndDistance(new BlockPos(x, y, z), 6);
		world.spawnEntity(scientist);
		return scientist;
	}

	public static General spawnGeneral(World world, int x, int y, int z) {
		General general = new General(world);
		general.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0f, 0.0f);
		general.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(x, y, z)), null);
		world.spawnEntity(general);
		return general;
	}

	/**
	 * Sets the current mission to ready and tells the player where to turn it in
	 */
	public static void setReady(EntityPlayerMP player, TGCampaignData data, CampaignMission mission) {
		data.setState(TGCampaignData.STATE_READY);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.3f);
		commanderSays(player, mission.requiresCommander ? "msg.ready_commander" : "msg.ready_radio");
		sync(player);
	}

	protected static void tryTurnIn(EntityPlayerMP player, TGCampaignData data, boolean atCommander) {
		CampaignMission mission = CampaignMission.byId(data.getMission());
		if (mission == null || data.getState() != TGCampaignData.STATE_READY) {
			return;
		}
		if (mission.requiresCommander && !atCommander) {
			commanderSays(player, "msg.come_in_person");
			return;
		}
		//quest items that are handed over to the commander
		if (mission == CampaignMission.SNAKE_HEAD && !consumeItem(player, TGItems.INTEL_DOCUMENTS)) {
			commanderSays(player, "msg.need_intel");
			return;
		}
		if (mission == CampaignMission.SAMPLE && !consumeItem(player, TGItems.MUTAGEN_SAMPLE)) {
			commanderSays(player, "msg.need_sample");
			return;
		}
		if (mission == CampaignMission.RESCUE) {
			//let the scientist stay at the post
			List<CapturedScientist> list = player.world.getEntitiesWithinAABB(CapturedScientist.class, player.getEntityBoundingBox().grow(12.0D));
			for (CapturedScientist scientist : list) {
				if (scientist.isRescuer(player)) {
					scientist.stayHere();
				}
			}
		}

		giveRewards(player, mission);
		overworldData(player).clearObjective(player);

		if (mission.id >= TGCampaignData.LAST_MISSION) {
			data.setMission(TGCampaignData.LAST_MISSION + 1, TGCampaignData.STATE_OFFERED);
			data.clearObjective();
			finishCampaign(player);
		} else {
			CampaignMission next = CampaignMission.byId(mission.id + 1);
			data.setMission(mission.id + 1, TGCampaignData.STATE_OFFERED);
			//the sample and prototype missions play in the lab found in mission 7, keep its marker
			if (next != CampaignMission.SAMPLE && next != CampaignMission.PROTOTYPE) {
				data.clearObjective();
			}
			commanderSays(player, "msg.mission_complete", new TextComponentTranslation(mission.getTitleKey()));
		}
		sync(player);
	}

	protected static void finishCampaign(EntityPlayerMP player) {
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		if (player.getServer() != null) {
			ITextComponent msg = new TextComponentTranslation(Techguns.MODID + ".campaign.msg.title_awarded", player.getName());
			msg.setStyle(new Style().setColor(TextFormatting.GOLD).setBold(true));
			player.getServer().getPlayerList().sendMessage(msg);
		}
	}

	/*
	 * ------------------------------------------------- rewards
	 */

	protected static void giveRewards(EntityPlayerMP player, CampaignMission mission) {
		int xp = REWARD_XP[mission.id];
		if (xp > 0) {
			player.addExperience(xp);
		}
		for (int i = 0; i < REWARD_CONTRACTS[mission.id]; i++) {
			giveOrDrop(player, new ItemStack(TGItems.MILITARY_CONTRACT));
		}

		if (mission == CampaignMission.SNAKE_HEAD && TGuns.scar != null) {
			giveOrDrop(player, makeNamedItem(new ItemStack(TGuns.scar), "techguns.campaign.reward.talon.name", "techguns.campaign.reward.talon.lore",
					new EnchantmentEntry(Enchantments.UNBREAKING, 3)));
			commanderSays(player, "msg.reward_weapon");
		}
		if (mission == CampaignMission.TRIUMPH) {
			giveOrDrop(player, makeNamedItem(new ItemStack(TGArmors.t4_power_Helmet), "techguns.campaign.reward.helmet.name", "techguns.campaign.reward.armor.lore",
					new EnchantmentEntry(Enchantments.PROTECTION, 4), new EnchantmentEntry(Enchantments.UNBREAKING, 3)));
			giveOrDrop(player, makeNamedItem(new ItemStack(TGArmors.t4_power_Chestplate), "techguns.campaign.reward.chestplate.name", "techguns.campaign.reward.armor.lore",
					new EnchantmentEntry(Enchantments.PROTECTION, 4), new EnchantmentEntry(Enchantments.UNBREAKING, 3)));
			if (TGuns.goldenrevolver != null) {
				giveOrDrop(player, makeNamedItem(new ItemStack(TGuns.goldenrevolver), "techguns.campaign.reward.revolver.name", "techguns.campaign.reward.revolver.lore",
						new EnchantmentEntry(Enchantments.UNBREAKING, 3)));
			}
			commanderSays(player, "msg.reward_final");
		}
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	protected static class EnchantmentEntry {
		final Enchantment ench;
		final int level;

		EnchantmentEntry(Enchantment ench, int level) {
			this.ench = ench;
			this.level = level;
		}
	}

	/**
	 * Unique reward item: colored display name, lore line and enchantments.
	 * Names use the raw translation of the server locale is not available per player,
	 * so the lang key text is resolved on the client via the localized name format codes.
	 */
	protected static ItemStack makeNamedItem(ItemStack stack, String nameKey, String loreKey, EnchantmentEntry... enchantments) {
		for (EnchantmentEntry e : enchantments) {
			if (e.ench != null) {
				stack.addEnchantment(e.ench, e.level);
			}
		}
		NBTTagCompound display = stack.getOrCreateSubCompound("display");
		//LocName is resolved client side, so every player sees the name in their own language
		display.setTag("LocName", new NBTTagString(nameKey));
		NBTTagList lore = new NBTTagList();
		lore.appendTag(new NBTTagString(TextFormatting.DARK_PURPLE + new TextComponentTranslation(loreKey).getUnformattedText()));
		display.setTag("Lore", lore);
		return stack;
	}

	/*
	 * ------------------------------------------------- kill tracking
	 */

	public static void onEntityKilled(EntityPlayerMP player, EntityLivingBase victim) {
		if (!isEnabled()) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished() || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return;
		}
		CampaignMission mission = CampaignMission.byId(data.getMission());
		if (mission == null) {
			return;
		}

		boolean counts = false;
		switch (mission) {
		case BAPTISM:
			counts = victim instanceof IMob;
			break;
		case CLEAR_SKIES:
			counts = victim instanceof MilitaryJet;
			break;
		case SNAKE_HEAD:
			counts = victim instanceof General;
			break;
		case PROTOTYPE:
			counts = victim instanceof PrototypeBoss;
			break;
		default:
			return;
		}
		if (!counts) {
			return;
		}

		int progress = data.getProgress() + 1;
		data.setProgress(progress);

		if (mission == CampaignMission.SNAKE_HEAD) {
			giveOrDrop(player, new ItemStack(TGItems.INTEL_DOCUMENTS));
			commanderSays(player, "msg.intel_taken");
		}

		if (progress >= mission.required) {
			setReady(player, data, mission);
		} else {
			player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.progress", progress, mission.required)
					.setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
			sync(player);
		}
	}

	/**
	 * Called when the captured scientist died while the escort mission was running: restart the mission
	 */
	public static void onScientistDied(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.getMission() != CampaignMission.RESCUE.id || data.getState() != TGCampaignData.STATE_ACTIVE) {
			return;
		}
		data.setState(TGCampaignData.STATE_OFFERED);
		data.setProgress(0);
		data.clearObjective();
		CampaignWorldData.get(player.world).clearObjective(player);
		commanderSays(player, "msg.scientist_died");
		sync(player);
	}

	/*
	 * ------------------------------------------------- test commands
	 */

	public static void setMission(EntityPlayerMP player, int missionId) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		overworldData(player).clearObjective(player);
		data.setMission(missionId, TGCampaignData.STATE_OFFERED);
		data.clearObjective();
		data.setRadioGiven(true);
		sync(player);
	}

	public static void reset(EntityPlayerMP player) {
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		CampaignWorldData wsd = overworldData(player);
		wsd.clearAll(player);
		data.setMission(TGCampaignData.FIRST_MISSION, TGCampaignData.STATE_OFFERED);
		data.clearObjective();
		sync(player);
	}
}
