package techguns.world.wasteland;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/**
 * Root layer of the apocalypse world: every cell gets a weighted random wasteland biome
 */
public class GenLayerWasteland extends GenLayer {

	protected final int[] biomeIds;
	protected final int[] weights;
	protected final int totalWeight;

	public GenLayerWasteland(long seed, int[] biomeIds, int[] weights) {
		super(seed);
		this.biomeIds = biomeIds;
		this.weights = weights;
		int total = 0;
		for (int w : weights) {
			total += w;
		}
		this.totalWeight = total;
	}

	@Override
	public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
		int[] out = IntCache.getIntCache(areaWidth * areaHeight);
		for (int z = 0; z < areaHeight; z++) {
			for (int x = 0; x < areaWidth; x++) {
				this.initChunkSeed((long) (areaX + x), (long) (areaY + z));
				int roll = this.nextInt(this.totalWeight);
				int i = 0;
				while (roll >= this.weights[i]) {
					roll -= this.weights[i];
					i++;
				}
				out[x + z * areaWidth] = this.biomeIds[i];
			}
		}
		return out;
	}
}
