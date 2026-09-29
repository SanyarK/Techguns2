package techguns.world.wasteland;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;

/**
 * "Apocalypse" world type: vanilla terrain generator with only the wasteland biomes.
 * The sea level is lowered so the dried sea stays dry, water lakes are rare and there are no villages or temples.
 */
public class WorldTypeApocalypse extends WorldType {

	public static final String NAME = "tg_apocalypse";

	protected static final String GENERATOR_SETTINGS = "{\"seaLevel\":50,\"useWaterLakes\":true,\"waterLakeChance\":24,"
			+ "\"useVillages\":false,\"useTemples\":false,\"useMonuments\":false,\"useMansions\":false}";

	public WorldTypeApocalypse() {
		super(NAME);
	}

	@Override
	public BiomeProvider getBiomeProvider(World world) {
		return new BiomeProviderApocalypse(world.getSeed());
	}

	@Override
	public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
		return new ChunkGeneratorOverworld(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), GENERATOR_SETTINGS);
	}

	/**
	 * players spawn exactly at the spawn point, inside the start bunker
	 */
	@Override
	public int getSpawnFuzz(WorldServer world, MinecraftServer server) {
		if (ApocalypseWorldData.get(world).isBunkerBuilt()) {
			return 0;
		}
		return super.getSpawnFuzz(world, server);
	}

	@Override
	public boolean hasInfoNotice() {
		return true;
	}
}
