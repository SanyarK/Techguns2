package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.feature.WorldGeneratorBonusChest;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.util.TGLogger;

/**
 * Builds the start bunker once when a new world is created and greets new players in it
 */
@Mod.EventBusSubscriber(modid = Techguns.MODID)
public class ApocalypseEventHandler {

	protected static final String INTRO_TAG = "techguns_bunker_intro";

	/**
	 * Only fired when a new world is created, existing worlds are never changed
	 */
	@SubscribeEvent
	public static void onCreateSpawnPosition(WorldEvent.CreateSpawnPosition event) {
		World world = event.getWorld();
		if (world.isRemote || world.provider.getDimension() != 0) {
			return;
		}
		boolean apocalypse = world.getWorldType() instanceof WorldTypeApocalypse;
		if (apocalypse ? !TGConfig.startBunker : !TGConfig.startBunkerNormalWorlds) {
			return;
		}
		ApocalypseWorldData data = ApocalypseWorldData.get(world);
		if (data.isBunkerBuilt()) {
			return;
		}

		//same search as vanilla: a spawn biome near 0/0, then a position where players may spawn
		Random random = new Random(world.getSeed());
		BiomeProvider provider = world.getBiomeProvider();
		BlockPos found = provider.findBiomePosition(0, 0, 256, provider.getBiomesToSpawnIn(), random);
		int x = 8;
		int z = 8;
		if (found != null) {
			x = found.getX();
			z = found.getZ();
		}
		for (int tries = 0; tries < 1000 && !world.provider.canCoordinateBeSpawn(x, z); tries++) {
			x += random.nextInt(64) - random.nextInt(64);
			z += random.nextInt(64) - random.nextInt(64);
		}

		BlockPos inside = StartBunker.build(world, x, z, random, true);
		data.setBunker(inside);
		if (inside == null) {
			//ground too low for a bunker, let vanilla choose the spawn
			return;
		}
		world.setSpawnPoint(inside);
		if (!apocalypse) {
			world.getGameRules().setOrCreateGameRule("spawnRadius", "0");
		}
		if (event.getSettings().isBonusChestEnabled()) {
			new WorldGeneratorBonusChest().generate(world, random, world.getTopSolidOrLiquidBlock(new BlockPos(x + 5, 0, z)));
		}
		TGLogger.logger_server.info("Built the start bunker at " + inside);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if (!(event.player instanceof EntityPlayerMP) || event.player.world.provider.getDimension() != 0) {
			return;
		}
		EntityPlayer player = event.player;
		NBTTagCompound persisted = getPersistedTag(player);
		if (persisted.getBoolean(INTRO_TAG)) {
			return;
		}
		ApocalypseWorldData data = ApocalypseWorldData.get(player.world);
		BlockPos bunker = data.getBunkerPos();
		if (bunker == null) {
			return;
		}
		persisted.setBoolean(INTRO_TAG, true);
		if (player.getDistanceSq(bunker) < 100.0D) {
			player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".apocalypse.bunker.welcome").setStyle(new Style().setColor(TextFormatting.GOLD)));
		}
	}

	protected static NBTTagCompound getPersistedTag(EntityPlayer player) {
		NBTTagCompound data = player.getEntityData();
		if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
			data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
		}
		return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
	}
}
