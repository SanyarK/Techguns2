package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.EnumOreType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.General;
import techguns.entities.npcs.HeavySoldier;
import techguns.entities.npcs.ZombieMiner;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.util.BlockUtils;

/**
 * Big underground military mine. A guarded mine head on the surface, a shaft down to the first level
 * with barracks, armory and a mining gallery, a staircase leads to the second level where the
 * General has his command center.
 */
public class UndergroundMilitaryMine extends WorldgenStructure {

	private static final ResourceLocation CHEST_LOOT = new ResourceLocation(Techguns.MODID, "chests/underground_mine");
	private static final ResourceLocation VAULT_LOOT = new ResourceLocation(Techguns.MODID, "chests/general_vault");

	protected static final int SURFACE_SIZE = 17;
	/** inner half size of the central hall on level 1 */
	protected static final int HALL = 5;

	protected static final int ROOM_BARRACKS = 0;
	protected static final int ROOM_ARMORY = 1;
	protected static final int ROOM_MINING = 2;
	protected static final int ROOM_STAIRS = 3;

	/** false: no General in the command center (the campaign fights him in his own bunker) */
	public boolean withGeneral = true;

	public UndergroundMilitaryMine() {
		super(SURFACE_SIZE, 40, SURFACE_SIZE, SURFACE_SIZE, 40, SURFACE_SIZE);
		this.setXZSize(SURFACE_SIZE, SURFACE_SIZE);
		this.lootTier = techguns.world.EnumLootType.TIER2;
	}

