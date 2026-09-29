package techguns.world.wasteland;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import techguns.TGConfig;

/**
 * Biome provider of the apocalypse world: only wasteland biomes, no oceans and no rivers.
 * The layer stack is built like the vanilla one, the number of zoom layers decides the biome size.
 */
public class BiomeProviderApocalypse extends BiomeProvider {

	protected final GenLayer genBiomes;
	protected final GenLayer biomeIndexLayer;
	protected final BiomeCache cache;
	protected final List<Biome> spawnBiomes;
	protected final Biome fallback;

	public BiomeProviderApocalypse(long seed) {
		super();
		this.fallback = TGBiomes.WASTELAND;
		this.spawnBiomes = Lists.newArrayList(TGBiomes.WASTELAND, TGBiomes.DEAD_FOREST, TGBiomes.SCORCHED_HILLS);

		int[] ids = {
			Biome.getIdForBiome(TGBiomes.WASTELAND),
			Biome.getIdForBiome(TGBiomes.SCORCHED_HILLS),
			Biome.getIdForBiome(TGBiomes.DEAD_FOREST),
			Biome.getIdForBiome(TGBiomes.RADIOACTIVE_ZONE),
			Biome.getIdForBiome(TGBiomes.DRIED_SEA),
			Biome.getIdForBiome(TGBiomes.CITY_RUINS)
		};
		int[] weights = {36, 14, 14, 10, 10, 16};

		//every root cell is 4 * 2^(size+2) blocks wide, like the vanilla biome size setting
		GenLayer layer = new GenLayerWasteland(200L, ids, weights);
		layer = new GenLayerFuzzyZoom(2000L, layer);
		layer = GenLayerZoom.magnify(1000L, layer, TGConfig.apocalypseBiomeSize + 1);
		layer = new GenLayerSmooth(1000L, layer);
		GenLayer index = new GenLayerVoronoiZoom(10L, layer);
		layer.initWorldGenSeed(seed);
		index.initWorldGenSeed(seed);
		this.genBiomes = layer;
		this.biomeIndexLayer = index;
		this.cache = new BiomeCache(this);
	}

	@Override
	public List<Biome> getBiomesToSpawnIn() {
		return this.spawnBiomes;
	}

	@Override
	public Biome getBiome(BlockPos pos, Biome defaultBiome) {
		return this.cache.getBiome(pos.getX(), pos.getZ(), defaultBiome);
	}

	@Override
	public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
		IntCache.resetIntCache();
		if (biomes == null || biomes.length < width * height) {
			biomes = new Biome[width * height];
		}
		int[] ints = this.genBiomes.getInts(x, z, width, height);
		for (int i = 0; i < width * height; i++) {
			biomes[i] = Biome.getBiome(ints[i], this.fallback);
		}
		return biomes;
	}

	@Override
	public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
		IntCache.resetIntCache();
		if (listToReuse == null || listToReuse.length < width * length) {
			listToReuse = new Biome[width * length];
		}
		if (cacheFlag && width == 16 && length == 16 && (x & 15) == 0 && (z & 15) == 0) {
			Biome[] cached = this.cache.getCachedBiomes(x, z);
			System.arraycopy(cached, 0, listToReuse, 0, width * length);
			return listToReuse;
		}
		int[] ints = this.biomeIndexLayer.getInts(x, z, width, length);
		for (int i = 0; i < width * length; i++) {
			listToReuse[i] = Biome.getBiome(ints[i], this.fallback);
		}
		return listToReuse;
	}

	@Override
	public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
		IntCache.resetIntCache();
		int x1 = x - radius >> 2;
		int z1 = z - radius >> 2;
		int x2 = x + radius >> 2;
		int z2 = z + radius >> 2;
		int w = x2 - x1 + 1;
		int h = z2 - z1 + 1;
		int[] ints = this.genBiomes.getInts(x1, z1, w, h);
		for (int i = 0; i < w * h; i++) {
			if (!allowed.contains(Biome.getBiome(ints[i], this.fallback))) {
				return false;
			}
		}
		return true;
	}

	@Override
	@Nullable
	public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
		IntCache.resetIntCache();
		int x1 = x - range >> 2;
		int z1 = z - range >> 2;
		int x2 = x + range >> 2;
		int z2 = z + range >> 2;
		int w = x2 - x1 + 1;
		int h = z2 - z1 + 1;
		int[] ints = this.genBiomes.getInts(x1, z1, w, h);
		BlockPos found = null;
		int count = 0;
		for (int i = 0; i < w * h; i++) {
			int bx = x1 + i % w << 2;
			int bz = z1 + i / w << 2;
			if (biomes.contains(Biome.getBiome(ints[i], this.fallback)) && (found == null || random.nextInt(count + 1) == 0)) {
				found = new BlockPos(bx, 0, bz);
				count++;
			}
		}
		return found;
	}

	@Override
	public void cleanupCache() {
		this.cache.cleanupCache();
		super.cleanupCache();
	}
}
