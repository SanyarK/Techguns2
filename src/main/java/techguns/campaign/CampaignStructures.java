package techguns.campaign;

import java.util.Random;

import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.HeavySoldier;
import techguns.entities.npcs.MutantWarrior;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.entities.npcs.SuperMutantElite;
import techguns.entities.npcs.ZombieSoldier;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.world.structures.StructureBuilder;
import techguns.world.structures.Tent;
import techguns.world.structures.WatchTowerSmall;
import techguns.world.structures.WorldgenStructure.BiomeColorType;
import techguns.world.wasteland.RuinBuilder;
import techguns.world.wasteland.WastelandRuins;

/**
 * Places of acts IV and V: the mutant zone, the ruins of the nuclear power plant, the Legion
 * headquarters with its bunker, the launch pad of the Purifier and the Hive under the crater.
 * Every builder moves the point to the ground level of the place and sets its points of interest.
 */
public class CampaignStructures {

	public static final ResourceLocation LAIR_LOOT = new ResourceLocation(Techguns.MODID, "chests/mutant_lair");
	public static final ResourceLocation FACTORY_LOOT = new ResourceLocation(Techguns.MODID, "chests/factory_building");
	public static final ResourceLocation BARRACKS_LOOT = new ResourceLocation(Techguns.MODID, "chests/militarybase_barracks");
	public static final ResourceLocation BUNKER_LOOT = new ResourceLocation(Techguns.MODID, "chests/militarybase_bunker");
	public static final ResourceLocation VAULT_LOOT = new ResourceLocation(Techguns.MODID, "chests/general_vault");

	protected static final IBlockState AIR = Blocks.AIR.getDefaultState();