	@Override
	public void spawnStructureWorldgen(World world, int chunkX, int chunkZ, int sizeX, int sizeY, int sizeZ, Random rnd, Biome biome) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		int y = BlockUtils.getValidSpawnYArea(world, x, z, SURFACE_SIZE, SURFACE_SIZE, 6, this.getStep());
		//needs enough space below for 2 levels
		if (y < 45) {
			return;
		}
		this.setBlocks(world, x, y - 1, z, SURFACE_SIZE, sizeY, SURFACE_SIZE, 0, getBiomeColorTypeFromBiome(biome), rnd);
	}

	/** walking level of the first underground level for the given ground level */
	public static int levelOne(int ground) {
		return Math.max(20, ground - 24);
	}

	/** walking level of the second underground level (command center) for the given ground level */
	public static int levelTwo(int ground) {
		return levelOne(ground) - 10;
	}

	/** x coordinate for a point 'dist' blocks away from the center in direction dir (0=N,1=E,2=S,3=W) with a sideways offset */
	protected static int px(int cx, int dir, int dist, int lateral) {
		switch (dir) {
		case 1:
			return cx + dist;
		case 3:
			return cx - dist;
		default:
			return cx + lateral;
		}
	}

	protected static int pz(int cz, int dir, int dist, int lateral) {
		switch (dir) {
		case 0:
			return cz - dist;
		case 2:
			return cz + dist;
		default:
			return cz + lateral;
		}
	}

	protected static EnumFacing dirFacing(int dir) {
		switch (dir) {
		case 0:
			return EnumFacing.NORTH;
		case 1:
			return EnumFacing.EAST;
		case 2:
			return EnumFacing.SOUTH;
		default:
			return EnumFacing.WEST;
		}
	}

	protected static void roomAt(StructureBuilder b, int cx, int cz, int dir, int distA, int distB, int latA, int latB, int y1, int y2, IBlockState wall, IBlockState floor, IBlockState ceiling) {
		int xa = px(cx, dir, distA, latA);
		int xb = px(cx, dir, distB, latB);
		int za = pz(cz, dir, distA, latA);
		int zb = pz(cz, dir, distB, latB);
		b.room(Math.min(xa, xb), y1, Math.min(za, zb), Math.max(xa, xb), y2, Math.max(za, zb), wall, floor, ceiling);
	}

	protected static void tunnelAt(StructureBuilder b, int cx, int cz, int dir, int distA, int distB, int latA, int latB, int y1, int y2, IBlockState wall, IBlockState floor, IBlockState ceiling) {
		int xa = px(cx, dir, distA, latA);
		int xb = px(cx, dir, distB, latB);
		int za = pz(cz, dir, distA, latA);
		int zb = pz(cz, dir, distB, latB);
		b.tunnel(Math.min(xa, xb), y1, Math.min(za, zb), Math.max(xa, xb), y2, Math.max(za, zb), wall, floor, ceiling);
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);

		int ground = posY;
		int cx = posX + SURFACE_SIZE / 2;
		int cz = posZ + SURFACE_SIZE / 2;
		int level1 = levelOne(ground);
		int level2 = levelTwo(ground);

		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState floor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState ceiling = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);

		//assign rooms to the 4 directions
		int[] roomTypes = {ROOM_BARRACKS, ROOM_ARMORY, ROOM_MINING, ROOM_STAIRS};
		for (int i = roomTypes.length - 1; i > 0; i--) {
			int j = rnd.nextInt(i + 1);
			int t = roomTypes[i];
			roomTypes[i] = roomTypes[j];
			roomTypes[j] = t;
		}
		int[] tunnelLength = new int[4];
		for (int dir = 0; dir < 4; dir++) {
			tunnelLength[dir] = 6 + rnd.nextInt(6);
		}

		/*
		 * LEVEL 1
		 */
		b.room(cx - HALL, level1, cz - HALL, cx + HALL, level1 + 4, cz + HALL, wall, floor, ceiling);

		for (int dir = 0; dir < 4; dir++) {
			int start = HALL + 1 + tunnelLength[dir] + 1;
			switch (roomTypes[dir]) {
			case ROOM_ARMORY:
				roomAt(b, cx, cz, dir, start, start + 8, -4, 4, level1, level1 + 3, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK), floor, ceiling);
				break;
			case ROOM_MINING:
				roomAt(b, cx, cz, dir, start, start + 10, -4, 4, level1, level1 + 3, Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState());
				break;
			case ROOM_STAIRS:
				//command center on level 2, behind the staircase
				roomAt(b, cx, cz, dir, start + 11, start + 21, -6, 6, level2, level2 + 4, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK), floor, wall);
				break;
			case ROOM_BARRACKS:
			default:
				roomAt(b, cx, cz, dir, start, start + 8, -4, 4, level1, level1 + 3, wall, floor, ceiling);
				break;
			}
		}

		for (int dir = 0; dir < 4; dir++) {
			int length = tunnelLength[dir];
			boolean mine = roomTypes[dir] == ROOM_MINING;
			IBlockState tWall = mine ? Blocks.STONE.getDefaultState() : wall;
			tunnelAt(b, cx, cz, dir, HALL + 1, HALL + 1 + length + (roomTypes[dir] == ROOM_STAIRS ? 0 : 1), -1, 1, level1, level1 + 2, tWall, floor, tWall);

			for (int d = HALL + 3; d <= HALL + length; d += 4) {
				b.ceilingLamp(px(cx, dir, d, 0), level1 + 2, pz(cz, dir, d, 0));
			}
		}

		for (int dir = 0; dir < 4; dir++) {
			int start = HALL + 1 + tunnelLength[dir] + 1;
			switch (roomTypes[dir]) {
			case ROOM_ARMORY:
				this.decorateArmory(b, cx, cz, dir, start, level1);
				break;
			case ROOM_MINING:
				this.decorateMine(b, cx, cz, dir, start, level1);
				break;
			case ROOM_STAIRS:
				this.buildStairsAndCommandCenter(b, cx, cz, dir, start, level1, level2, wall, floor);
				break;
			case ROOM_BARRACKS:
			default:
				this.decorateBarracks(b, cx, cz, dir, start, level1);
				break;
			}
		}

		//hall details
		for (int i = -1; i <= 1; i += 2) {
			for (int j = -1; j <= 1; j += 2) {
				b.ceilingLamp(cx + i * 3, level1 + 4, cz + j * 3);
				b.crateStack(cx + i * HALL, level1, cz + j * HALL, 3);
			}
		}
		TGSpawnerTileEnt hallSpawner = b.spawner(cx - 3, level1, cz + 3, EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 2, 300, 1);
		StructureBuilder.addMob(hallSpawner, ArmySoldier.class, 2);
		StructureBuilder.addMob(hallSpawner, EliteSoldier.class, 1);

		/*
		 * SURFACE
		 */
		this.buildSurface(b, posX, posZ, cx, cz, ground, level1, wall, floor);
	}

	protected void buildSurface(StructureBuilder b, int posX, int posZ, int cx, int cz, int ground, int level1, IBlockState wall, IBlockState floor) {
		int max = SURFACE_SIZE - 1;
		b.prepareGround(posX, posZ, posX + max, posZ + max, ground + 1, 8, Blocks.DIRT.getDefaultState());
		b.fill(posX, ground, posZ, posX + max, ground, posZ + max, StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN));

		//sandbag walls with gaps in the middle of each side
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		for (int i = 0; i <= max; i++) {
			if (i < 7 || i > 9) {
				for (int h = 1; h <= 2; h++) {
					b.set(posX + i, ground + h, posZ, sandbags);
					b.set(posX + i, ground + h, posZ + max, sandbags);
					b.set(posX, ground + h, posZ + i, sandbags);
					b.set(posX + max, ground + h, posZ + i, sandbags);
				}
			}
		}

		//mine head building
		b.room(cx - 4, ground + 1, cz - 4, cx + 4, ground + 4, cz + 4, wall, StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN), StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER));
		b.clear(cx, ground + 1, cz - 5, cx, ground + 2, cz - 5);
		b.clear(cx, ground + 1, cz + 5, cx, ground + 2, cz + 5);
		b.clear(cx - 5, ground + 1, cz, cx - 5, ground + 2, cz);
		b.clear(cx + 5, ground + 1, cz, cx + 5, ground + 2, cz);

		//shaft down to the hall on level 1
		for (int y = level1 + 5; y < ground; y++) {
			for (int x = cx - 2; x <= cx + 2; x++) {
				for (int z = cz - 2; z <= cz + 2; z++) {
					if (x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2) {
						b.set(x, y, z, wall);
					}
				}
			}
		}
		b.clear(cx - 1, level1 + 5, cz - 1, cx + 1, ground, cz + 1);
		b.fill(cx, level1, cz - 3, cx, ground + 2, cz - 3, wall);
		b.ladder(cx, level1, ground + 1, cz - 2, EnumFacing.NORTH);

		//railing around the shaft
		IBlockState bars = Blocks.IRON_BARS.getDefaultState();
		for (int x = cx - 2; x <= cx + 2; x++) {
			b.set(x, ground + 1, cz + 2, bars);
		}
		for (int z = cz - 2; z <= cz + 1; z++) {
			b.set(cx - 2, ground + 1, z, bars);
			b.set(cx + 2, ground + 1, z, bars);
		}

		//interior
		b.ceilingLamp(cx - 3, ground + 4, cz - 3);
		b.ceilingLamp(cx + 3, ground + 4, cz - 3);
		b.ceilingLamp(cx - 3, ground + 4, cz + 3);
		b.ceilingLamp(cx + 3, ground + 4, cz + 3);
		b.crateStack(cx - 4, ground + 1, cz + 4, 2);
		b.crateStack(cx + 4, ground + 1, cz + 4, 2);
		b.crateStack(cx + 4, ground + 1, cz - 4, 2);

		//guards
		b.spawner(posX + 1, ground + 1, posZ + 1, EnumMonsterSpawnerType.SOLDIER_SPAWN, 2, 1, 300, 0, ArmySoldier.class);
		b.spawner(posX + max - 1, ground + 1, posZ + max - 1, EnumMonsterSpawnerType.SOLDIER_SPAWN, 2, 1, 300, 0, ArmySoldier.class);
		b.crateStack(posX + max - 2, ground + 1, posZ + 2, 3);
		b.crateStack(posX + 2, ground + 1, posZ + max - 2, 3);
	}

	protected void decorateBarracks(StructureBuilder b, int cx, int cz, int dir, int start, int y) {
		int center = start + 4;
		IBlockState bunk = Blocks.WOOL.getStateFromMeta(13);
		IBlockState post = Blocks.OAK_FENCE.getDefaultState();
		for (int d = start + 1; d <= start + 7; d += 2) {
			for (int lat = -4; lat <= 4; lat += 8) {
				int x = px(cx, dir, d, lat);
				int z = pz(cz, dir, d, lat);
				b.set(x, y, z, bunk);
				b.set(x, y + 1, z, post);
				b.set(x, y + 2, z, bunk);
			}
		}
		b.ceilingLamp(px(cx, dir, center, 0), y + 3, pz(cz, dir, center, 0));
		b.ceilingLamp(px(cx, dir, start + 1, 0), y + 3, pz(cz, dir, start + 1, 0));
		b.ceilingLamp(px(cx, dir, start + 7, 0), y + 3, pz(cz, dir, start + 7, 0));

		b.lootChest(px(cx, dir, start + 8, 0), y, pz(cz, dir, start + 8, 0), dirFacing(dir).getOpposite(), CHEST_LOOT);
		b.crateStack(px(cx, dir, start + 8, 2), y, pz(cz, dir, start + 8, 2), 2);
		b.crateStack(px(cx, dir, start + 8, -2), y, pz(cz, dir, start + 8, -2), 2);

		TGSpawnerTileEnt spawner = b.spawner(px(cx, dir, center, 0), y, pz(cz, dir, center, 0), EnumMonsterSpawnerType.SOLDIER_SPAWN, 5, 2, 250, 1);
		StructureBuilder.addMob(spawner, ArmySoldier.class, 3);
		StructureBuilder.addMob(spawner, EliteSoldier.class, 1);
	}

	protected void decorateArmory(StructureBuilder b, int cx, int cz, int dir, int start, int y) {
		int center = start + 4;
		for (int d = start; d <= start + 8; d += 2) {
			b.crateStack(px(cx, dir, d, -4), y, pz(cz, dir, d, -4), 3);
			b.crateStack(px(cx, dir, d, 4), y, pz(cz, dir, d, 4), 3);
		}
		b.lootChest(px(cx, dir, start + 8, -1), y, pz(cz, dir, start + 8, -1), dirFacing(dir).getOpposite(), CHEST_LOOT);
		b.lootChest(px(cx, dir, start + 8, 1), y, pz(cz, dir, start + 8, 1), dirFacing(dir).getOpposite(), CHEST_LOOT);

		b.ceilingLamp(px(cx, dir, start + 2, 0), y + 3, pz(cz, dir, start + 2, 0));
		b.ceilingLamp(px(cx, dir, start + 6, 0), y + 3, pz(cz, dir, start + 6, 0));

		TGSpawnerTileEnt spawner = b.spawner(px(cx, dir, center, 0), y, pz(cz, dir, center, 0), EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 1, 300, 1);
		StructureBuilder.addMob(spawner, HeavySoldier.class, 2);
		StructureBuilder.addMob(spawner, EliteSoldier.class, 1);
	}

	protected void decorateMine(StructureBuilder b, int cx, int cz, int dir, int start, int y) {
		Random rnd = b.rnd;
		IBlockState[] ores = {
			Blocks.COAL_ORE.getDefaultState(), Blocks.COAL_ORE.getDefaultState(), Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_ORE.getDefaultState(),
			Blocks.GOLD_ORE.getDefaultState(), Blocks.REDSTONE_ORE.getDefaultState(), Blocks.LAPIS_ORE.getDefaultState(), Blocks.DIAMOND_ORE.getDefaultState(),
			TGBlocks.TG_ORE.getStateFromMeta(EnumOreType.ORE_COPPER.ordinal()), TGBlocks.TG_ORE.getStateFromMeta(EnumOreType.ORE_TIN.ordinal()),
			TGBlocks.TG_ORE.getStateFromMeta(EnumOreType.ORE_LEAD.ordinal()), TGBlocks.TG_ORE.getStateFromMeta(EnumOreType.ORE_TITANIUM.ordinal()),
			TGBlocks.TG_ORE.getStateFromMeta(EnumOreType.ORE_URANIUM.ordinal())
		};

		//ores in the gallery walls and ceiling
		for (int d = start - 1; d <= start + 11; d++) {
			for (int lat = -5; lat <= 5; lat++) {
				for (int h = -1; h <= 4; h++) {
					boolean shell = d == start - 1 || d == start + 11 || lat == -5 || lat == 5 || h == -1 || h == 4;
					if (shell && h >= 0 && rnd.nextInt(7) == 0) {
						int x = px(cx, dir, d, lat);
						int z = pz(cz, dir, d, lat);
						if (b.get(x, y + h, z).getBlock() == Blocks.STONE) {
							b.set(x, y + h, z, ores[rnd.nextInt(ores.length)]);
						}
					}
				}
			}
		}

		//wooden supports
		IBlockState log = Blocks.LOG.getDefaultState();
		IBlockState planks = Blocks.PLANKS.getDefaultState();
		for (int d = start + 1; d <= start + 9; d += 4) {
			for (int h = 0; h <= 2; h++) {
				b.set(px(cx, dir, d, -4), y + h, pz(cz, dir, d, -4), log);
				b.set(px(cx, dir, d, 4), y + h, pz(cz, dir, d, 4), log);
			}
			for (int lat = -4; lat <= 4; lat++) {
				b.set(px(cx, dir, d, lat), y + 3, pz(cz, dir, d, lat), planks);
			}
			if (d + 2 <= start + 10) {
				b.ceilingLamp(px(cx, dir, d + 2, 0), y + 3, pz(cz, dir, d + 2, 0));
			}
		}

		//rails from the hall to the end of the gallery
		IBlockState rail = Blocks.RAIL.getStateFromMeta(dir == 0 || dir == 2 ? 0 : 1);
		for (int d = HALL + 1; d <= start + 10; d++) {
			b.set(px(cx, dir, d, 0), y, pz(cz, dir, d, 0), rail);
		}

		b.lootChest(px(cx, dir, start + 10, 3), y, pz(cz, dir, start + 10, 3), dirFacing(dir).getOpposite(), CHEST_LOOT);
		b.crateStack(px(cx, dir, start + 10, -3), y, pz(cz, dir, start + 10, -3), 2);
		b.crateStack(px(cx, dir, start + 9, -3), y, pz(cz, dir, start + 9, -3), 1);

		TGSpawnerTileEnt spawner = b.spawner(px(cx, dir, start + 5, 2), y, pz(cz, dir, start + 5, 2), EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 2, 250, 1);
		StructureBuilder.addMob(spawner, ArmySoldier.class, 2);
		StructureBuilder.addMob(spawner, ZombieMiner.class, 1);
	}

	protected void buildStairsAndCommandCenter(StructureBuilder b, int cx, int cz, int dir, int start, int level1, int level2, IBlockState wall, IBlockState floor) {
		EnumFacing out = dirFacing(dir);
		IBlockState stairs = TGBlocks.CONCRETE_STAIRS.getDefaultState();

		//staircase down to level 2, one step per block. Carve everything first, the shell of a step would overwrite the stairs of its neighbours
		for (int i = 1; i <= 10; i++) {
			int d = start - 1 + i;
			int walk = level1 - i;
			tunnelAt(b, cx, cz, dir, d, d, -1, 1, walk, walk + 3, wall, floor, wall);
		}
		for (int i = 1; i <= 10; i++) {
			int d = start - 1 + i;
			int walk = level1 - i;
			for (int lat = -1; lat <= 1; lat++) {
				b.stairs(px(cx, dir, d, lat), walk - 1, pz(cz, dir, d, lat), stairs, out.getOpposite());
			}
			if (i % 3 == 0) {
				b.ceilingLamp(px(cx, dir, d, 0), walk + 3, pz(cz, dir, d, 0));
			}
		}

		//door into the command center
		int doorDist = start + 10;
		b.clear(px(cx, dir, doorDist, -1), level2, pz(cz, dir, doorDist, -1), px(cx, dir, doorDist, 1), level2 + 2, pz(cz, dir, doorDist, 1));
		b.set(px(cx, dir, doorDist, -1), level2, pz(cz, dir, doorDist, -1), wall);
		b.set(px(cx, dir, doorDist, -1), level2 + 1, pz(cz, dir, doorDist, -1), wall);
		b.set(px(cx, dir, doorDist, 1), level2, pz(cz, dir, doorDist, 1), wall);
		b.set(px(cx, dir, doorDist, 1), level2 + 1, pz(cz, dir, doorDist, 1), wall);
		b.set(px(cx, dir, doorDist, -1), level2 + 2, pz(cz, dir, doorDist, -1), wall);
		b.set(px(cx, dir, doorDist, 0), level2 + 2, pz(cz, dir, doorDist, 0), wall);
		b.set(px(cx, dir, doorDist, 1), level2 + 2, pz(cz, dir, doorDist, 1), wall);
		b.door(px(cx, dir, doorDist, 0), level2, pz(cz, dir, doorDist, 0), out);

		//command center interior: start+11 .. start+21
		int ccStart = start + 11;
		IBlockState light = TGBlocks.NEONLIGHT_BLOCK.getDefaultState();
		for (int d = ccStart + 1; d <= ccStart + 9; d += 4) {
			for (int lat = -4; lat <= 4; lat += 4) {
				b.set(px(cx, dir, d, lat), level2 + 5, pz(cz, dir, d, lat), light);
			}
		}

		//map table
		IBlockState table = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN);
		for (int d = ccStart + 5; d <= ccStart + 6; d++) {
			for (int lat = -1; lat <= 1; lat++) {
				b.set(px(cx, dir, d, lat), level2, pz(cz, dir, d, lat), table);
			}
		}

		//crates in the corners
		b.crateStack(px(cx, dir, ccStart, -6), level2, pz(cz, dir, ccStart, -6), 3);
		b.crateStack(px(cx, dir, ccStart, 6), level2, pz(cz, dir, ccStart, 6), 3);
		b.crateStack(px(cx, dir, ccStart + 10, -6), level2, pz(cz, dir, ccStart + 10, -6), 3);
		b.crateStack(px(cx, dir, ccStart + 10, 6), level2, pz(cz, dir, ccStart + 10, 6), 3);

		//vault behind the general
		EnumFacing chestFacing = out.getOpposite();
		b.lootChest(px(cx, dir, ccStart + 10, -2), level2, pz(cz, dir, ccStart + 10, -2), chestFacing, VAULT_LOOT);
		b.lootChest(px(cx, dir, ccStart + 10, 2), level2, pz(cz, dir, ccStart + 10, 2), chestFacing, CHEST_LOOT);

		//the boss and his guards
		if (this.withGeneral) {
			b.spawner(px(cx, dir, ccStart + 8, 0), level2, pz(cz, dir, ccStart + 8, 0), EnumMonsterSpawnerType.SOLDIER_SPAWN, 1, 1, 60, 0, General.class);
		}
		b.spawner(px(cx, dir, ccStart + 3, -4), level2, pz(cz, dir, ccStart + 3, -4), EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1, EliteSoldier.class);
		b.spawner(px(cx, dir, ccStart + 3, 4), level2, pz(cz, dir, ccStart + 3, 4), EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1, EliteSoldier.class);
	}
}
