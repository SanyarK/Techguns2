package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import techguns.TGBlocks;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.blocks.BlockRadioStation;
import techguns.blocks.BlockTGLadder;
import techguns.blocks.BlockTGOre;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.EnumOreType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.Bandit;
import techguns.entities.npcs.MutantWarrior;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.entities.npcs.ZombieFarmer;
import techguns.entities.npcs.ZombieMiner;
import techguns.entities.npcs.ZombiePoliceman;
import techguns.entities.npcs.ZombieSoldier;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.world.structures.StructureBuilder;
import techguns.world.wasteland.BiomeWasteland.Kind;

/**
 * Procedural ruins of the wasteland biomes. Every method builds inside one 16x16 cell (x0/z0 is its
 * north west corner), during worldgen the cell is the chunk area shifted by +8 like vanilla decorators
 * use it, so the neighbouring chunks are already generated and nothing cascades.
 */
public class WastelandRuins {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "chests/wasteland_ruins");
	public static final ResourceLocation LOOT_GAS_STATION = new ResourceLocation(Techguns.MODID, "chests/gasstation");

	/** names for /tgstructure */
	public static final String[] NAMES = { "city_block", "ruined_building", "ruined_house", "burnt_house", "gas_station", "radio_tower", "crater", "road", "car_wreck", "shipwreck" };

	/** highways cross the wastelands every X chunks */
	protected static final int HIGHWAY_SPACING = 20;

	protected static final IBlockState AIR = RuinBuilder.AIR;
	protected static final IBlockState ASPHALT = RuinBuilder.concreteColor(EnumDyeColor.BLACK);
	protected static final IBlockState ASPHALT_LIGHT = RuinBuilder.concreteColor(EnumDyeColor.GRAY);
	protected static final IBlockState ROAD_LINE = RuinBuilder.concreteColor(EnumDyeColor.YELLOW);
	protected static final IBlockState PARKING_LINE = RuinBuilder.concreteColor(EnumDyeColor.WHITE);
	protected static final IBlockState SIDEWALK = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
	protected static final IBlockState SLAB_CONCRETE = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
	protected static final IBlockState GLASS_PANE = Blocks.GLASS_PANE.getDefaultState();
	protected static final IBlockState STONE_SLAB = Blocks.STONE_SLAB.getDefaultState();

	protected static final IBlockState[] BUILDING_WALLS = {
		StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY), StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT),
		StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN), Blocks.STONEBRICK.getDefaultState(),
		Blocks.BRICK_BLOCK.getDefaultState(), RuinBuilder.concreteColor(EnumDyeColor.WHITE)
	};

	protected static final EnumDyeColor[] CAR_COLORS = { EnumDyeColor.RED, EnumDyeColor.ORANGE, EnumDyeColor.BROWN, EnumDyeColor.WHITE,
		EnumDyeColor.SILVER, EnumDyeColor.CYAN, EnumDyeColor.BLUE, EnumDyeColor.GREEN };

	/**
	 * Local coordinates i/j (0..15) of a cell, rotated around the cell center
	 */
	protected static class Cell {
		final int x0;
		final int z0;
		final int rot;

		Cell(int x0, int z0, int rot) {
			this.x0 = x0;
			this.z0 = z0;
			this.rot = rot & 3;
		}

		int x(int i, int j) {
			switch (this.rot) {
			case 1:
				return this.x0 + 15 - j;
			case 2:
				return this.x0 + 15 - i;
			case 3:
				return this.x0 + j;
			default:
				return this.x0 + i;
			}
		}

		int z(int i, int j) {
			switch (this.rot) {
			case 1:
				return this.z0 + i;
			case 2:
				return this.z0 + 15 - j;
			case 3:
				return this.z0 + 15 - i;
			default:
				return this.z0 + j;
			}
		}

		/** local EAST is +i, local SOUTH is +j */
		EnumFacing f(EnumFacing local) {
			EnumFacing f = local;
			for (int k = 0; k < this.rot; k++) {
				f = f.rotateY();
			}
			return f;
		}

		void set(RuinBuilder b, int i, int y, int j, IBlockState state) {
			b.set(this.x(i, j), y, this.z(i, j), state);
		}

		int ground(RuinBuilder b, int i, int j) {
			return b.ground(this.x(i, j), this.z(i, j));
		}
	}

	/*
	 * ------------------------------------------------- worldgen entry
	 */

	public static void decorate(World world, Random rand, int x0, int z0, BiomeWasteland biome) {
		RuinBuilder b = new RuinBuilder(world, rand);
		if (biome.kind == Kind.CITY_RUINS) {
			cityBlock(b, x0, z0);
			return;
		}

		highways(b, x0, z0);

		if (rand.nextInt(Math.max(1, TGConfig.wastelandRuinRarity)) == 0) {
			int roll = rand.nextInt(100);
			if (biome.kind == Kind.DRIED_SEA && roll < 45) {
				shipwreck(b, x0, z0);
			} else if (roll < (biome.kind == Kind.RADIOACTIVE_ZONE ? 50 : 30)) {
				crater(b, x0 + 5 + rand.nextInt(6), z0 + 5 + rand.nextInt(6), 3 + rand.nextInt(3), 2 + rand.nextInt(3), biome.kind == Kind.RADIOACTIVE_ZONE);
			} else if (roll < 55) {
				ruinedHouse(b, x0 + 3, z0 + 3, 10, 10, rand.nextBoolean());
			} else if (roll < 68) {
				carWrecks(b, x0, z0);
			} else if (roll < 78) {
				if (b.heightDifference(x0, z0, x0 + 15, z0 + 15) <= 4) {
					gasStation(b, x0, z0);
				}
			} else if (roll < 88) {
				if (b.heightDifference(x0 + 1, z0 + 1, x0 + 14, z0 + 14) <= 4) {
					radioTower(b, x0, z0);
				}
			} else {
				ruinedBuilding(b, x0 + 3, z0 + 3, 10, 10, 2, 4);
			}
		} else if (rand.nextInt(5) == 0) {
			debris(b, x0, z0);
		}

		if (biome.kind == Kind.RADIOACTIVE_ZONE && rand.nextInt(3) == 0) {
			uraniumPatch(b, x0 + 2 + rand.nextInt(12), z0 + 2 + rand.nextInt(12));
		}
	}

	/**
	 * places a ruin around pos for /tgstructure, returns false for unknown names
	 */
	public static boolean place(String name, World world, BlockPos pos, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int x0 = pos.getX() - 8;
		int z0 = pos.getZ() - 8;
		switch (name) {
		case "city_block":
			cityBlock(b, x0, z0);
			break;
		case "ruined_building":
			ruinedBuilding(b, x0 + 3, z0 + 3, 10, 10, 3, 8);
			break;
		case "ruined_house":
			ruinedHouse(b, x0 + 3, z0 + 3, 10, 10, false);
			break;
		case "burnt_house":
			ruinedHouse(b, x0 + 3, z0 + 3, 10, 10, true);
			break;
		case "gas_station":
			gasStation(b, x0, z0);
			break;
		case "radio_tower":
			radioTower(b, x0, z0);
			break;
		case "crater":
			crater(b, x0 + 8, z0 + 8, 5, 4, false);
			break;
		case "road":
			road(b, x0, z0, true);
			break;
		case "car_wreck":
			car(b, x0 + 6, z0 + 7, EnumFacing.EAST, rnd.nextBoolean());
			break;
		case "shipwreck":
			shipwreck(b, x0, z0);
			break;
		default:
			return false;
		}
		return true;
	}

	/*
	 * ------------------------------------------------- small helpers
	 */

	protected static IBlockState asphalt(Random r) {
		int i = r.nextInt(100);
		if (i < 72) {
			return ASPHALT;
		} else if (i < 80) {
			return ASPHALT_LIGHT;
		} else if (i < 88) {
			return RuinBuilder.GRAVEL;
		} else if (i < 94) {
			return RuinBuilder.COARSE_DIRT;
		}
		return RuinBuilder.COBBLESTONE;
	}

	protected static IBlockState window(Random r) {
		int i = r.nextInt(100);
		if (i < 60) {
			return AIR;
		} else if (i < 85) {
			return GLASS_PANE;
		}
		return RuinBuilder.IRON_BARS;
	}

	protected static IBlockState rust(Random r, IBlockState body) {
		int i = r.nextInt(10);
		if (i == 0) {
			return RuinBuilder.terracotta(EnumDyeColor.ORANGE);
		} else if (i == 1) {
			return RuinBuilder.terracotta(EnumDyeColor.BROWN);
		}
		return body;
	}

	protected static EnumFacing randomFacing(Random r) {
		return EnumFacing.getHorizontal(r.nextInt(4));
	}

	/**
	 * monster spawner hole with zombies, bandits or mutants, if enabled in the config
	 */
	protected static void ruinSpawner(RuinBuilder b, int x, int y, int z) {
		if (!TGConfig.wastelandRuinSpawners) {
			return;
		}
		Random r = b.rnd;
		TGSpawnerTileEnt spawner = b.spawner(x, y, z, EnumMonsterSpawnerType.HOLE, 3 + r.nextInt(4), 2, 200, 2);
		switch (r.nextInt(3)) {
		case 0:
			StructureBuilder.addMob(spawner, Bandit.class, 1);
			break;
		case 1:
			StructureBuilder.addMob(spawner, SuperMutantBasic.class, 3);
			StructureBuilder.addMob(spawner, MutantWarrior.class, 1);
			break;
		default:
			StructureBuilder.addMob(spawner, ZombieSoldier.class, 2);
			StructureBuilder.addMob(spawner, ZombieFarmer.class, 2);
			StructureBuilder.addMob(spawner, ZombieMiner.class, 1);
			StructureBuilder.addMob(spawner, ZombiePoliceman.class, 2);
			break;
		}
	}

	/*
	 * ------------------------------------------------- roads
	 */

	protected static void highways(RuinBuilder b, int x0, int z0) {
		long seed = b.world.getSeed();
		int cx = x0 >> 4;
		int cz = z0 >> 4;
		int offX = (int) Math.floorMod(seed * 31L + 7L, (long) HIGHWAY_SPACING);
		int offZ = (int) Math.floorMod(seed * 17L + 3L, (long) HIGHWAY_SPACING);
		if (Math.floorMod(cz, HIGHWAY_SPACING) == offZ && segmentExists(seed, cx >> 2, cz, 1)) {
			road(b, x0, z0, true);
		}
		if (Math.floorMod(cx, HIGHWAY_SPACING) == offX && segmentExists(seed, cz >> 2, cx, 2)) {
			road(b, x0, z0, false);
		}
	}

	/**
	 * highways are broken into pieces of 64 blocks, about two thirds of them still exist
	 */
	protected static boolean segmentExists(long seed, int segment, int line, int salt) {
		long h = seed ^ (segment * 341873128712L) ^ (line * 132897987541L) ^ (salt * 0x9E3779B97F4A7C15L);
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return Math.floorMod(h, 100L) < 65;
	}

	/**
	 * 5 wide road with a dashed center line through the cell
	 */
	public static void road(RuinBuilder b, int x0, int z0, boolean alongX) {
		Random r = b.rnd;
		for (int a = 0; a < 16; a++) {
			for (int c = 5; c <= 9; c++) {
				int x = alongX ? x0 + a : x0 + c;
				int z = alongX ? z0 + c : z0 + a;
				if (r.nextInt(12) == 0) {
					continue;
				}
				int y = b.ground(x, z);
				IBlockState s = asphalt(r);
				if (c == 7 && Math.floorMod(alongX ? x : z, 4) < 2 && r.nextInt(4) != 0) {
					s = ROAD_LINE;
				}
				b.set(x, y, z, s);
				b.clearAbove(x, y + 1, z, 2);
			}
		}
		if (r.nextInt(12) == 0) {
			if (alongX) {
				car(b, x0 + 2 + r.nextInt(8), z0 + 5, EnumFacing.EAST, r.nextBoolean());
			} else {
				car(b, x0 + 9, z0 + 2 + r.nextInt(8), EnumFacing.SOUTH, r.nextBoolean());
			}
		}
	}

	/**
	 * iron bar pole with an arm towards the road, some of them still have a lamp, some fell over
	 */
	protected static void streetLamp(RuinBuilder b, int x, int y, int z, EnumFacing arm) {
		Random r = b.rnd;
		if (r.nextInt(6) == 0) {
			for (int i = 0; i < 4; i++) {
				int px = x + arm.getFrontOffsetX() * i;
				int pz = z + arm.getFrontOffsetZ() * i;
				b.set(px, b.ground(px, pz) + 1, pz, RuinBuilder.IRON_BARS);
			}
			return;
		}
		for (int i = 0; i < 4; i++) {
			b.set(x, y + i, z, RuinBuilder.IRON_BARS);
		}
		int ax = x + arm.getFrontOffsetX();
		int az = z + arm.getFrontOffsetZ();
		b.set(ax, y + 3, az, RuinBuilder.IRON_BARS);
		if (r.nextInt(100) < 40) {
			b.ceilingLamp(ax, y + 2, az);
		}
	}

	/**
	 * abandoned car, 4 blocks long in direction dir and 2 wide to the right of it
	 */
	public static void car(RuinBuilder b, int sx, int sz, EnumFacing dir, boolean burnt) {
		Random r = b.rnd;
		EnumFacing side = dir.rotateY();
		int max = 0;
		int min = 255;
		for (int i = 0; i < 4; i++) {
			for (int k = 0; k < 2; k++) {
				int g = b.ground(sx + dir.getFrontOffsetX() * i + side.getFrontOffsetX() * k, sz + dir.getFrontOffsetZ() * i + side.getFrontOffsetZ() * k);
				max = Math.max(max, g);
				min = Math.min(min, g);
			}
		}
		if (max - min > 2) {
			return;
		}
		int y = max + 1;
		IBlockState body = burnt ? RuinBuilder.terracotta(r.nextBoolean() ? EnumDyeColor.BLACK : EnumDyeColor.GRAY) : RuinBuilder.terracotta(CAR_COLORS[r.nextInt(CAR_COLORS.length)]);
		IBlockState wheel = ASPHALT;
		boolean trunkChest = !burnt && r.nextInt(8) == 0;
		for (int i = 0; i < 4; i++) {
			for (int k = 0; k < 2; k++) {
				int x = sx + dir.getFrontOffsetX() * i + side.getFrontOffsetX() * k;
				int z = sz + dir.getFrontOffsetZ() * i + side.getFrontOffsetZ() * k;
				for (int yy = b.ground(x, z) + 1; yy < y; yy++) {
					b.set(x, yy, z, RuinBuilder.COARSE_DIRT);
				}
				boolean end = i == 0 || i == 3;
				b.set(x, y, z, end ? wheel : rust(r, body));
				if (end) {
					if (trunkChest && i == 3 && k == 0) {
						b.lootChest(x, y + 1, z, dir, LOOT);
					} else {
						b.set(x, y + 1, z, STONE_SLAB);
					}
				} else {
					b.set(x, y + 1, z, burnt || r.nextInt(3) == 0 ? AIR : GLASS_PANE);
					b.set(x, y + 2, z, burnt && r.nextInt(3) == 0 ? AIR : rust(r, body));
				}
			}
		}
	}

	protected static void carWrecks(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		int n = 1 + r.nextInt(2);
		for (int c = 0; c < n; c++) {
			EnumFacing dir = randomFacing(r);
			car(b, x0 + 5 + r.nextInt(6), z0 + 5 + r.nextInt(6), dir, r.nextInt(3) == 0);
		}
		if (r.nextBoolean()) {
			b.rubblePile(x0 + 3 + r.nextInt(10), z0 + 3 + r.nextInt(10), 1, 1, x0, z0, x0 + 15, z0 + 15);
		}
	}

	/*
	 * ------------------------------------------------- city
	 */

	/**
	 * One block of a ruined city: roads along the cell borders, a sidewalk and a lot in the middle
	 */
	public static void cityBlock(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		int[][] g = new int[16][16];
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				g[i][j] = b.ground(x0 + i, z0 + j);
			}
		}
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				int x = x0 + i;
				int z = z0 + j;
				int y = g[i][j];
				boolean road = i < 2 || i > 13 || j < 2 || j > 13;
				boolean walk = !road && (i == 2 || i == 13 || j == 2 || j == 13);
				if (road) {
					IBlockState s = asphalt(r);
					//the center line of a road lies on the cell border, every border gets it once
					boolean line = (j == 0 && i >= 2 && i <= 13 && Math.floorMod(x, 4) < 2) || (i == 0 && j >= 2 && j <= 13 && Math.floorMod(z, 4) < 2);
					if (line && r.nextInt(5) != 0) {
						s = ROAD_LINE;
					}
					b.set(x, y, z, s);
					b.clearAbove(x, y + 1, z, 3);
				} else if (walk) {
					b.set(x, y, z, r.nextInt(8) == 0 ? RuinBuilder.CRACKED_BRICKS : SIDEWALK);
					b.clearAbove(x, y + 1, z, 2);
				}
			}
		}

		int[][] corners = { { 2, 2 }, { 13, 2 }, { 2, 13 }, { 13, 13 } };
		for (int[] c : corners) {
			if (r.nextInt(100) < 45) {
				streetLamp(b, x0 + c[0], g[c[0]][c[1]] + 1, z0 + c[1], c[0] < 8 ? EnumFacing.WEST : EnumFacing.EAST);
			}
		}

		int lot = r.nextInt(100);
		if (lot < 45) {
			ruinedBuilding(b, x0 + 3, z0 + 3, 10, 10, 3, 8);
		} else if (lot < 60) {
			ruinedHouse(b, x0 + 3, z0 + 3, 10, 10, r.nextInt(10) < 7);
		} else if (lot < 70) {
			collapsedLot(b, x0 + 3, z0 + 3, 10, 10);
		} else if (lot < 80) {
			parkingLot(b, x0 + 3, z0 + 3);
		} else if (lot < 90) {
			crater(b, x0 + 8, z0 + 8, 3 + r.nextInt(2), 2 + r.nextInt(2), false);
			b.rubblePile(x0 + 4 + r.nextInt(8), z0 + 4 + r.nextInt(8), 1, 2, x0 + 3, z0 + 3, x0 + 12, z0 + 12);
		} else {
			park(b, x0 + 3, z0 + 3);
		}

		//abandoned cars on the roads
		if (r.nextInt(100) < 55) {
			int n = 1 + r.nextInt(2);
			for (int c = 0; c < n; c++) {
				int a = 2 + r.nextInt(9);
				boolean forward = r.nextBoolean();
				boolean burnt = r.nextInt(3) == 0;
				switch (r.nextInt(4)) {
				case 0:
					if (forward) car(b, x0 + a, z0, EnumFacing.EAST, burnt); else car(b, x0 + a + 3, z0 + 1, EnumFacing.WEST, burnt);
					break;
				case 1:
					if (forward) car(b, x0 + a, z0 + 14, EnumFacing.EAST, burnt); else car(b, x0 + a + 3, z0 + 15, EnumFacing.WEST, burnt);
					break;
				case 2:
					if (forward) car(b, x0 + 1, z0 + a, EnumFacing.SOUTH, burnt); else car(b, x0, z0 + a + 3, EnumFacing.NORTH, burnt);
					break;
				default:
					if (forward) car(b, x0 + 15, z0 + a, EnumFacing.SOUTH, burnt); else car(b, x0 + 14, z0 + a + 3, EnumFacing.NORTH, burnt);
					break;
				}
			}
		}
		if (r.nextInt(100) < 30) {
			int i = r.nextBoolean() ? r.nextInt(2) : 14 + r.nextInt(2);
			int j = 2 + r.nextInt(12);
			if (r.nextBoolean()) {
				b.rubblePile(x0 + i, z0 + j, 1, 2, x0, z0, x0 + 15, z0 + 15);
			} else {
				b.rubblePile(x0 + j, z0 + i, 1, 2, x0, z0, x0 + 15, z0 + 15);
			}
		}
	}

	/**
	 * Ruined high rise: concrete floors, broken windows, a collapsed corner, holes in the floors,
	 * a jagged top with rebar sticking out and rubble around it
	 */
	public static void ruinedBuilding(RuinBuilder b, int lx, int lz, int lotW, int lotD, int minFloors, int maxFloors) {
		Random r = b.rnd;
		int sx = Math.max(5, lotW - r.nextInt(3));
		int sz = Math.max(5, lotD - r.nextInt(3));
		int bx = lx + r.nextInt(lotW - sx + 1);
		int bz = lz + r.nextInt(lotD - sz + 1);
		int floors = minFloors + r.nextInt(maxFloors - minFloors + 1);
		int h = floors * 4 + 1;
		int by = b.averageGround(bx, bz, bx + sx - 1, bz + sz - 1);

		IBlockState wall = BUILDING_WALLS[r.nextInt(BUILDING_WALLS.length)];
		IBlockState slab = SLAB_CONCRETE;
		IBlockState ladder = TGBlocks.LADDER_0.getDefaultState().withProperty(BlockTGLadder.FACING, EnumFacing.EAST);
		IBlockState[][][] g = new IBlockState[sx][h][sz];
		int doorSide = r.nextInt(4);

		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				boolean edgeX = x == 0 || x == sx - 1;
				boolean edgeZ = z == 0 || z == sz - 1;
				int along = edgeZ ? x : z;
				boolean doorWall = (doorSide == 0 && z == 0) || (doorSide == 1 && x == sx - 1) || (doorSide == 2 && z == sz - 1) || (doorSide == 3 && x == 0);
				for (int y = 0; y < h; y++) {
					int fy = y % 4;
					IBlockState s;
					if (fy == 0) {
						s = slab;
					} else if (!edgeX && !edgeZ) {
						s = AIR;
					} else if (edgeX && edgeZ) {
						s = wall;
					} else if (along % 2 == 1 && (fy == 2 || (y < 4 && doorWall && fy == 1))) {
						s = window(r);
					} else {
						s = wall;
					}
					g[x][y][z] = s;
				}
			}
		}
		//entrance
		int mid = (doorSide % 2 == 0 ? sx : sz) / 2 - 1;
		for (int k = mid; k <= mid + 1; k++) {
			for (int y = 1; y <= 2; y++) {
				switch (doorSide) {
				case 0: g[k][y][0] = AIR; break;
				case 1: g[sx - 1][y][k] = AIR; break;
				case 2: g[k][y][sz - 1] = AIR; break;
				default: g[0][y][k] = AIR; break;
				}
			}
		}
		//ladder through all floors in the west wall
		for (int y = 1; y < h - 1; y++) {
			g[1][y][2] = ladder;
		}

		//collapsed corner, cut off diagonally
		int removed = 0;
		int cornerX = r.nextBoolean() ? 0 : sx - 1;
		int cornerZ = r.nextBoolean() ? 0 : sz - 1;
		boolean corner = r.nextInt(10) < 7;
		if (corner) {
			int cut = 2 + r.nextInt(Math.max(1, h / 2));
			int slope = 1 + r.nextInt(3);
			for (int x = 0; x < sx; x++) {
				for (int z = 0; z < sz; z++) {
					int d = Math.abs(x - cornerX) + Math.abs(z - cornerZ);
					for (int y = Math.max(1, cut + slope * d); y < h; y++) {
						if (g[x][y][z] != AIR) {
							removed++;
						}
						g[x][y][z] = AIR;
					}
				}
			}
		}
		//dents in the top and a ragged edge
		int dents = 1 + r.nextInt(2);
		for (int n = 0; n < dents; n++) {
			int px = r.nextInt(sx);
			int pz = r.nextInt(sz);
			double rad = 2.0D + r.nextInt(3);
			int depth = 3 + r.nextInt(6);
			for (int x = 0; x < sx; x++) {
				for (int z = 0; z < sz; z++) {
					double d = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
					if (d < rad) {
						int loss = (int) (depth * (1.0D - d / rad));
						for (int y = Math.max(1, h - loss); y < h; y++) {
							g[x][y][z] = AIR;
						}
					}
				}
			}
		}
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				int loss = r.nextInt(3);
				for (int y = Math.max(1, h - loss); y < h; y++) {
					g[x][y][z] = AIR;
				}
				if (x > 0 && z > 0 && x < sx - 1 && z < sz - 1 && r.nextBoolean()) {
					g[x][h - 1][z] = AIR;
				}
			}
		}
		//holes in the floors, the pieces lie on the floor below
		for (int f = 1; f < floors; f++) {
			if (r.nextInt(10) < 4) {
				int hw = 2 + r.nextInt(3);
				int hd = 2 + r.nextInt(3);
				int hx = 1 + r.nextInt(Math.max(1, sx - 1 - hw));
				int hz = 1 + r.nextInt(Math.max(1, sz - 1 - hd));
				for (int x = hx; x < Math.min(sx - 1, hx + hw); x++) {
					for (int z = hz; z < Math.min(sz - 1, hz + hd); z++) {
						if (g[x][f * 4][z] == slab) {
							g[x][f * 4][z] = AIR;
							if (r.nextBoolean() && g[x][f * 4 - 3][z] == AIR) {
								g[x][f * 4 - 3][z] = b.rubbleState(false);
							}
						}
					}
				}
			}
		}
		//shell holes in the walls
		int holes = r.nextInt(3);
		for (int n = 0; n < holes; n++) {
			boolean onX = r.nextBoolean();
			int hx = onX ? (r.nextBoolean() ? 0 : sx - 1) : r.nextInt(sx);
			int hz = onX ? r.nextInt(sz) : (r.nextBoolean() ? 0 : sz - 1);
			int hy = 2 + r.nextInt(Math.max(1, h - 3));
			double rad = 1.3D + r.nextDouble() * 1.2D;
			for (int x = 0; x < sx; x++) {
				for (int z = 0; z < sz; z++) {
					for (int y = 1; y < h; y++) {
						double dx = x - hx, dy = y - hy, dz = z - hz;
						if (dx * dx + dy * dy + dz * dz < rad * rad) {
							g[x][y][z] = AIR;
						}
					}
				}
			}
		}

		//write the building
		IBlockState foundation = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				int wx = bx + x;
				int wz = bz + z;
				b.foundation(wx, wz, by - 1, foundation);
				for (int y = 0; y < h; y++) {
					b.set(wx, by + y, wz, g[x][y][z]);
				}
				b.clearAbove(wx, by + h, wz, 3);
			}
		}
		//rebar sticking out of the broken walls
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				if (x != 0 && z != 0 && x != sx - 1 && z != sz - 1) {
					continue;
				}
				int top = -1;
				for (int y = h - 1; y >= 0; y--) {
					if (g[x][y][z] == wall || g[x][y][z] == slab) {
						top = y;
						break;
					}
				}
				if (top >= 1 && r.nextInt(100) < (top < h - 1 ? 35 : 10)) {
					int len = 1 + r.nextInt(2);
					for (int i = 1; i <= len; i++) {
						if (top + i >= h || g[x][top + i][z] == AIR) {
							b.set(bx + x, by + top + i, bz + z, RuinBuilder.IRON_BARS);
						}
					}
				}
			}
		}
		//rubble of the collapsed corner
		if (corner) {
			int px = bx + (cornerX == 0 ? -1 : sx);
			int pz = bz + (cornerZ == 0 ? -1 : sz);
			int pileH = Math.min(4, 1 + removed / 40);
			b.rubblePile(px, pz, 2 + r.nextInt(2), pileH, lx - 3, lz - 3, lx + lotW + 2, lz + lotD + 2);
		}
		//inside rubble
		for (int n = 0; n < 3; n++) {
			int x = 1 + r.nextInt(sx - 2);
			int z = 1 + r.nextInt(sz - 2);
			if (g[x][1][z] == AIR && g[x][0][z] == slab) {
				b.set(bx + x, by + 1, bz + z, b.rubbleState(false));
			}
		}

		//loot and monsters
		if (r.nextInt(10) < 6) {
			for (int t = 0; t < 12; t++) {
				int f = r.nextInt(r.nextInt(floors) + 1);
				int x = 1 + r.nextInt(sx - 2);
				int z = 1 + r.nextInt(sz - 2);
				int y = f * 4;
				if (g[x][y][z] == slab && g[x][y + 1][z] == AIR && !(x == 1 && z == 2)) {
					b.lootChest(bx + x, by + y + 1, bz + z, randomFacing(r), LOOT);
					break;
				}
			}
		}
		if (r.nextInt(100) < 15) {
			for (int t = 0; t < 8; t++) {
				int x = 2 + r.nextInt(Math.max(1, sx - 4));
				int z = 2 + r.nextInt(Math.max(1, sz - 4));
				if (g[x][0][z] == slab && g[x][1][z] == AIR) {
					ruinSpawner(b, bx + x, by, bz + z);
					break;
				}
			}
		}
	}

	/**
	 * Small house without roof, burnt houses are black with ash on the floor, old ones mossy with cobwebs
	 */
	public static void ruinedHouse(RuinBuilder b, int lx, int lz, int lotW, int lotD, boolean burnt) {
		Random r = b.rnd;
		int sx = Math.min(lotW, 6 + r.nextInt(3));
		int sz = Math.min(lotD, 6 + r.nextInt(3));
		int bx = lx + r.nextInt(lotW - sx + 1);
		int bz = lz + r.nextInt(lotD - sz + 1);
		int floors = r.nextInt(3) == 0 ? 2 : 1;
		int h = floors * 4 + 1;
		int by = b.averageGround(bx, bz, bx + sx - 1, bz + sz - 1);

		IBlockState[] walls = burnt
			? new IBlockState[] { Blocks.BRICK_BLOCK.getDefaultState(), RuinBuilder.terracotta(EnumDyeColor.BLACK), RuinBuilder.COBBLESTONE,
				Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK), RuinBuilder.concreteColor(EnumDyeColor.BLACK) }
			: new IBlockState[] { RuinBuilder.CRACKED_BRICKS, RuinBuilder.COBBLESTONE, Blocks.MOSSY_COBBLESTONE.getDefaultState(),
				Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.SPRUCE), Blocks.BRICK_BLOCK.getDefaultState() };
		IBlockState floor = burnt ? Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK)
				: Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.OAK);
		IBlockState post = burnt ? Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK)
				: Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE);
		IBlockState mainWall = walls[r.nextInt(walls.length)];

		int doorSide = r.nextInt(4);
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				int wx = bx + x;
				int wz = bz + z;
				boolean edgeX = x == 0 || x == sx - 1;
				boolean edgeZ = z == 0 || z == sz - 1;
				int along = edgeZ ? x : z;
				int len = edgeZ ? sx : sz;
				boolean doorWall = (doorSide == 0 && z == 0) || (doorSide == 1 && x == sx - 1) || (doorSide == 2 && z == sz - 1) || (doorSide == 3 && x == 0);
				b.foundation(wx, wz, by - 1, RuinBuilder.COBBLESTONE);
				//walls get lower towards the top, the upper floor is mostly gone
				int wallTop = h - 1 - r.nextInt(3) - (r.nextInt(3) == 0 ? r.nextInt(h - 2) : 0);
				for (int y = 0; y < h; y++) {
					int fy = y % 4;
					IBlockState s;
					if (y == 0) {
						s = (edgeX || edgeZ) ? RuinBuilder.COBBLESTONE : floor;
					} else if (!edgeX && !edgeZ) {
						if (fy == 0 && y < h - 1 && r.nextInt(10) < 6) {
							s = floor;
						} else {
							s = AIR;
						}
					} else if (y > wallTop) {
						s = AIR;
					} else if (edgeX && edgeZ) {
						s = post;
					} else if (doorWall && along == len / 2 && fy >= 1 && fy <= 2 && y < 4) {
						s = AIR;
					} else if (fy == 2 && along % 3 == 1) {
						s = AIR;
					} else {
						s = r.nextInt(4) == 0 ? walls[r.nextInt(walls.length)] : mainWall;
					}
					b.set(wx, by + y, wz, s);
				}
				b.clearAbove(wx, by + h, wz, 3);

				if (!edgeX && !edgeZ) {
					if (burnt && r.nextInt(4) == 0) {
						b.set(wx, by + 1, wz, Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, r.nextBoolean() ? EnumDyeColor.BLACK : EnumDyeColor.GRAY));
					} else if (!burnt && r.nextInt(12) == 0) {
						b.set(wx, by + 1, wz, b.rubbleState(false));
					}
				}
			}
		}
		//cobwebs in the corners
		if (!burnt) {
			int[][] c = { { 1, 1 }, { sx - 2, 1 }, { 1, sz - 2 }, { sx - 2, sz - 2 } };
			for (int[] p : c) {
				if (r.nextInt(3) == 0) {
					b.set(bx + p[0], by + 3, bz + p[1], Blocks.WEB.getDefaultState());
				}
			}
		}
		//some of the walls lie next to the house
		b.rubblePile(bx + (r.nextBoolean() ? -1 : sx), bz + r.nextInt(sz), 1, 2, lx, lz, lx + lotW - 1, lz + lotD - 1);

		if (r.nextInt(100) < 45) {
			b.lootChest(bx + 1 + r.nextInt(sx - 2), by + 1, bz + 1 + r.nextInt(sz - 2), randomFacing(r), LOOT);
		}
		if (r.nextInt(100) < 6) {
			ruinSpawner(b, bx + sx / 2, by, bz + sz / 2);
		}
	}

	/**
	 * collapsed building: a mound of rubble with a few wall stubs and rebar
	 */
	protected static void collapsedLot(RuinBuilder b, int lx, int lz, int w, int d) {
		Random r = b.rnd;
		double cx = lx + (w - 1) / 2.0D;
		double cz = lz + (d - 1) / 2.0D;
		for (int x = lx; x < lx + w; x++) {
			for (int z = lz; z < lz + d; z++) {
				double dx = (x - cx) / (w / 2.0D);
				double dz = (z - cz) / (d / 2.0D);
				double dist = Math.sqrt(dx * dx + dz * dz);
				int height = (int) Math.round(4.0D * (1.0D - dist) + r.nextDouble() * 1.5D - 0.5D);
				int gy = b.ground(x, z);
				b.clearAbove(x, gy + 1, z, 3);
				for (int i = 1; i <= height; i++) {
					b.set(x, gy + i, z, b.rubbleState(true));
				}
			}
		}
		IBlockState wall = BUILDING_WALLS[r.nextInt(BUILDING_WALLS.length)];
		int stubs = 2 + r.nextInt(2);
		for (int n = 0; n < stubs; n++) {
			boolean alongX = r.nextBoolean();
			int len = 3 + r.nextInt(4);
			int sx = lx + r.nextInt(Math.max(1, w - (alongX ? len : 0)));
			int sz = lz + r.nextInt(Math.max(1, d - (alongX ? 0 : len)));
			int height = 2 + r.nextInt(5);
			for (int i = 0; i < len; i++) {
				int x = alongX ? sx + i : sx;
				int z = alongX ? sz : sz + i;
				if (x >= lx + w || z >= lz + d) {
					break;
				}
				int gy = b.ground(x, z);
				int top = Math.max(1, height - r.nextInt(3));
				for (int y = 1; y <= top; y++) {
					b.set(x, gy + y, z, wall);
				}
				if (r.nextInt(100) < 40) {
					b.set(x, gy + top + 1, z, RuinBuilder.IRON_BARS);
				}
			}
		}
		if (r.nextInt(100) < 25) {
			int x = lx + r.nextInt(w);
			int z = lz;
			b.lootChest(x, b.ground(x, z) + 1, z, EnumFacing.NORTH, LOOT);
		}
	}

	protected static void parkingLot(RuinBuilder b, int lx, int lz) {
		Random r = b.rnd;
		int y = b.averageGround(lx, lz, lx + 9, lz + 9);
		for (int x = lx; x < lx + 10; x++) {
			for (int z = lz; z < lz + 10; z++) {
				b.foundation(x, z, y - 1, RuinBuilder.COARSE_DIRT);
				IBlockState s = asphalt(r);
				if ((x - lx) % 3 == 0 && (z - lz < 3 || z - lz > 6)) {
					s = PARKING_LINE;
				}
				b.set(x, y, z, s);
				b.clearAbove(x, y + 1, z, 4);
			}
		}
		int cars = 1 + r.nextInt(3);
		for (int c = 0; c < cars; c++) {
			int bay = r.nextInt(3);
			if (r.nextBoolean()) {
				car(b, lx + 3 * bay + 2, lz, EnumFacing.SOUTH, r.nextInt(4) == 0);
			} else {
				car(b, lx + 3 * bay + 1, lz + 9, EnumFacing.NORTH, r.nextInt(4) == 0);
			}
		}
	}

	/**
	 * dead park with a bench, sometimes survivors had a small camp here
	 */
	protected static void park(RuinBuilder b, int lx, int lz) {
		Random r = b.rnd;
		for (int x = lx; x < lx + 10; x++) {
			for (int z = lz; z < lz + 10; z++) {
				int gy = b.ground(x, z);
				int i = r.nextInt(10);
				b.set(x, gy, z, i < 4 ? Blocks.GRASS.getDefaultState() : i < 8 ? RuinBuilder.COARSE_DIRT : Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL));
			}
		}
		WorldGenDeadTree tree = new WorldGenDeadTree(false, false, true);
		int trees = 1 + r.nextInt(3);
		for (int t = 0; t < trees; t++) {
			int x = lx + 1 + r.nextInt(8);
			int z = lz + 1 + r.nextInt(8);
			tree.generate(b.world, r, new BlockPos(x, b.ground(x, z) + 1, z));
		}
		int bx = lx + 2 + r.nextInt(5);
		int bz = lz + 8;
		int gy = b.ground(bx, bz);
		IBlockState bench = Blocks.OAK_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH);
		b.set(bx, gy + 1, bz, bench);
		b.set(bx + 1, gy + 1, bz, bench);

		if (r.nextInt(100) < 30) {
			//sandbag ring of a survivor camp
			int cx = lx + 5;
			int cz = lz + 4;
			for (int x = cx - 2; x <= cx + 2; x++) {
				for (int z = cz - 2; z <= cz + 2; z++) {
					boolean ring = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
					if (ring && !(x == cx && z == cz + 2)) {
						b.set(x, b.ground(x, z) + 1, z, TGBlocks.SANDBAGS.getDefaultState());
					}
				}
			}
			b.lootChest(cx, b.ground(cx, cz) + 1, cz, EnumFacing.SOUTH, LOOT);
			b.crate(cx - 1, b.ground(cx - 1, cz - 1) + 1, cz - 1);
		}
	}

	/*
	 * ------------------------------------------------- wasteland ruins
	 */

	/**
	 * bomb crater with a scorched floor and a rim of thrown out earth
	 */
	public static void crater(RuinBuilder b, int cx, int cz, int radius, int depth, boolean radioactive) {
		Random r = b.rnd;
		for (int x = cx - radius - 2; x <= cx + radius + 2; x++) {
			for (int z = cz - radius - 2; z <= cz + radius + 2; z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				int gy = b.ground(x, z);
				if (d <= radius) {
					int dep = (int) Math.round(depth * (1.0D - (d * d) / (radius * radius)));
					int floor = gy - dep;
					for (int y = floor + 1; y <= gy + 6; y++) {
						b.set(x, y, z, AIR);
					}
					IBlockState s;
					int i = r.nextInt(100);
					if (radioactive && i < 30) {
						s = RuinBuilder.terracotta(r.nextBoolean() ? EnumDyeColor.LIME : EnumDyeColor.GREEN);
					} else if (i < 35) {
						s = RuinBuilder.terracotta(EnumDyeColor.BLACK);
					} else if (i < 65) {
						s = RuinBuilder.COARSE_DIRT;
					} else if (i < 85) {
						s = RuinBuilder.GRAVEL;
					} else {
						s = RuinBuilder.COBBLESTONE;
					}
					b.set(x, floor, z, s);
				} else if (d <= radius + 1.5D && r.nextInt(3) != 0) {
					b.set(x, gy + 1, z, r.nextBoolean() ? RuinBuilder.COARSE_DIRT : b.rubbleState(true));
					if (r.nextInt(4) == 0) {
						b.set(x, gy + 2, z, RuinBuilder.COARSE_DIRT);
					}
				}
			}
		}
		if (radioactive) {
			uraniumPatch(b, cx, cz);
		}
	}

	/**
	 * a few blocks of uranium ore at the surface with a green stain around
	 */
	protected static void uraniumPatch(RuinBuilder b, int cx, int cz) {
		Random r = b.rnd;
		IBlockState ore = TGBlocks.TG_ORE.getDefaultState().withProperty(BlockTGOre.ORE_TYPE, EnumOreType.ORE_URANIUM);
		for (int x = cx - 2; x <= cx + 2; x++) {
			for (int z = cz - 2; z <= cz + 2; z++) {
				int d = Math.abs(x - cx) + Math.abs(z - cz);
				int gy = b.ground(x, z);
				if (d <= 1 && r.nextInt(3) != 0) {
					b.set(x, gy, z, ore);
				} else if (d <= 3 && r.nextBoolean()) {
					b.set(x, gy, z, RuinBuilder.terracotta(EnumDyeColor.LIME));
				}
			}
		}
	}

	/**
	 * small signs of the old world: bones, rusty barrels, scrap or a pile of rubble
	 */
	protected static void debris(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		int x = x0 + 3 + r.nextInt(10);
		int z = z0 + 3 + r.nextInt(10);
		int y = b.ground(x, z);
		switch (r.nextInt(4)) {
		case 0:
			//half buried skeleton
			b.set(x, y, z, Blocks.BONE_BLOCK.getDefaultState().withProperty(BlockRotatedPillar.AXIS, r.nextBoolean() ? EnumFacing.Axis.X : EnumFacing.Axis.Z));
			if (r.nextBoolean()) {
				b.set(x + 1, b.ground(x + 1, z), z, Blocks.BONE_BLOCK.getDefaultState());
			}
			break;
		case 1:
			int n = 1 + r.nextInt(3);
			for (int i = 0; i < n; i++) {
				int bx = x + r.nextInt(3) - 1;
				int bz = z + r.nextInt(3) - 1;
				b.set(bx, b.ground(bx, bz) + 1, bz, Blocks.CAULDRON.getDefaultState());
			}
			break;
		case 2:
			b.set(x, y + 1, z, ASPHALT);
			b.set(x + 1, y + 1, z, RuinBuilder.IRON_BARS);
			b.set(x, y + 1, z + 1, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_ORANGE));
			break;
		default:
			b.rubblePile(x, z, 1 + r.nextInt(2), 2, x0, z0, x0 + 15, z0 + 15);
			break;
		}
	}

	/**
	 * Abandoned gas station at a road: canopy with a collapsed corner, fuel pumps, a looted shop and a price sign
	 */
	public static void gasStation(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		Cell c = new Cell(x0, z0, r.nextInt(4));
		int y = b.averageGround(x0, z0, x0 + 15, z0 + 15);

		//road in front, concrete pad behind it
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 15; j++) {
				int x = c.x(i, j);
				int z = c.z(i, j);
				b.foundation(x, z, y - 1, RuinBuilder.COARSE_DIRT);
				IBlockState s;
				if (j < 3) {
					s = (j == 1 && i % 4 < 2) ? ROAD_LINE : asphalt(r);
				} else {
					s = r.nextInt(10) < 7 ? ASPHALT_LIGHT : (r.nextInt(3) == 0 ? ASPHALT : RuinBuilder.GRAVEL);
				}
				b.set(x, y, z, s);
				b.clearAbove(x, y + 1, z, 7);
			}
		}

		//canopy on four pillars, one corner came down
		IBlockState pillar = SIDEWALK;
		IBlockState roof = RuinBuilder.concreteColor(EnumDyeColor.WHITE);
		int[][] pillars = { { 2, 5 }, { 9, 5 }, { 2, 10 }, { 9, 10 } };
		int broken = r.nextInt(4);
		for (int p = 0; p < 4; p++) {
			int top = p == broken ? 1 + r.nextInt(2) : 4;
			for (int k = 1; k <= top; k++) {
				c.set(b, pillars[p][0], y + k, pillars[p][1], pillar);
			}
		}
		for (int i = 1; i <= 10; i++) {
			for (int j = 4; j <= 11; j++) {
				int d = Math.abs(i - pillars[broken][0]) + Math.abs(j - pillars[broken][1]);
				if (d <= 3) {
					if (r.nextInt(3) == 0) {
						c.set(b, i, y + 1, j, b.rubbleState(false));
					}
				} else if (r.nextInt(10) < 7) {
					c.set(b, i, y + 5, j, roof);
				}
			}
		}

		//fuel pumps with the nozzle on the side
		for (int pi : new int[] { 4, 7 }) {
			c.set(b, pi, y + 1, 7, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_RED));
			if (r.nextInt(4) != 0) {
				c.set(b, pi, y + 2, 7, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK));
			}
			c.set(b, pi, y + 1, 8, Blocks.STONE_BUTTON.getDefaultState().withProperty(BlockButton.FACING, c.f(EnumFacing.SOUTH)));
		}

		//the shop
		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);
		for (int i = 11; i <= 14; i++) {
			for (int j = 5; j <= 12; j++) {
				boolean edge = i == 11 || i == 14 || j == 5 || j == 12;
				for (int k = 1; k <= 4; k++) {
					IBlockState s = AIR;
					if (k == 4 && edge) {
						s = r.nextInt(5) == 0 ? AIR : wall;
					} else if (edge) {
						if (i == 11 && (j == 8 || j == 9) && k <= 2) {
							s = AIR;
						} else if (i == 11 && k <= 2) {
							s = window(r);
						} else {
							s = wall;
						}
					}
					c.set(b, i, y + k, j, s);
				}
				if (r.nextInt(10) < 6) {
					c.set(b, i, y + 5, j, SLAB_CONCRETE);
				}
			}
		}
		c.set(b, 13, y + 1, 7, StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER));
		c.set(b, 13, y + 1, 8, StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER));
		b.lootChest(c.x(13, 11), y + 1, c.z(13, 11), c.f(EnumFacing.WEST), LOOT_GAS_STATION);
		if (r.nextInt(100) < 20) {
			ruinSpawner(b, c.x(12, 10), y, c.z(12, 10));
		}

		//price sign
		for (int k = 1; k <= 5; k++) {
			c.set(b, 1, y + k, 3, RuinBuilder.IRON_BARS);
		}
		for (int i = 0; i <= 2; i++) {
			for (int k = 6; k <= 7; k++) {
				if (!(i == 2 && k == 7 && r.nextBoolean())) {
					c.set(b, i, y + k, 3, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_ORANGE));
				}
			}
		}

		//burnt car at the pumps
		car(b, c.x(3, 9), c.z(3, 9), c.f(EnumFacing.EAST), true);
	}

	/**
	 * Radio mast of iron bars, sometimes broken with the upper part lying on the ground, and an equipment hut with a working radio
	 */
	public static void radioTower(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		Cell c = new Cell(x0, z0, r.nextInt(4));
		int y = b.averageGround(Math.min(c.x(5, 5), c.x(9, 9)), Math.min(c.z(5, 5), c.z(9, 9)), Math.max(c.x(5, 5), c.x(9, 9)), Math.max(c.z(5, 5), c.z(9, 9)));
		IBlockState bars = RuinBuilder.IRON_BARS;

		for (int i = 5; i <= 9; i++) {
			for (int j = 5; j <= 9; j++) {
				b.foundation(c.x(i, j), c.z(i, j), y - 1, RuinBuilder.COBBLESTONE);
				c.set(b, i, y, j, SIDEWALK);
				b.clearAbove(c.x(i, j), y + 1, c.z(i, j), 4);
			}
		}

		int height = 18 + r.nextInt(10);
		boolean collapsed = r.nextInt(10) < 4;
		int top = collapsed ? height / 3 + r.nextInt(height / 3) : height;
		int[][] legs = { { 6, 6 }, { 8, 6 }, { 6, 8 }, { 8, 8 } };
		for (int k = 1; k <= top; k++) {
			for (int[] l : legs) {
				c.set(b, l[0], y + k, l[1], bars);
			}
			if (k % 4 == 0) {
				c.set(b, 7, y + k, 6, bars);
				c.set(b, 6, y + k, 7, bars);
				c.set(b, 8, y + k, 7, bars);
				c.set(b, 7, y + k, 8, bars);
			}
		}
		if (!collapsed) {
			for (int k = height - 3; k <= height + 3; k++) {
				c.set(b, 7, y + k, 7, bars);
			}
			c.set(b, 7, y + height + 4, 7, Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED));
		} else {
			//the upper part lies on the ground
			int length = Math.min(6, height - top);
			for (int i = 10; i < 10 + length; i++) {
				for (int j : new int[] { 6, 8 }) {
					c.set(b, i, c.ground(b, i, j) + 1, j, bars);
				}
				if (i % 2 == 0) {
					c.set(b, i, c.ground(b, i, 7) + 1, 7, bars);
				}
			}
			b.rubblePile(c.x(10, 7), c.z(10, 7), 1, 1, x0, z0, x0 + 15, z0 + 15);
		}

		//equipment hut
		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		int hy = c.ground(b, 2, 11);
		for (int i = 1; i <= 4; i++) {
			for (int j = 10; j <= 13; j++) {
				boolean edge = i == 1 || i == 4 || j == 10 || j == 13;
				b.foundation(c.x(i, j), c.z(i, j), hy - 1, RuinBuilder.COBBLESTONE);
				c.set(b, i, hy, j, SLAB_CONCRETE);
				for (int k = 1; k <= 2; k++) {
					c.set(b, i, hy + k, j, edge ? wall : AIR);
				}
				c.set(b, i, hy + 3, j, r.nextInt(6) == 0 ? AIR : SLAB_CONCRETE);
			}
		}
		c.set(b, 4, hy + 1, 11, AIR);
		c.set(b, 4, hy + 2, 11, AIR);
		b.lootChest(c.x(2, 12), hy + 1, c.z(2, 12), c.f(EnumFacing.EAST), LOOT);
		b.crate(c.x(3, 12), hy + 1, c.z(3, 12));
		if (TGBlocks.RADIO_STATION != null) {
			c.set(b, 2, hy + 2, 11, TGBlocks.RADIO_STATION.getDefaultState().withProperty(BlockRadioStation.FACING, c.f(EnumFacing.EAST)));
		}
	}

	/**
	 * Wreck of a ship on the dried sea floor, half buried in the salt and full of holes
	 */
	public static void shipwreck(RuinBuilder b, int x0, int z0) {
		Random r = b.rnd;
		Cell c = new Cell(x0, z0, r.nextInt(4));
		boolean wooden = r.nextBoolean();
		IBlockState hullA = wooden ? Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK) : RuinBuilder.terracotta(EnumDyeColor.ORANGE);
		IBlockState hullB = wooden ? Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.SPRUCE) : RuinBuilder.terracotta(EnumDyeColor.BROWN);
		IBlockState deck = wooden ? Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.OAK) : StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK);
		int mid = 7;
		int base = c.ground(b, 7, mid) - 2;
		int tilt = r.nextInt(3) - 1;

		for (int i = 1; i <= 14; i++) {
			int w = Math.max(1, (int) Math.round(3.0D * Math.sin(Math.PI * (i - 0.5D) / 14.0D)));
			int yb = base + (tilt * i) / 5;
			for (int k = 0; k <= 3; k++) {
				int hw = k == 0 ? Math.max(0, w - 1) : w;
				for (int j = mid - hw; j <= mid + hw; j++) {
					boolean shell = k == 0 || Math.abs(j - mid) == hw;
					IBlockState s;
					if (shell) {
						s = r.nextInt(3) == 0 ? hullB : hullA;
					} else if (k == 3) {
						s = r.nextInt(10) < 4 ? AIR : deck;
					} else if (k == 1 && r.nextInt(10) < 4) {
						s = Blocks.SAND.getDefaultState();
					} else {
						s = AIR;
					}
					c.set(b, i, yb + k, j, s);
				}
			}
		}
		//holes in the hull
		int holes = 2 + r.nextInt(2);
		for (int n = 0; n < holes; n++) {
			int hi = 2 + r.nextInt(12);
			int hj = r.nextBoolean() ? mid - 2 : mid + 2;
			int hy = base + 2 + r.nextInt(2) + (tilt * hi) / 5;
			double rad = 1.4D + r.nextDouble() * 0.8D;
			for (int i = hi - 2; i <= hi + 2; i++) {
				for (int j = hj - 2; j <= hj + 2; j++) {
					for (int k = -2; k <= 2; k++) {
						double d2 = (i - hi) * (i - hi) + (j - hj) * (j - hj) + k * k;
						if (d2 < rad * rad && i >= 1 && i <= 14) {
							IBlockState old = b.get(c.x(i, j), hy + k, c.z(i, j));
							if (old == hullA || old == hullB || old == deck) {
								c.set(b, i, hy + k, j, AIR);
							}
						}
					}
				}
			}
		}
		//broken mast
		int deckY = base + 4 + (tilt * 6) / 5;
		int mast = 3 + r.nextInt(5);
		for (int k = 0; k < mast; k++) {
			c.set(b, 6, deckY + k, mid, wooden ? Blocks.OAK_FENCE.getDefaultState() : RuinBuilder.IRON_BARS);
		}
		//small bridge on steel ships
		if (!wooden) {
			int cy = base + 4 + (tilt * 10) / 5;
			for (int i = 9; i <= 11; i++) {
				for (int j = mid - 1; j <= mid + 1; j++) {
					boolean edge = i == 9 || i == 11 || j != mid;
					c.set(b, i, cy, j, edge ? (r.nextInt(3) == 0 ? GLASS_PANE : hullA) : AIR);
					c.set(b, i, cy + 1, j, r.nextInt(3) == 0 ? AIR : STONE_SLAB);
				}
			}
		}
		if (r.nextInt(10) < 7) {
			int cy = base + 1 + (tilt * 4) / 5;
			b.lootChest(c.x(4, mid), cy, c.z(4, mid), c.f(EnumFacing.SOUTH), LOOT);
		}
		//bones and shells in the salt around the wreck
		for (int n = 0; n < 3; n++) {
			int i = 1 + r.nextInt(14);
			int j = r.nextBoolean() ? 1 + r.nextInt(2) : 13 + r.nextInt(2);
			c.set(b, i, c.ground(b, i, j), j, Blocks.BONE_BLOCK.getDefaultState());
		}
	}
}