	protected static IBlockState glass(EnumDyeColor color) {
		return Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, color);
	}

	protected static IBlockState wool(EnumDyeColor color) {
		return Blocks.WOOL.getStateFromMeta(color.getMetadata());
	}

	/*
	 * ------------------------------------------------- act IV: mutant zone
	 */

	/**
	 * Ruins around a radioactive crater, overgrown with the nests of mutant warriors
	 */
	public static void mutantZone(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int y = b.averageGround(cx - 20, cz - 20, cx + 20, cz + 20);
		WastelandRuins.crater(b, cx, cz, 6, 3, true);
		int[][] lots = { { -21, -20 }, { 11, -21 }, { -20, 10 }, { 12, 11 } };
		for (int[] lot : lots) {
			WastelandRuins.ruinedHouse(b, cx + lot[0], cz + lot[1], 9, 9, rnd.nextBoolean(), -1);
		}
		WastelandRuins.car(b, cx - 4, cz + 13, EnumFacing.EAST, true, -1);
		WastelandRuins.car(b, cx + 14, cz - 4, EnumFacing.SOUTH, true, -1);
		int[][] nests = { { -11, 0 }, { 11, 2 }, { 0, -12 }, { 2, 12 } };
		for (int i = 0; i < nests.length; i++) {
			nest(b, cx + nests[i][0], cz + nests[i][1], i == 0);
		}
		point.pos = new BlockPos(cx, y, cz);
		point.setArea(cx - 21, cz - 21, cx + 21, cz + 21);
	}

	/**
	 * a ring of bones and slime around the hole the warriors crawl out of
	 */
	protected static void nest(RuinBuilder b, int x, int z, boolean withChest) {
		int gy = b.ground(x, z);
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
				int d = Math.abs(i) + Math.abs(j);
				if (d > 4) {
					continue;
				}
				int g = b.ground(x + i, z + j);
				b.set(x + i, g, z + j, d <= 1 ? hiveBlock(b.rnd) : RuinBuilder.COARSE_DIRT);
				if (d >= 3 && b.rnd.nextInt(3) == 0) {
					b.set(x + i, g + 1, z + j, Blocks.BONE_BLOCK.getDefaultState());
				} else if (d == 2 && b.rnd.nextInt(4) == 0) {
					b.set(x + i, g + 1, z + j, Blocks.WEB.getDefaultState());
				}
			}
		}
		b.clearAbove(x, gy + 1, z, 3);
		TGSpawnerTileEnt spawner = b.spawner(x, gy + 1, z, EnumMonsterSpawnerType.HOLE, 3, 1, 400, 1);
		StructureBuilder.addMob(spawner, MutantWarrior.class, 2);
		StructureBuilder.addMob(spawner, SuperMutantBasic.class, 1);
		if (withChest) {
			int g = b.ground(x + 2, z + 2);
			b.clearAbove(x + 2, g + 1, z + 2, 2);
			b.lootChest(x + 2, g + 1, z + 2, EnumFacing.WEST, LAIR_LOOT);
		}
	}

	/**
	 * the warlord of the campaign is spawned by the mission, the spawner of the lair structure goes
	 * @return the spot of the warlord in the middle of the crater
	 */
	public static BlockPos removeCenterSpawner(World world, BlockPos center) {
		for (int y = center.getY() + 2; y >= center.getY() - 8; y--) {
			BlockPos p = new BlockPos(center.getX(), y, center.getZ());
			if (world.getBlockState(p).getBlock() == TGBlocks.MONSTER_SPAWNER) {
				world.setBlockToAir(p);
				return p;
			}
		}
		return center.up();
	}

	/*
	 * ------------------------------------------------- act V: ruins of the nuclear power plant
	 */

	/**
	 * Reactor hall with a breached wall and the glowing reactor pit, a collapsed cooling tower to the
	 * west and a chimney to the east. The reactor core lies in the container north of the pit.
	 */
	public static void reactorRuins(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int r = 9;
		int y = b.averageGround(cx - r, cz - r, cx + r, cz + r);
		IBlockState concrete = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState dark = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState pipes = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_PIPES);
		IBlockState white = RuinBuilder.concreteColor(EnumDyeColor.WHITE);
		IBlockState red = RuinBuilder.concreteColor(EnumDyeColor.RED);
		IBlockState yellow = RuinBuilder.concreteColor(EnumDyeColor.YELLOW);
		IBlockState black = RuinBuilder.concreteColor(EnumDyeColor.BLACK);
		IBlockState scaffold = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_SCAFFOLD);
		IBlockState panel = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);

		CampaignSiteBuilder.level(b, cx - r - 2, cz - r - 2, cx + r + 2, cz + r + 2, y, dark, 14);
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				boolean edge = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
				if (edge) {
					//the explosion tore open the south wall
					boolean breach = z == cz + r && Math.abs(x - cx) <= 3;
					int top = rnd.nextInt(4) == 0 ? 6 + rnd.nextInt(4) : 10;
					if (breach) {
						top = rnd.nextInt(3);
					}
					for (int h = 1; h <= top; h++) {
						b.set(x, y + h, z, h == 8 ? red : (rnd.nextInt(8) == 0 ? RuinBuilder.CRACKED_BRICKS : white));
					}
				} else if (z < cz - 3 && rnd.nextInt(4) != 0) {
					//what is left of the roof
					b.set(x, y + 11, z, concrete);
				}
			}
		}
		//reactor pit: lined with dark concrete, the glowing core under lime glass
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				boolean rim = Math.abs(i) == 2 || Math.abs(j) == 2;
				for (int d = 0; d <= 3; d++) {
					b.set(cx + i, y - d, cz + j, rim ? dark : AIR);
				}
				b.set(cx + i, y - 4, cz + j, Math.abs(i) <= 1 && Math.abs(j) <= 1 ? Blocks.SEA_LANTERN.getDefaultState() : dark);
				if (Math.abs(i) <= 1 && Math.abs(j) <= 1) {
					b.set(cx + i, y - 3, cz + j, glass(EnumDyeColor.LIME));
				}
				if (rim) {
					b.set(cx + i, y + 1, cz + j, RuinBuilder.IRON_BARS);
				}
				//hazard stripes around the pit
				if (Math.abs(i) == 2 || Math.abs(j) == 2) {
					b.set(cx + i + Integer.signum(i), y, cz + j + Integer.signum(j), ((cx + i + cz + j) & 1) == 0 ? yellow : black);
				}
			}
		}
		//catwalk over the pit with ladders at both ends
		for (int z = cz - r + 2; z <= cz + r - 1; z++) {
			if (z == cz - r + 2 || rnd.nextInt(7) != 0) {
				b.set(cx, y + 5, z, scaffold);
			}
		}
		b.ladder(cx, y + 1, y + 5, cz - r + 1, EnumFacing.NORTH);
		//pipes in the corners and along the west wall
		for (int h = 1; h <= 9; h++) {
			b.set(cx - r + 1, y + h, cz - r + 1, pipes);
			b.set(cx + r - 1, y + h, cz - r + 1, pipes);
			b.set(cx - r + 1, y + h, cz + r - 1, pipes);
		}
		for (int z = cz - r + 2; z <= cz + r - 2; z++) {
			b.set(cx - r + 1, y + 4, z, pipes);
		}
		//control desks along the north wall
		for (int x = cx - 6; x <= cx + 6; x++) {
			if (x != cx) {
				b.set(x, y + 1, cz - r + 1, panel);
			}
		}
		b.ceilingLamp(cx - 4, y + 10, cz - 4);
		b.ceilingLamp(cx + 4, y + 10, cz - 4);
		b.floorLamp(cx + 6, y + 1, cz + 6);
		//the core container north of the pit, on a raised platform
		BlockPos core = new BlockPos(cx, y + 2, cz - 5);
		for (int i = -1; i <= 1; i++) {
			b.set(cx + i, y + 1, cz - 5, dark);
			b.set(cx + i, y + 1, cz - 6, dark);
		}
		b.stairs(cx, y + 1, cz - 4, Blocks.STONE_BRICK_STAIRS.getDefaultState(), EnumFacing.NORTH);
		b.lootChest(cx - r + 2, y + 1, cz + r - 2, EnumFacing.EAST, FACTORY_LOOT);
		b.lootChest(cx + r - 2, y + 1, cz - r + 3, EnumFacing.WEST, FACTORY_LOOT);
		TGSpawnerTileEnt spawner = b.spawner(cx + 5, y + 1, cz + 3, EnumMonsterSpawnerType.HOLE, 4, 2, 300, 2);
		StructureBuilder.addMob(spawner, ZombieSoldier.class, 2);
		StructureBuilder.addMob(spawner, SuperMutantBasic.class, 1);

		coolingTower(b, cx - 25, cz, y);
		//chimney with red and white bands
		for (int h = 1; h <= 30; h++) {
			if (h > 26 && rnd.nextInt(3) == 0) {
				continue;
			}
			IBlockState band = (h / 4) % 2 == 0 ? white : red;
			for (int i = 0; i <= 1; i++) {
				for (int j = 0; j <= 1; j++) {
					b.set(cx + 13 + i, y + h, cz - 8 + j, band);
				}
			}
		}
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 1; j++) {
				b.foundation(cx + 13 + i, cz - 8 + j, y, concrete);
			}
		}
		b.rubblePile(cx + 5, cz + 13, 3, 2, cx - 30, cz - 30, cx + 30, cz + 30);
		b.rubblePile(cx - 12, cz - 11, 2, 2, cx - 30, cz - 30, cx + 30, cz + 30);
		WastelandRuins.crater(b, cx + 8, cz - 16, 3, 2, true);

		point.pos = new BlockPos(cx, y, cz);
		point.poi = core;
		point.setArea(cx - 36, cz - 20, cx + 15, cz + 13);
	}

	/**
	 * hyperbolic concrete shell, a part broke off and lies inside as rubble
	 */
	protected static void coolingTower(RuinBuilder b, int tx, int tz, int y) {
		IBlockState shell = RuinBuilder.concreteColor(EnumDyeColor.SILVER);
		IBlockState stain = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		int height = 24;
		double broken = b.rnd.nextDouble() * Math.PI * 2.0D;
		CampaignSiteBuilder.level(b, tx - 10, tz - 10, tx + 10, tz + 10, y, RuinBuilder.GRAVEL, height + 2);
		for (int x = tx - 10; x <= tx + 10; x++) {
			for (int z = tz - 10; z <= tz + 10; z++) {
				double dist = Math.sqrt((x - tx) * (x - tx) + (z - tz) * (z - tz));
				double angle = Math.atan2(z - tz, x - tx) - broken;
				while (angle > Math.PI) {
					angle -= Math.PI * 2.0D;
				}
				while (angle < -Math.PI) {
					angle += Math.PI * 2.0D;
				}
				double gap = Math.abs(Math.toDegrees(angle));
				for (int h = 1; h <= height; h++) {
					double t = (h - 15) / 15.0D;
					double radius = 6.0D + 3.0D * t * t;
					if (Math.abs(dist - radius) > 0.7D) {
						continue;
					}
					if ((gap < 35.0D && h > 3 + (int) (gap / 5.0D)) || b.rnd.nextInt(14) == 0) {
						continue;
					}
					b.set(x, y + h, z, h > 18 || b.rnd.nextInt(6) == 0 ? stain : shell);
				}
			}
		}
		b.rubblePile(tx, tz, 5, 2, tx - 6, tz - 6, tx + 6, tz + 6);
	}

	/*
	 * ------------------------------------------------- act V: Legion headquarters
	 */

	/** walking level of the bunker below the ground of the headquarters */
	public static final int HQ_DEPTH = 10;

	/**
	 * Walled compound with watch towers, tents and a command building. A ladder in the building leads
	 * down into the bunker: entry room, corridor, barracks, armory, generator room and the command
	 * center with the safe that holds the control module.
	 */
	public static void legionHQ(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int r = 16;
		int y = b.averageGround(cx - r, cz - r, cx + r, cz + r);
		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN);
		IBlockState wallLight = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);
		IBlockState floor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState grey = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState panel = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK);
		IBlockState table = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);
		IBlockState radio = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN);
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		IBlockState red = wool(EnumDyeColor.RED);
		IBlockState black = wool(EnumDyeColor.BLACK);

		/*
		 * surface
		 */
		CampaignSiteBuilder.level(b, cx - r, cz - r, cx + r, cz + r, y, RuinBuilder.GRAVEL, 10);
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				if (Math.abs(x - cx) <= 1 || Math.abs(z - cz) <= 1) {
					b.set(x, y, z, floor);
				}
				boolean edge = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
				boolean gate = z == cz + r && Math.abs(x - cx) <= 2;
				if (edge && !gate) {
					for (int h = 1; h <= 4; h++) {
						b.set(x, y + h, z, h == 4 ? wallLight : wall);
					}
					if (((x + z) & 1) == 0) {
						b.set(x, y + 5, z, RuinBuilder.IRON_BARS);
					}
				}
			}
		}
		BiomeColorType color = CampaignSiteBuilder.colorTypeFor(world, point.pos);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx - r + 1, y, cz - r + 1, 3, 8, 3, 2, color, rnd);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx + r - 3, y, cz - r + 1, 3, 8, 3, 3, color, rnd);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx - r + 1, y, cz + r - 3, 3, 8, 3, 1, color, rnd);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx + r - 3, y, cz + r - 3, 3, 8, 3, 0, color, rnd);
		new Tent(7, 4, 5, 7, 4, 5, 1).setBlocks(world, cx - r + 3, y, cz + 3, 7, 4, 5, 1, color, rnd);
		new Tent(7, 4, 5, 7, 4, 5, 1).setBlocks(world, cx + r - 9, y, cz + 3, 7, 4, 5, 3, color, rnd);
		//sandbag nests at the gate
		for (int i = -5; i <= 5; i++) {
			if (Math.abs(i) >= 3) {
				b.set(cx + i, y + 1, cz + r - 3, sandbags);
			}
		}
		//flag of the Legion
		for (int h = 1; h <= 8; h++) {
			b.set(cx + 10, y + h, cz - 12, Blocks.OAK_FENCE.getDefaultState());
		}
		b.set(cx + 11, y + 8, cz - 12, red);
		b.set(cx + 12, y + 8, cz - 12, black);
		b.set(cx + 11, y + 7, cz - 12, black);
		b.set(cx + 12, y + 7, cz - 12, red);
		b.crateStack(cx - 12, y + 1, cz - 3, 3);
		b.crateStack(cx - 12, y + 1, cz - 4, 2);
		b.crateStack(cx + 12, y + 1, cz + 11, 2);
		b.floorLamp(cx - 3, y + 1, cz + r - 2);
		b.floorLamp(cx + 3, y + 1, cz + r - 2);
		WastelandRuins.car(b, cx + 5, cz + 9, EnumFacing.NORTH, false, y + 1);

		//command building above the bunker entrance
		b.room(cx - 5, y + 1, cz - 10, cx + 5, y + 4, cz - 2, wall, floor, grey);
		b.clear(cx - 1, y + 1, cz - 1, cx + 1, y + 3, cz - 1);
		for (int x = cx - 4; x <= cx + 4; x++) {
			if (Math.abs(x - cx) > 1) {
				b.set(x, y + 1, cz - 10, radio);
			}
		}
		b.set(cx + 4, y + 1, cz - 5, table);
		b.set(cx + 4, y + 1, cz - 6, table);
		b.ceilingLamp(cx, y + 4, cz - 5);
		for (int h = 6; h <= 10; h++) {
			b.set(cx - 4, y + h, cz - 9, RuinBuilder.IRON_BARS);
		}
		b.set(cx - 4, y + 11, cz - 9, Blocks.REDSTONE_LAMP.getDefaultState());

		/*
		 * bunker
		 */
		int by = y - HQ_DEPTH;
		b.room(cx - 2, by, cz - 10, cx + 2, by + 3, cz - 6, grey, floor, grey);
		b.room(cx - 8, by, cz - 36, cx + 8, by + 4, cz - 25, panel, floor, grey);
		b.room(cx - 12, by, cz - 22, cx - 4, by + 3, cz - 14, grey, floor, grey);
		b.room(cx + 4, by, cz - 22, cx + 12, by + 3, cz - 17, grey, floor, grey);
		b.room(cx + 4, by, cz - 14, cx + 10, by + 3, cz - 11, grey, floor, grey);
		b.tunnel(cx - 1, by, cz - 24, cx + 1, by + 2, cz - 11, grey, floor, grey);
		b.tunnel(cx - 3, by, cz - 18, cx - 2, by + 2, cz - 17, grey, floor, grey);
		b.tunnel(cx + 2, by, cz - 20, cx + 3, by + 2, cz - 19, grey, floor, grey);
		b.tunnel(cx + 2, by, cz - 13, cx + 3, by + 2, cz - 12, grey, floor, grey);
		for (int z = cz - 23; z <= cz - 12; z += 5) {
			b.ceilingLamp(cx, by + 3, z);
		}

		//ladder shaft from the building floor down to the entry room, on a pillar in the room
		b.fill(cx - 1, by + 4, cz - 9, cx + 1, y - 1, cz - 7, grey);
		b.clear(cx, by + 4, cz - 8, cx, y, cz - 8);
		b.fill(cx, by, cz - 9, cx, by + 3, cz - 9, grey);
		b.ladder(cx, by, y, cz - 8, EnumFacing.NORTH);
		b.ceilingLamp(cx + 2, by + 3, cz - 7);

		//barracks: bunks and lockers
		for (int z = cz - 21; z <= cz - 15; z += 2) {
			b.set(cx - 11, by, z, Blocks.BED.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.WEST)
					.withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.HEAD));
			b.set(cx - 10, by, z, Blocks.BED.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.WEST)
					.withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.FOOT));
		}
		b.lootChest(cx - 5, by, cz - 21, EnumFacing.WEST, BARRACKS_LOOT);
		b.ceilingLamp(cx - 8, by + 3, cz - 18);
		b.spawner(cx - 7, by, cz - 16, EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1, ArmySoldier.class);

		//armory
		b.crateStack(cx + 11, by, cz - 21, 3);
		b.crateStack(cx + 11, by, cz - 20, 2);
		b.crateStack(cx + 10, by, cz - 21, 2);
		b.lootChest(cx + 11, by, cz - 18, EnumFacing.WEST, BUNKER_LOOT);
		b.lootChest(cx + 7, by, cz - 21, EnumFacing.SOUTH, BUNKER_LOOT);
		b.ceilingLamp(cx + 8, by + 3, cz - 19);

		//generator room
		for (int x = cx + 6; x <= cx + 9; x++) {
			b.set(x, by, cz - 13, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_ORANGE));
			b.set(x, by + 1, cz - 13, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_ORANGE));
		}
		b.set(cx + 9, by + 2, cz - 12, Blocks.REDSTONE_BLOCK.getDefaultState());
		b.set(cx + 9, by + 3, cz - 12, Blocks.REDSTONE_LAMP.getDefaultState());

		//command center: map table, radios, the banner and the safe
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 32; z <= cz - 29; z++) {
				b.set(x, by, z, table);
			}
		}
		for (int x = cx - 7; x <= cx + 7; x++) {
			if (x % 3 != 0) {
				b.set(x, by, cz - 36, radio);
			}
		}
		for (int h = 1; h <= 3; h++) {
			b.set(cx - 1, by + h, cz - 37, h % 2 == 0 ? black : red);
			b.set(cx, by + h, cz - 37, h % 2 == 0 ? red : black);
			b.set(cx + 1, by + h, cz - 37, h % 2 == 0 ? black : red);
		}
		b.ceilingLamp(cx - 4, by + 4, cz - 30);
		b.ceilingLamp(cx + 4, by + 4, cz - 30);
		b.ceilingLamp(cx, by + 4, cz - 34);
		b.lootChest(cx - 7, by, cz - 35, EnumFacing.EAST, VAULT_LOOT);
		TGSpawnerTileEnt guards = b.spawner(cx - 5, by, cz - 27, EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 2, 300, 2);
		StructureBuilder.addMob(guards, EliteSoldier.class, 2);
		StructureBuilder.addMob(guards, HeavySoldier.class, 1);
		//the safe: an iron alcove with the chest of the control module
		BlockPos safe = new BlockPos(cx + 6, by, cz - 35);
		b.fill(cx + 5, by, cz - 36, cx + 7, by + 2, cz - 34, Blocks.IRON_BLOCK.getDefaultState());
		b.clear(cx + 6, by, cz - 35, cx + 6, by + 1, cz - 34);

		point.pos = new BlockPos(cx, y, cz);
		point.poi = safe;
		point.setArea(cx - r, cz - r, cx + r, cz + r);
	}

	/*
	 * ------------------------------------------------- act V: launch site of the Purifier
	 */

	/**
	 * Concrete pad with the Purifier in the middle: a beacon on iron blocks, closed with an iron lid
	 * until it is charged, four capacitor columns with pipes, a console for doctor Volkov and a ring of sandbags.
	 */
	public static void launchSite(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int r = 12;
		int y = b.averageGround(cx - r, cz - r, cx + r, cz + r);
		IBlockState pad = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState yellow = RuinBuilder.concreteColor(EnumDyeColor.YELLOW);
		IBlockState black = RuinBuilder.concreteColor(EnumDyeColor.BLACK);
		IBlockState blue = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_BLUE);
		IBlockState pipes = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_PIPES);
		IBlockState console = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_BLUE);
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();

		CampaignSiteBuilder.level(b, cx - r, cz - r, cx + r, cz + r, y, pad, 16);
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
				if (d == r || d == 5) {
					b.set(x, y, z, ((x + z) & 1) == 0 ? yellow : black);
				}
				double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				boolean gap = Math.abs(x - cx) <= 1 || Math.abs(z - cz) <= 1;
				if (dist >= 9.5D && dist < 10.5D && !gap) {
					b.set(x, y + 1, z, sandbags);
				}
			}
		}
		//the Purifier
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				b.set(cx + i, y, cz + j, Blocks.IRON_BLOCK.getDefaultState());
				if (i != 0 || j != 0) {
					b.set(cx + i, y + 1, cz + j, Blocks.GLASS.getDefaultState());
					b.set(cx + i, y + 2, cz + j, glass(EnumDyeColor.LIME));
				}
			}
		}
		b.set(cx, y + 1, cz, Blocks.BEACON.getDefaultState());
		b.set(cx, y + 2, cz, Blocks.IRON_BLOCK.getDefaultState());
		for (int i = -3; i <= 3; i += 6) {
			for (int j = -3; j <= 3; j += 6) {
				for (int h = 1; h <= 5; h++) {
					b.set(cx + i, y + h, cz + j, blue);
				}
				b.set(cx + i, y + 6, cz + j, Blocks.SEA_LANTERN.getDefaultState());
			}
		}
		for (int k = -2; k <= 2; k++) {
			b.set(cx + k, y + 6, cz - 3, pipes);
			b.set(cx + k, y + 6, cz + 3, pipes);
			b.set(cx - 3, y + 6, cz + k, pipes);
			b.set(cx + 3, y + 6, cz + k, pipes);
		}
		//console of doctor Volkov
		for (int i = 0; i <= 2; i++) {
			b.set(cx + 6 + i, y + 1, cz + 7, console);
		}
		b.set(cx + 7, y + 2, cz + 7, StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER));
		b.floorLamp(cx + 9, y + 1, cz + 7);
		//floodlights, supplies and a radio mast
		b.floorLamp(cx - r + 1, y + 1, cz - r + 1);
		b.floorLamp(cx + r - 1, y + 1, cz - r + 1);
		b.floorLamp(cx - r + 1, y + 1, cz + r - 1);
		b.floorLamp(cx + r - 1, y + 1, cz + r - 1);
		b.crateStack(cx - 7, y + 1, cz + 7, 3);
		b.crateStack(cx - 8, y + 1, cz + 7, 2);
		b.crateStack(cx - 7, y + 1, cz - 7, 2);
		b.lootChest(cx - 8, y + 1, cz - 7, EnumFacing.EAST, CampaignSiteBuilder.CAMP_LOOT);
		for (int h = 1; h <= 10; h++) {
			b.set(cx + 8, y + h, cz - 8, RuinBuilder.IRON_BARS);
		}
		b.set(cx + 8, y + 11, cz - 8, Blocks.REDSTONE_LAMP.getDefaultState());

		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx, y + 1, cz);
		point.setArea(cx - r, cz - r, cx + r, cz + r);
	}

	/**
	 * the Purifier is charged: the lid over the beacon opens and the beam shines green
	 */
	public static void openPurifier(World world, BlockPos beacon) {
		if (!world.isBlockLoaded(beacon) || world.getBlockState(beacon).getBlock() != Blocks.BEACON) {
			return;
		}
		world.setBlockState(beacon.up(), glass(EnumDyeColor.LIME), 3);
		world.playSound(null, beacon, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0f, 1.2f);
		if (world instanceof WorldServer) {
			((WorldServer) world).spawnParticle(EnumParticleTypes.END_ROD, beacon.getX() + 0.5D, beacon.getY() + 2.0D, beacon.getZ() + 0.5D, 60, 0.5D, 1.5D, 0.5D, 0.05D);
		}
	}

	/*
	 * ------------------------------------------------- act V: the Hive
	 */

	/** depth of the crater floor and of the hive level below the ground */
	public static final int HIVE_CRATER_DEPTH = 7;
	public static final int HIVE_LEVEL_DEPTH = 21;
	/** the arena of Chimera lies north of the shaft */
	public static final int HIVE_ARENA_OFFSET = 26;

	/**
	 * A scorched crater with organic spires, a shaft with slime ladders in its middle and the Hive
	 * below: a hub, two egg chambers and the great chamber where Chimera waits. Built with the
	 * {@link StructureBuilder}, the organic walls only replace solid blocks so the caves connect.
	 */
	public static void hive(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int r = 22;
		int ground = b.averageGround(cx - r, cz - r, cx + r, cz + r);
		int floor = ground - HIVE_CRATER_DEPTH;
		int lvl = ground - HIVE_LEVEL_DEPTH;

		//the crater
		for (int x = cx - r - 3; x <= cx + r + 3; x++) {
			for (int z = cz - r - 3; z <= cz + r + 3; z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				int gy = b.ground(x, z);
				if (d <= r) {
					int fy = ground - (int) Math.round(HIVE_CRATER_DEPTH * (1.0D - (d * d) / (r * r)));
					for (int yy = fy + 1; yy <= Math.max(gy, ground) + 8; yy++) {
						b.set(x, yy, z, AIR);
					}
					b.set(x, fy, z, craterBlock(rnd, d / r));
					b.foundation(x, z, fy - 1, Blocks.STONE.getDefaultState());
				} else if (d <= r + 2.5D && rnd.nextInt(3) != 0) {
					b.set(x, gy + 1, z, rnd.nextBoolean() ? RuinBuilder.COARSE_DIRT : RuinBuilder.terracotta(EnumDyeColor.BLACK));
					if (rnd.nextInt(3) == 0) {
						b.set(x, gy + 2, z, RuinBuilder.COARSE_DIRT);
					}
				}
			}
		}
		//organic spires growing out of the crater
		for (int i = 0; i < 7; i++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double dist = 7.0D + rnd.nextDouble() * 11.0D;
			spire(b, cx + (int) (Math.cos(angle) * dist), cz + (int) (Math.sin(angle) * dist), 4 + rnd.nextInt(7));
		}

		//the Hive: hub under the shaft, egg chambers west and east, the great chamber north
		int arenaZ = cz - HIVE_ARENA_OFFSET;
		cave(b, cx, lvl, cz, 6.5D, 5.0D, 6.5D);
		cave(b, cx - 15, lvl, cz + 3, 5.0D, 4.0D, 5.0D);
		cave(b, cx + 15, lvl, cz - 3, 5.0D, 4.0D, 5.0D);
		cave(b, cx, lvl, arenaZ, 14.0D, 9.0D, 12.0D);
		worm(b, cx - 5, lvl, cz + 1, cx - 11, cz + 3, 2.0D);
		worm(b, cx + 5, lvl, cz - 1, cx + 11, cz - 3, 2.0D);
		worm(b, cx, lvl, cz - 5, cx, arenaZ + 11, 2.7D);

		//shaft with a slime ladder on a column of flesh
		IBlockState ladder = TGBlocks.SLIMY_LADDER.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
		for (int yy = lvl; yy <= floor; yy++) {
			for (int i = -2; i <= 2; i++) {
				for (int j = -2; j <= 2; j++) {
					boolean inner = Math.abs(i) <= 1 && Math.abs(j) <= 1;
					if (inner) {
						b.set(cx + i, yy, cz + j, AIR);
					} else if (yy > lvl + 5) {
						b.setIfNotAir(cx + i, yy, cz + j, hiveBlock(rnd));
					}
				}
			}
			b.set(cx, yy, cz - 2, hiveBlock(rnd));
			b.set(cx, yy, cz - 1, ladder);
		}
		//the ladder ends one block above the crater floor, bones mark the corners of the hole
		b.set(cx, floor + 1, cz - 2, Blocks.BONE_BLOCK.getDefaultState());
		b.set(cx, floor + 1, cz - 1, ladder);
		for (int i = -2; i <= 2; i += 4) {
			for (int j = -2; j <= 2; j += 4) {
				b.set(cx + i, floor + 1, cz + j, Blocks.BONE_BLOCK.getDefaultState());
				b.set(cx + i, floor + 2, cz + j, Blocks.BONE_BLOCK.getDefaultState());
			}
		}

		//light and life of the Hive
		glowPod(b, cx - 4, lvl, cz + 3);
		glowPod(b, cx + 4, lvl, cz - 3);
		glowPod(b, cx - 17, lvl, cz + 5);
		glowPod(b, cx + 17, lvl, cz - 5);
		for (int i = 0; i < 6; i++) {
			double angle = i * Math.PI / 3.0D + 0.3D;
			glowPod(b, cx + (int) (Math.cos(angle) * 10.0D), lvl, arenaZ + (int) (Math.sin(angle) * 8.0D));
		}
		//egg clusters and spawners in the chambers
		eggs(b, cx - 15, lvl, cz + 3);
		eggs(b, cx + 15, lvl, cz - 3);
		TGSpawnerTileEnt west = b.spawner(cx - 15, lvl, cz + 3, EnumMonsterSpawnerType.HOLE, 3, 1, 400, 2);
		StructureBuilder.addMob(west, SuperMutantBasic.class, 2);
		StructureBuilder.addMob(west, MutantWarrior.class, 1);
		TGSpawnerTileEnt east = b.spawner(cx + 15, lvl, cz - 3, EnumMonsterSpawnerType.HOLE, 3, 1, 400, 2);
		StructureBuilder.addMob(east, MutantWarrior.class, 2);
		StructureBuilder.addMob(east, SuperMutantElite.class, 1);
		b.lootChest(cx - 18, lvl, cz + 3, EnumFacing.EAST, LAIR_LOOT);
		b.lootChest(cx + 18, lvl, cz - 3, EnumFacing.WEST, LAIR_LOOT);
		//the great chamber: pillars of flesh and the trophies behind the boss
		for (int i = -1; i <= 1; i += 2) {
			for (int j = -1; j <= 1; j += 2) {
				pillar(b, cx + i * 7, lvl, arenaZ + j * 5);
			}
		}
		b.lootChest(cx - 2, lvl, arenaZ - 10, EnumFacing.SOUTH, VAULT_LOOT);
		b.lootChest(cx + 2, lvl, arenaZ - 10, EnumFacing.SOUTH, VAULT_LOOT);

		point.pos = new BlockPos(cx, floor, cz);
		point.poi = new BlockPos(cx, lvl, arenaZ);
		point.reachY = lvl + 3;
		point.setArea(cx - r - 3, cz - r - 3, cx + r + 3, cz + r + 3);
	}

	protected static IBlockState craterBlock(Random rnd, double rel) {
		int i = rnd.nextInt(100);
		if (rel < 0.35D && i < 30) {
			return hiveBlock(rnd);
		}
		if (i < 30) {
			return RuinBuilder.terracotta(EnumDyeColor.BLACK);
		} else if (i < 45) {
			return Blocks.MAGMA.getDefaultState();
		} else if (i < 70) {
			return RuinBuilder.COARSE_DIRT;
		} else if (i < 85) {
			return RuinBuilder.GRAVEL;
		}
		return Blocks.OBSIDIAN.getDefaultState();
	}

	/**
	 * living walls of the Hive
	 */
	protected static IBlockState hiveBlock(Random rnd) {
		int i = rnd.nextInt(100);
		if (i < 42) {
			return Blocks.NETHER_WART_BLOCK.getDefaultState();
		} else if (i < 62) {
			return RuinBuilder.terracotta(EnumDyeColor.RED);
		} else if (i < 74) {
			return TGBlocks.SLIMY_BLOCK.getDefaultState();
		} else if (i < 84) {
			return Blocks.BONE_BLOCK.getDefaultState();
		} else if (i < 94) {
			return Blocks.RED_NETHER_BRICK.getDefaultState();
		}
		return Blocks.MAGMA.getDefaultState();
	}

	/**
	 * dome over a flat floor, the walls only replace solid blocks
	 */
	protected static void cave(StructureBuilder b, int cx, int floorY, int cz, double rx, double height, double rz) {
		int ix = (int) Math.ceil(rx) + 2;
		int iz = (int) Math.ceil(rz) + 2;
		for (int x = cx - ix; x <= cx + ix; x++) {
			for (int z = cz - iz; z <= cz + iz; z++) {
				double nx = (x - cx) / rx;
				double nz = (z - cz) / rz;
				double flat = nx * nx + nz * nz;
				for (int y = floorY - 1; y <= floorY + (int) Math.ceil(height) + 2; y++) {
					double ny = (y - floorY) / height;
					double wobble = ((x * 7 + y * 13 + z * 5) & 7) * 0.02D;
					double d = flat + (y >= floorY ? ny * ny : 0.0D) + wobble;
					if (y >= floorY && d < 1.0D) {
						b.set(x, y, z, AIR);
					} else if (y == floorY - 1 && flat < 1.1D) {
						b.set(x, y, z, hiveBlock(b.rnd));
					} else if (d < 1.5D) {
						b.setIfNotAir(x, y, z, hiveBlock(b.rnd));
					}
				}
			}
		}
	}

	/**
	 * winding tunnel with a flat floor between two points on the same level
	 */
	protected static void worm(StructureBuilder b, int x1, int floorY, int z1, int x2, int z2, double radius) {
		double dx = x2 - x1;
		double dz = z2 - z1;
		double len = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
		int steps = (int) Math.ceil(len / 1.5D);
		for (int s = 0; s <= steps; s++) {
			double t = (double) s / steps;
			double sway = Math.sin(t * Math.PI * 2.0D) * 1.5D;
			double px = x1 + dx * t - dz / len * sway;
			double pz = z1 + dz * t + dx / len * sway;
			sphere(b, px, floorY, pz, radius);
		}
	}

	protected static void sphere(StructureBuilder b, double px, int floorY, double pz, double radius) {
		double cy = floorY + radius - 0.5D;
		int ir = (int) Math.ceil(radius) + 2;
		for (int x = (int) Math.floor(px) - ir; x <= (int) Math.floor(px) + ir; x++) {
			for (int z = (int) Math.floor(pz) - ir; z <= (int) Math.floor(pz) + ir; z++) {
				for (int y = floorY - 1; y <= (int) Math.ceil(cy + radius) + 1; y++) {
					double d = Math.sqrt((x + 0.5D - px) * (x + 0.5D - px) + (y + 0.5D - cy) * (y + 0.5D - cy) + (z + 0.5D - pz) * (z + 0.5D - pz));
					if (y >= floorY && d < radius) {
						b.set(x, y, z, AIR);
					} else if (y == floorY - 1 && d < radius + 1.0D) {
						b.set(x, y, z, hiveBlock(b.rnd));
					} else if (d < radius + 1.2D) {
						b.setIfNotAir(x, y, z, hiveBlock(b.rnd));
					}
				}
			}
		}
	}

	/** tapered column of flesh and bone */
	protected static void spire(RuinBuilder b, int x, int z, int height) {
		int gy = b.ground(x, z);
		for (int h = 1; h <= height; h++) {
			b.set(x, gy + h, z, hiveBlock(b.rnd));
			if (h <= height / 2) {
				b.set(x + 1, gy + h, z, hiveBlock(b.rnd));
				b.set(x, gy + h, z + 1, hiveBlock(b.rnd));
			}
		}
		b.set(x, gy + height + 1, z, Blocks.BONE_BLOCK.getDefaultState());
	}

	/** a glowing pod: sea lantern in lime glass on a stalk */
	protected static void glowPod(StructureBuilder b, int x, int floorY, int z) {
		b.set(x, floorY, z, Blocks.NETHER_WART_BLOCK.getDefaultState());
		b.set(x, floorY + 1, z, Blocks.SEA_LANTERN.getDefaultState());
		b.set(x, floorY + 2, z, glass(EnumDyeColor.LIME));
	}

	protected static void eggs(StructureBuilder b, int x, int floorY, int z) {
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				if ((i != 0 || j != 0) && b.rnd.nextInt(3) == 0 && b.isAir(x + i, floorY, z + j)) {
					b.set(x + i, floorY, z + j, TGBlocks.SLIMY_BLOCK.getDefaultState());
				}
			}
		}
	}

	protected static void pillar(StructureBuilder b, int x, int floorY, int z) {
		for (int h = 0; h <= 10; h++) {
			if (b.isAir(x, floorY + h, z)) {
				b.set(x, floorY + h, z, h % 4 == 3 ? Blocks.BONE_BLOCK.getDefaultState() : hiveBlock(b.rnd));
			}
		}
		b.set(x + 1, floorY, z, Blocks.NETHER_WART_BLOCK.getDefaultState());
		b.set(x - 1, floorY, z, Blocks.NETHER_WART_BLOCK.getDefaultState());
	}

	/*
	 * ------------------------------------------------- quest items in the places
	 */

	/**
	 * puts the quest item into the chest at pos (a chest is placed when there is none)
	 * @return false when the chest could not be placed
	 */
	public static boolean fillChest(World world, BlockPos pos, Item item) {
		if (item == null || !world.isBlockLoaded(pos)) {
			return false;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileEntityChest)) {
			world.setBlockState(pos, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, EnumFacing.SOUTH), 2);
			tile = world.getTileEntity(pos);
		}
		if (!(tile instanceof TileEntityChest)) {
			return false;
		}
		TileEntityChest chest = (TileEntityChest) tile;
		for (int i = 0; i < chest.getSizeInventory(); i++) {
			if (chest.getStackInSlot(i).getItem() == item) {
				return true;
			}
		}
		chest.setInventorySlotContents(13, new ItemStack(item));
		return true;
	}
}
