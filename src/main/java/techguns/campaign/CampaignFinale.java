package techguns.campaign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.TGPackets;
import techguns.entities.npcs.SettlerNPC;
import techguns.packets.PacketCampaignEvent;

/**
 * The happy end after the last mission: the colonel gives the order, doctor Volkov launches the
 * Purifier, a flash, clean rain, the wastes come back to life around the command post and the start
 * bunker, settlers come to the post, the player becomes the Wasteland Hero and the epilogue starts.
 * The scene runs per player (ticks since the start, only in memory), a player that logs out in the
 * middle of it still gets the restored world.
 */
public class CampaignFinale {

	protected static final int LAUNCH = 120;
	protected static final int SETTLERS = 320;
	protected static final int TITLE = 400;
	protected static final int EPILOGUE = 480;

	protected static final int SETTLER_COUNT = 5;

	protected static final Map<UUID, Integer> RUNNING = new HashMap<>();

	public static void start(EntityPlayerMP player) {
		RUNNING.put(player.getUniqueID(), 0);
	}

	public static boolean isRunning(EntityPlayerMP player) {
		return RUNNING.containsKey(player.getUniqueID());
	}

	/**
	 * every server tick of the player
	 */
	public static void tick(EntityPlayerMP player) {
		Integer t = RUNNING.get(player.getUniqueID());
		if (t == null) {
			return;
		}
		int time = t + 1;
		RUNNING.put(player.getUniqueID(), time);
		switch (time) {
		case 1:
			TGCampaign.commanderSays(player, "finale.order");
			break;
		case 60:
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.8f, 0.6f);
			TGCampaign.volkovSays(player, "finale.countdown");
			break;
		case LAUNCH:
			launch(player);
			break;
		case LAUNCH + 60:
			TGCampaign.commanderSays(player, "finale.rain");
			break;
		case LAUNCH + 130:
			TGCampaign.commanderSays(player, "finale.life");
			break;
		case SETTLERS:
			spawnSettlers(player, TGCampaign.overworldData(player), SETTLER_COUNT);
			TGCampaign.commanderSays(player, "finale.settlers");
			break;
		case TITLE:
			TGCampaign.commanderSays(player, "finale.hero");
			TGCampaign.finishCampaign(player);
			break;
		case EPILOGUE:
			RUNNING.remove(player.getUniqueID());
			TGPackets.network.sendTo(new PacketCampaignEvent(PacketCampaignEvent.EPILOGUE, 0), player);
			break;
		default:
			break;
		}
	}

	/**
	 * the Purifier starts: sound, flash, the world is restored
	 */
	protected static void launch(EntityPlayerMP player) {
		World world = TGCampaign.overworld(player);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 3.0f, 0.7f);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.8f);
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_FIREWORK_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 0.6f);
		TGPackets.network.sendTo(new PacketCampaignEvent(PacketCampaignEvent.FLASH, 70), player);
		if (player.world instanceof WorldServer) {
			((WorldServer) player.world).spawnParticle(EnumParticleTypes.END_ROD, player.posX, player.posY + 6.0D, player.posZ, 120, 12.0D, 4.0D, 12.0D, 0.02D);
		}
		restoreWorld(player, world);
	}

	/**
	 * sets the flag and starts the new life around the command post and the start bunker of the player
	 */
	public static void restoreWorld(EntityPlayerMP player, World overworld) {
		WorldRestoration data = WorldRestoration.get(overworld);
		data.restore(overworld);
		CampaignWorldData wsd = CampaignWorldData.get(overworld);
		if (wsd.hasCommandPost(player)) {
			data.addCircle(wsd.getCommandPost(player));
		}
		data.addCircle(TGCampaign.getHome(player));
	}

	/**
	 * peaceful settlers walk around the command post
	 */
	public static void spawnSettlers(EntityPlayerMP player, CampaignWorldData wsd, int count) {
		if (!wsd.hasCommandPost(player)) {
			return;
		}
		World world = TGCampaign.overworld(player);
		BlockPos post = wsd.getCommandPost(player);
		if (!world.isBlockLoaded(post)) {
			return;
		}
		for (int i = 0; i < count; i++) {
			BlockPos spot = TGCampaign.randomSurface(world, post, 4, 11);
			SettlerNPC settler = new SettlerNPC(world);
			settler.setLocationAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, world.rand.nextFloat() * 360.0f, 0.0f);
			settler.onInitialSpawn(world.getDifficultyForLocation(spot), null);
			settler.setHomePosAndDistance(post, 14);
			settler.enablePersistence();
			world.spawnEntity(settler);
		}
	}

	/**
	 * after the campaign the post keeps its settlers, new ones come when there are too few
	 */
	public static void settlersCheck(EntityPlayerMP player, CampaignWorldData wsd) {
		if (player.ticksExisted % 600 != 0 || !wsd.hasCommandPost(player) || isRunning(player)) {
			return;
		}
		BlockPos post = wsd.getCommandPost(player);
		if (TGCampaign.horizontalDistSq(player, post) > 48.0D * 48.0D) {
			return;
		}
		List<SettlerNPC> list = player.world.getEntitiesWithinAABB(SettlerNPC.class, new AxisAlignedBB(post).grow(40.0D, 16.0D, 40.0D));
		if (list.size() < 3) {
			spawnSettlers(player, wsd, SETTLER_COUNT - list.size());
		}
	}

	/**
	 * a player that leaves in the middle of the scene still gets the restored world and the title
	 */
	public static void onLogout(EntityPlayerMP player) {
		Integer t = RUNNING.remove(player.getUniqueID());
		if (t == null) {
			return;
		}
		if (t < LAUNCH) {
			restoreWorld(player, TGCampaign.overworld(player));
		}
		if (t < TITLE) {
			TGCampaign.finishCampaign(player);
		}
	}
}
