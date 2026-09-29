package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import techguns.TGConfig;

/**
 * Ruins of the wasteland biomes, runs for every chunk so city blocks at the border of the city biome are finished too
 */
public class WastelandWorldGenerator implements IWorldGenerator {

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
		if (TGConfig.wastelandRuins && world.provider.getDimension() == 0) {
			WastelandRuins.generate(world, random, chunkX, chunkZ);
		}
	}
}
