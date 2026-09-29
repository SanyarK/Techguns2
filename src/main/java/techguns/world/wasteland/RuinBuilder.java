package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.world.structures.StructureBuilder;

/**
 * Structure builder for worldgen ruins: blocks are placed without neighbour and observer updates
 * so nothing outside the populated area gets loaded, plus helpers for ground height and rubble.
 */
public class RuinBuilder extends StructureBuilder {

	public static final IBlockState AIR = Blocks.AIR.getDefaultState();
	public static final IBlockState COARSE_DIRT = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
	public static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
	public static final IBlockState COBBLESTONE = Blocks.COBBLESTONE.getDefaultState();
	public static final IBlockState IRON_BARS = Blocks.IRON_BARS.getDefaultState();
	public static final IBlockState CRACKED_BRICKS = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
	public static final IBlockState ANDESITE = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE);

	/** blocks outside of this area are not placed, used to build chunk aligned city blocks in parts */
	protected int clipMinX = Integer.MIN_VALUE;
	protected int clipMinZ = Integer.MIN_VALUE;
	protected int clipMaxX = Integer.MAX_VALUE;
	protected int clipMaxZ = Integer.MAX_VALUE;

	public RuinBuilder(World world, Random rnd) {
		super(world, rnd);
	}

	public RuinBuilder clip(int minX, int minZ, int maxX, int maxZ) {
		this.clipMinX = minX;
		this.clipMinZ = minZ;
		this.clipMaxX = maxX;
		this.clipMaxZ = maxZ;
		return this;
	}

	public boolean inClip(int x, int z) {
		return x >= this.clipMinX && x <= this.clipMaxX && z >= this.clipMinZ && z <= this.clipMaxZ;
	}

	public static IBlockState concreteColor(EnumDyeColor color) {
		return Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, color);
	}

	public static IBlockState terracotta(EnumDyeColor color) {
		return Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, color);
	}

	/**
	 * flags 2|16: send the change to clients, but no neighbour or observer updates
	 */
	@Override
	public void set(int x, int y, int z, IBlockState state) {
		if (isValidY(y) && this.inClip(x, z)) {
			this.world.setBlockState(this.p.setPos(x, y, z), state, 18);
		}
	}

	@Override
	public void lootChest(int x, int y, int z, EnumFacing facing, ResourceLocation loottable) {
		if (this.inClip(x, z)) {
			super.lootChest(x, y, z, facing, loottable);
		}
	}

	@Override
	public TGSpawnerTileEnt spawner(int x, int y, int z, EnumMonsterSpawnerType type, int mobsLeft, int maxActive, int delay, int range) {
		if (!this.inClip(x, z)) {
			return null;
		}
		return super.spawner(x, y, z, type, mobsLeft, maxActive, delay, range);
	}

	/**
	 * y of the highest ground block, plants, trees, snow and webs are ignored
	 */
	public int ground(int x, int z) {
		int y = Math.min(this.world.getHeight(this.p.setPos(x, 0, z)).getY(), 254);
		for (; y > 1; y--) {
			Material m = this.get(x, y, z).getMaterial();
			if (m == Material.AIR || m == Material.PLANTS || m == Material.VINE || m == Material.LEAVES || m == Material.WOOD
					|| m == Material.WEB || m == Material.SNOW || m == Material.CACTUS || m == Material.GOURD || m == Material.CARPET) {
				continue;
			}
			break;
		}
		return y;
	}

	public int averageGround(int x1, int z1, int x2, int z2) {
		int sum = 0;
		int count = 0;
		for (int x = x1; x <= x2; x += 2) {
			for (int z = z1; z <= z2; z += 2) {
				sum += this.ground(x, z);
				count++;
			}
		}
		return count > 0 ? Math.round((float) sum / count) : this.ground(x1, z1);
	}

	/**
	 * difference between highest and lowest ground of the area corners and center
	 */
	public int heightDifference(int x1, int z1, int x2, int z2) {
		int[] h = { this.ground(x1, z1), this.ground(x2, z1), this.ground(x1, z2), this.ground(x2, z2), this.ground((x1 + x2) / 2, (z1 + z2) / 2) };
		int min = h[0];
		int max = h[0];
		for (int v : h) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		return max - min;
	}

	/**
	 * fills from y downwards until solid ground is reached
	 */
	public void foundation(int x, int z, int y, IBlockState state) {
		for (int i = 0; i < 12 && y - i > 1; i++) {
			Material m = this.get(x, y - i, z).getMaterial();
			if (m.blocksMovement() && m != Material.LEAVES && m != Material.WOOD) {
				return;
			}
			this.set(x, y - i, z, state);
		}
	}

	public void clearAbove(int x, int y, int z, int height) {
		for (int i = 0; i < height; i++) {
			if (!this.isAir(x, y + i, z)) {
				this.set(x, y + i, z, AIR);
			}
		}
	}

	public IBlockState rubbleState(boolean allowGravel) {
		int i = this.rnd.nextInt(100);
		if (i < 25) {
			return COBBLESTONE;
		} else if (i < 40) {
			return CRACKED_BRICKS;
		} else if (i < 55) {
			return concrete(EnumConcreteType.CONCRETE_GREY);
		} else if (i < 65) {
			return ANDESITE;
		} else if (i < 80) {
			return allowGravel ? GRAVEL : COBBLESTONE;
		} else if (i < 88) {
			return Blocks.MOSSY_COBBLESTONE.getDefaultState();
		} else if (i < 94) {
			return Blocks.BRICK_BLOCK.getDefaultState();
		}
		return Blocks.STONE_SLAB.getDefaultState();
	}

	/**
	 * round pile of rubble on the ground, only inside the given bounds
	 */
	public void rubblePile(int cx, int cz, int radius, int height, int minX, int minZ, int maxX, int maxZ) {
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				if (x < minX || x > maxX || z < minZ || z > maxZ) {
					continue;
				}
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				int h = (int) Math.round(height * (1.0D - d / (radius + 0.5D)) + this.rnd.nextDouble() - 0.5D);
				if (h <= 0) {
					continue;
				}
				int gy = this.ground(x, z);
				for (int i = 1; i <= h; i++) {
					this.set(x, gy + i, z, this.rubbleState(true));
				}
			}
		}
	}
}
