package techguns.campaign;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.Techguns;
import techguns.capabilities.TGCampaignData;
import techguns.capabilities.TGCampaignDataCapProvider;
import techguns.entities.npcs.CapturedScientist;
import techguns.entities.npcs.CommanderNPC;
import techguns.entities.npcs.General;
import techguns.entities.npcs.MilitaryJet;
import techguns.entities.npcs.PrototypeBoss;
import techguns.events.MilitaryExpansionEventHandler;
import techguns.world.structures.MutagenLabStructure;

/**
 * Events of the story campaign: capability attach, first join radio,
 * kill tracking, objective structure placement and mission progress checks.
 */
@Mod.EventBusSubscriber(modid = Techguns.MODID)
public class CampaignEventHandler {

	@SubscribeEvent
	public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(TGCampaignDataCapProvider.ID, new TGCampaignDataCapProvider());
		}
	}

	@SubscribeEvent
	public static void playerClone(final PlayerEvent.Clone event) {
		TGCampaignData oldData = TGCampaignData.get(event.getOriginal());
		TGCampaignData newData = TGCampaignData.get(event.getEntityPlayer());
		if (oldData != null && newData != null) {
			newData.copyFrom(oldData);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if (!(event.player instanceof EntityPlayerMP)) {
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		if (TGConfig.campaignEnabled && TGConfig.campaignGiveRadio && !data.isRadioGiven()) {
			data.setRadioGiven(true);
			TGCampaign.giveOrDrop(player, new ItemStack(TGItems.RADIO));
			player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.radio_given").setStyle(new Style().setColor(TextFormatting.GOLD)));
		}
		TGCampaign.sync(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			TGCampaign.sync((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			TGCampaign.sync((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		EntityLivingBase victim = event.getEntityLiving();
		if (victim.world.isRemote) {
			return;
		}
		Entity killer = event.getSource().getTrueSource();
		if (killer instanceof EntityPlayerMP && !(killer instanceof FakePlayer)) {
			TGCampaign.onEntityKilled((EntityPlayerMP) killer, victim);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !TGConfig.campaignEnabled) {
			return;
		}
		if (!(event.player instanceof EntityPlayerMP)) {
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		World world = player.world;
		if (world.isRemote || world.provider.getDimension() != 0) {
			return;
		}
		if (player.ticksExisted <= 0 || player.ticksExisted % 20 != 0) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null || data.isFinished()) {
			return;
		}

		CampaignWorldData wsd = CampaignWorldData.get(world);

		//place the objective structure when the player gets close to the planned point
		if (wsd.hasObjective(player) && !wsd.isObjectivePlaced(player)
				&& horizontalDistanceSq(player, wsd.getObjectivePos(player)) < TGCampaign.PLACE_DISTANCE * TGCampaign.PLACE_DISTANCE) {
			TGCampaign.placeObjectiveStructure(player);
		}

		if (data.getState() != TGCampaignData.STATE_ACTIVE) {
			//keep the command post staffed even between missions
			respawnCommanderCheck(player, wsd);
			return;
		}

		CampaignMission mission = CampaignMission.byId(data.getMission());
		if (mission == null) {
			return;
		}

		switch (mission) {
		case CONTACT:
			if (data.hasObjective() && horizontalDistanceSq(player, data.getObjective()) < 14.0D * 14.0D) {
				TGCampaign.setReady(player, data, mission);
			}
			break;
		case RECON:
			if (data.hasObjective() && horizontalDistanceSq(player, data.getObjective()) < 40.0D * 40.0D) {
				TGCampaign.setReady(player, data, mission);
			}
			break;
		case FIND_LAB:
			if (data.hasObjective() && horizontalDistanceSq(player, data.getObjective()) < 32.0D * 32.0D) {
				TGCampaign.setReady(player, data, mission);
			}
			break;
		case CLEAR_SKIES:
			if (player.ticksExisted % 600 == 0) {
				spawnMissionJet(player);
			}
			break;
		case RESCUE:
			checkEscort(player, data, mission);
			break;
		case SNAKE_HEAD:
			if (player.ticksExisted % 100 == 0) {
				respawnGeneralCheck(player, wsd);
			}
			break;
		case SAMPLE:
			if (hasSample(player)) {
				TGCampaign.setReady(player, data, mission);
			}
			break;
		case PROTOTYPE:
			if (player.ticksExisted % 100 == 0) {
				spawnPrototypeCheck(player, data);
			}
			break;
		case TRIUMPH:
			if (TGCampaign.findCommanderNear(player, 10.0D) != null) {
				TGCampaign.setReady(player, data, mission);
			}
			break;
		default:
			break;
		}

		respawnCommanderCheck(player, wsd);
	}

	protected static double horizontalDistanceSq(EntityPlayer player, BlockPos pos) {
		double dx = player.posX - (pos.getX() + 0.5D);
		double dz = player.posZ - (pos.getZ() + 0.5D);
		return dx * dx + dz * dz;
	}

	protected static boolean hasSample(EntityPlayer player) {
		for (ItemStack stack : player.inventory.mainInventory) {
			if (!stack.isEmpty() && stack.getItem() == TGItems.MUTAGEN_SAMPLE) {
				return true;
			}
		}
		for (ItemStack stack : player.inventory.offHandInventory) {
			if (!stack.isEmpty() && stack.getItem() == TGItems.MUTAGEN_SAMPLE) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Jets attack the player during the "clear skies" mission so there is something to shoot down
	 */
	protected static void spawnMissionJet(EntityPlayerMP player) {
		World world = player.world;
		if (!world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ))) {
			return;
		}
		if (!world.getEntitiesWithinAABB(MilitaryJet.class, player.getEntityBoundingBox().grow(128.0D, 128.0D, 128.0D)).isEmpty()) {
			return;
		}
		MilitaryExpansionEventHandler.startAirRaid(world, player);
	}

	protected static void checkEscort(EntityPlayerMP player, TGCampaignData data, CampaignMission mission) {
		List<CapturedScientist> list = player.world.getEntitiesWithinAABB(CapturedScientist.class, player.getEntityBoundingBox().grow(16.0D));
		for (CapturedScientist scientist : list) {
			if (scientist.isRescuer(player) && scientist.isEntityAlive()
					&& TGCampaign.findCommanderNear(scientist, 10.0D) != null) {
				TGCampaign.setReady(player, data, mission);
				return;
			}
		}
	}

	/**
	 * Respawns the General at his bunker when he is gone but the mission is still running
	 * (e.g. he fell into lava before the player got the kill)
	 */
	protected static void respawnGeneralCheck(EntityPlayerMP player, CampaignWorldData wsd) {
		if (!wsd.hasObjective(player) || wsd.getObjectiveMission(player) != CampaignMission.SNAKE_HEAD.id || !wsd.isObjectivePlaced(player)) {
			return;
		}
		BlockPos pos = wsd.getObjectivePos(player);
		if (horizontalDistanceSq(player, pos) > 48.0D * 48.0D) {
			return;
		}
		List<General> list = player.world.getEntitiesWithinAABB(General.class,
				new net.minecraft.util.math.AxisAlignedBB(pos).grow(64.0D, 32.0D, 64.0D));
		if (list.isEmpty()) {
			//the bunker hall is 8 blocks below ground level, see BunkerComplex
			TGCampaign.spawnGeneral(player.world, pos.getX(), pos.getY() - 7, pos.getZ() - 11);
		}
	}

	/**
	 * Spawns the Prototype in the boss hall of the mutagen lab when the player comes for him
	 */
	protected static void spawnPrototypeCheck(EntityPlayerMP player, TGCampaignData data) {
		if (!data.hasObjective()) {
			return;
		}
		BlockPos lab = data.getObjective();
		if (horizontalDistanceSq(player, lab) > 48.0D * 48.0D) {
			return;
		}
		List<PrototypeBoss> list = player.world.getEntitiesWithinAABB(PrototypeBoss.class,
				new net.minecraft.util.math.AxisAlignedBB(lab).grow(80.0D, 64.0D, 80.0D));
		if (list.isEmpty()) {
			BlockPos boss = lab.add(MutagenLabStructure.BOSS_OFFSET_X, MutagenLabStructure.BOSS_OFFSET_Y, MutagenLabStructure.BOSS_OFFSET_Z);
			PrototypeBoss prototype = new PrototypeBoss(player.world);
			prototype.setLocationAndAngles(boss.getX() + 0.5D, boss.getY(), boss.getZ() + 0.5D, 0.0f, 0.0f);
			prototype.onInitialSpawn(player.world.getDifficultyForLocation(boss), null);
			player.world.spawnEntity(prototype);
		}
	}

	/**
	 * The commander must always be available at the command post
	 */
	protected static void respawnCommanderCheck(EntityPlayerMP player, CampaignWorldData wsd) {
		if (player.ticksExisted % 200 != 0 || !wsd.hasCommandPost(player)) {
			return;
		}
		BlockPos post = wsd.getCommandPost(player);
		if (horizontalDistanceSq(player, post) > 32.0D * 32.0D) {
			return;
		}
		List<CommanderNPC> list = player.world.getEntitiesWithinAABB(CommanderNPC.class,
				new net.minecraft.util.math.AxisAlignedBB(post).grow(32.0D, 16.0D, 32.0D));
		if (list.isEmpty()) {
			TGCampaign.spawnCommander(player.world, post.getX(), post.getY() + 1, post.getZ());
		}
	}
}
