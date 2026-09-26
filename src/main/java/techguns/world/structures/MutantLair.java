package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import techguns.Techguns;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.entities.npcs.MutantWarlord;
import techguns.entities.npcs.MutantWarrior;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.util.BlockUtils;

/**
 * Crater with ruins where a mutant warlord gathers his warriors
 */
public class MutantLair extends WorldgenStructure {

	private static final ResourceLocation CHEST_LOOT = new ResourceLocation(Techguns.MODID, "chests/mutant_lair");

	protected static final int SIZE = 17;
	protected static final double RADIUS = 8.5D;

	public MutantLair() {
		super(SIZE, 8, SIZE, SIZE, 8, SIZE);
		this.setXZSize(SIZE, SIZE);
	}

	@Override
	public void spawnStructureWorldgen(World world, int chunkX, int chunkZ, int sizeX, int sizeY, int sizeZ, Random rnd, Biome biome) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		int y = BlockUtils.getValidSpawnYArea(world, x, z, SIZE, SIZE, 5, this.getStep());
		if (y < 12) {
			return;
		}
		this.setBlocks(world, x, y - 1, z, SIZE, sizeY, SIZE, 0, getBiomeColorTypeFromBiome(biome), rnd);
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);

		int ground = posY;
		int cx = posX + SIZE / 2;
		int cz = posZ + SIZE / 2;

		IBlockState[] floorBlocks = {
			Blocks.GRAVEL.getDefaultState(), Blocks.GRAVEL.getDefaultState(),
			Blocks.DIRT.getStateFromMeta(1), Blocks.DIRT.getStateFromMeta(1),
			Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.COBBLESTONE.getDefaultState(),
			Blocks.BONE_BLOCK.getDefaultState()
		};
		IBlockState foundation = Blocks.COBBLESTONE.getDefaultState();

		//the crater, floor height of every column is stored for later
		int[][] floorY = new int[SIZE][SIZE];
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				int x = posX + i;
				int z = posZ + j;
				double r = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				if (r > RADIUS) {
					floorY[i][j] = -1;
					continue;
				}
				int depth = (int) Math.round(4.0D * (1.0D - (r / RADIUS) * (r / RADIUS)));
				if (r > 2.0D && rnd.nextInt(4) == 0) {
					depth += rnd.nextBoolean() ? 1 : -1;
				}
				depth = Math.max(0, depth);
				int fy = ground - depth;
				floorY[i][j] = fy;

				b.clear(x, fy + 1, z, x, ground + 7, z);
				b.set(x, fy, z, floorBlocks[rnd.nextInt(floorBlocks.length)]);
				for (int dy = 1; dy <= 4; dy++) {
					if (b.get(x, fy - dy, z).getMaterial().blocksMovement()) {
						break;
					}
					b.set(x, fy - dy, z, foundation);
				}
			}
		}

		//ruined concrete walls around the rim
		IBlockState concrete = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN);
		IBlockState concreteLight = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				int fy = floorY[i][j];
				if (fy < 0) continue;
				int x = posX + i;
				int z = posZ + j;
				double r = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				if (r > 6.5D && rnd.nextInt(3) == 0) {
					int h = 1 + rnd.nextInt(4);
					for (int dy = 1; dy <= h; dy++) {
						IBlockState state = rnd.nextInt(4) == 0 ? Blocks.COBBLESTONE.getDefaultState() : (rnd.nextBoolean() ? concrete : concreteLight);
						b.set(x, fy + dy, z, state);
					}
				} else if (r > 3.5D && rnd.nextInt(14) == 0) {
					b.set(x, fy + 1 + rnd.nextInt(2), z, Blocks.WEB.getDefaultState());
				} else if (r > 2.5D && rnd.nextInt(18) == 0) {
					//bone piles
					b.set(x, fy + 1, z, Blocks.BONE_BLOCK.getDefaultState());
					if (rnd.nextBoolean()) {
						b.set(x, fy + 2, z, Blocks.BONE_BLOCK.getDefaultState());
					}
				} else if (r > 2.5D && r < 6.0D && rnd.nextInt(25) == 0) {
					b.set(x, fy, z, Blocks.GLOWSTONE.getDefaultState());
				}
			}
		}

		int center = SIZE / 2;

		//loot at the rim
		this.placeChest(b, floorY, posX, posZ, center, 2, EnumFacing.SOUTH);
		if (rnd.nextBoolean()) {
			this.placeChest(b, floorY, posX, posZ, center, SIZE - 3, EnumFacing.NORTH);
		}

		//warriors around the center
		int[][] guardSpots = {{center - 4, center}, {center + 4, center}, {center, center + 4}, {center, center - 4}};
		for (int[] spot : guardSpots) {
			if (rnd.nextInt(4) == 0) continue;
			int fy = floorY[spot[0]][spot[1]];
			int x = posX + spot[0];
			int z = posZ + spot[1];
			b.clear(x, fy + 1, z, x, fy + 3, z);
			TGSpawnerTileEnt spawner = b.spawner(x, fy + 1, z, EnumMonsterSpawnerType.HOLE, 3, 1, 400, 0);
			StructureBuilder.addMob(spawner, MutantWarrior.class, 2);
			StructureBuilder.addMob(spawner, SuperMutantBasic.class, 1);
		}

		//the warlord in the middle of the crater
		int fy = floorY[center][center];
		b.clear(cx - 1, fy + 1, cz - 1, cx + 1, fy + 5, cz + 1);
		b.spawner(cx, fy + 1, cz, EnumMonsterSpawnerType.HOLE, 1, 1, 100, 0, MutantWarlord.class);
	}

	protected void placeChest(StructureBuilder b, int[][] floorY, int posX, int posZ, int i, int j, EnumFacing facing) {
		int fy = floorY[i][j];
		if (fy < 0) return;
		b.clear(posX + i, fy + 1, posZ + j, posX + i, fy + 2, posZ + j);
		b.lootChest(posX + i, fy + 1, posZ + j, facing, CHEST_LOOT);
	}
}
