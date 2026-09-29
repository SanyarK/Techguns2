package techguns.campaign;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import techguns.TGBlocks;
import techguns.TGItems;
import techguns.Techguns;
import techguns.blocks.EnumCampaignTargetType;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.CapturedScientist;
import techguns.entities.npcs.LegionPrisoner;
import techguns.util.BlockUtils;
import techguns.world.structures.Airfield;
import techguns.world.structures.BunkerComplex;
import techguns.world.structures.CommandPost;
import techguns.world.structures.MilitaryCamp;
import techguns.world.structures.MutagenLabStructure;
import techguns.world.structures.MutantLair;
import techguns.world.structures.StructureBuilder;
import techguns.world.structures.Tent;
import techguns.world.structures.UndergroundMilitaryMine;
import techguns.world.structures.WatchTowerSmall;
import techguns.world.structures.WorldgenStructure;
import techguns.world.structures.WorldgenStructure.BiomeColorType;
import techguns.world.wasteland.RuinBuilder;
import techguns.world.wasteland.WastelandRuins;

/**
 * Builds the places of the campaign missions ({@link #place}) and fills them with the enemies,
 * captives, bosses and targets of the running mission ({@link #arm}).
 */
public class CampaignSiteBuilder {

	public static final ResourceLocation CAMP_LOOT = new ResourceLocation(Techguns.MODID, "chests/campaign_camp");
	public static final ResourceLocation HOSPITAL_LOOT = new ResourceLocation(Techguns.MODID, "chests/campaign_hospital");

	protected static final IBlockState AIR = Blocks.AIR.getDefaultState();

	/*
	 * ------------------------------------------------- placement
	 */

	public static void place(World world, CampaignWorldData wsd, EntityPlayerMP player, CampaignPoint point, Random rnd) {
		switch (point.site) {
		case STASH_HOUSE:
			stashHouse(world, point, rnd);
			break;
		case BANDIT_CAMP:
			banditCamp(world, point, rnd);
			break;
		case RADIO_MAST:
			radioMast(world, point, rnd);
			break;
		case COMMAND_POST:
			structure(world, point, new CommandPost(), rnd);
			wsd.setCommandPost(player, point.pos);
			break;
		case HOSPITAL:
			hospital(world, point, rnd);
			break;
		case AIRFIELD:
			structure(world, point, new Airfield(), rnd);
			break;
		case SCIENTIST_BUNKER:
		case GENERAL_BUNKER:
			structure(world, point, new BunkerComplex(), rnd);
			//walking level of the underground hall is 7 blocks below the ground, see BunkerComplex
			int yb = point.pos.getY() - 7;
			point.poi = point.site == CampaignSite.SCIENTIST_BUNKER ? new BlockPos(point.pos.getX() - 4, yb, point.pos.getZ() - 13)
					: new BlockPos(point.pos.getX(), yb, point.pos.getZ() - 11);
			break;
		case MILITARY_BASE:
			militaryBase(world, point, rnd);
			break;
		case CONVOY:
			convoy(world, point, rnd);
			break;
		case UNDERGROUND_MINE:
			UndergroundMilitaryMine mine = new UndergroundMilitaryMine();
			mine.withGeneral = false;
			structure(world, point, mine, rnd);
			point.reachY = UndergroundMilitaryMine.levelTwo(point.pos.getY()) + 3;
			break;
		case PRISON_CAMP:
			prisonCamp(world, point, rnd);
			break;
		case EVAC:
			evac(world, point, rnd);
			break;
		case MUTANT_LAIR:
			structure(world, point, new MutantLair(), rnd);
			break;
		case MUTAGEN_LAB:
			structure(world, point, new MutagenLabStructure(), rnd);
			point.poi = point.pos.add(MutagenLabStructure.BOSS_OFFSET_X, MutagenLabStructure.BOSS_OFFSET_Y, MutagenLabStructure.BOSS_OFFSET_Z);
			break;
		default:
			//no structure (yet): the point is on the ground
			point.pos = world.getTopSolidOrLiquidBlock(point.pos).down();
			break;
		}
		point.placed = true;
	}

	protected static BiomeColorType colorTypeFor(World world, BlockPos pos) {
		Biome biome = world.getBiome(pos);
		if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
			return BiomeColorType.SNOW;
		}
		if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA)) {
			return BiomeColorType.DESERT;
		}
		return BiomeColorType.WOODLAND;
	}

	/**
	 * walking level for a structure of the given area, like the worldgen of the structures
	 */
	protected static int spawnY(World world, int x, int z, int sizeX, int sizeZ, BlockPos center) {
		int y = BlockUtils.getValidSpawnYArea(world, x, z, sizeX, sizeZ, 6, 4);
		if (y < 20) {
			y = world.getHeight(center.getX(), center.getZ());
		}
		if (y < 20) {
			y = 64;
		}
		return y;
	}

	/**
	 * Places an existing Techguns structure centered on the point, the point then is the ground level at the center
	 */
	protected static void structure(World world, CampaignPoint point, WorldgenStructure structure, Random rnd) {
		int sizeX = structure.getSizeX(rnd);
		int sizeZ = structure.getSizeZ(rnd);
		int x = point.pos.getX() - sizeX / 2;
		int z = point.pos.getZ() - sizeZ / 2;
		int y = spawnY(world, x, z, sizeX, sizeZ, point.pos);
		structure.setBlocks(world, x, y - 1, z, sizeX, structure.getSizeY(rnd), sizeZ, 0, colorTypeFor(world, point.pos), rnd);
		point.pos = new BlockPos(x + sizeX / 2, y - 1, z + sizeZ / 2);
		point.setArea(x, z, x + sizeX - 1, z + sizeZ - 1);
	}

	/**
	 * levels a rectangle: foundation below, top block at y, air above
	 */
	protected static void level(RuinBuilder b, int x1, int z1, int x2, int z2, int y, IBlockState top, int clearHeight) {
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				b.foundation(x, z, y - 1, RuinBuilder.COBBLESTONE);
				b.set(x, y, z, top);
				b.clearAbove(x, y + 1, z, clearHeight);
			}
		}
	}

	protected static TileEntityChest chest(World world, BlockPos pos, boolean trapped, EnumFacing facing) {
		world.setBlockState(pos, (trapped ? Blocks.TRAPPED_CHEST : Blocks.CHEST).getDefaultState().withProperty(BlockChest.FACING, facing), 2);
		TileEntity tile = world.getTileEntity(pos);
		return tile instanceof TileEntityChest ? (TileEntityChest) tile : null;
	}

	/*
	 * ------------------------------------------------- act I
	 */

	/**
	 * Ruined house with a survivor stash (a trapped chest, so it never joins another chest) in the middle
	 */
	protected static void stashHouse(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int lx = point.pos.getX() - 4;
		int lz = point.pos.getZ() - 4;
		int by = b.averageGround(lx, lz, lx + 7, lz + 7);
		WastelandRuins.ruinedHouse(b, lx, lz, 8, 8, rnd.nextInt(3) == 0, by);
		//the house always covers the center of its 8x8 lot
		BlockPos stash = new BlockPos(lx + 4, by + 1, lz + 4);
		world.setBlockToAir(stash.up());
		TileEntityChest chest = chest(world, stash, true, EnumFacing.NORTH);
		if (chest != null) {
			int slot = 0;
			chest.setInventorySlotContents(slot++, new ItemStack(Items.BREAD, 2 + rnd.nextInt(3)));
			chest.setInventorySlotContents(slot++, new ItemStack(rnd.nextBoolean() ? Items.COOKED_BEEF : Items.APPLE, 2 + rnd.nextInt(2)));
			if (TGItems.BANDAGE != null) {
				chest.setInventorySlotContents(slot++, new ItemStack(TGItems.BANDAGE, 2));
			}
			chest.setInventorySlotContents(slot++, TGItems.newStack(TGItems.PISTOL_MAGAZINE, 2));
			if (rnd.nextBoolean()) {
				chest.setInventorySlotContents(slot++, TGItems.newStack(TGItems.PISTOL_ROUNDS, 8 + rnd.nextInt(8)));
			}
		}
		point.pos = new BlockPos(lx + 4, by, lz + 4);
		point.poi = stash;
		point.setArea(lx, lz, lx + 7, lz + 7);
	}

	/**
	 * Three tents around a fireplace of embers, crates and a chest
	 */
	protected static void banditCamp(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int y = b.averageGround(cx - 7, cz - 7, cx + 7, cz + 7);
		level(b, cx - 7, cz - 7, cx + 7, cz + 7, y, RuinBuilder.COARSE_DIRT, 6);
		for (int x = cx - 7; x <= cx + 7; x++) {
			for (int z = cz - 7; z <= cz + 7; z++) {
				if (rnd.nextInt(4) == 0) {
					b.set(x, y, z, RuinBuilder.GRAVEL);
				}
			}
		}
		//fireplace
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				b.set(cx + i, y, cz + j, (i == 0 && j == 0) ? Blocks.MAGMA.getDefaultState() : RuinBuilder.COBBLESTONE);
			}
		}
		BiomeColorType color = colorTypeFor(world, point.pos);
		new Tent(5, 4, 5, 5, 4, 5, 1).setBlocks(world, cx - 7, y, cz - 7, 5, 4, 5, 3, color, rnd);
		new Tent(5, 4, 5, 5, 4, 5, 1).setBlocks(world, cx + 3, y, cz - 7, 5, 4, 5, 3, color, rnd);
		new Tent(5, 4, 5, 5, 4, 5, 1).setBlocks(world, cx - 7, y, cz + 3, 5, 4, 5, 2, color, rnd);
		b.crateStack(cx + 5, y + 1, cz + 5, 2);
		b.crateStack(cx + 6, y + 1, cz + 5, 1);
		b.lootChest(cx + 5, y + 1, cz + 3, EnumFacing.WEST, CAMP_LOOT);
		b.floorLamp(cx + 3, y + 1, cz + 3);
		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx, y + 1, cz);
		point.setArea(cx - 7, cz - 7, cx + 7, cz + 7);
	}

	/** height of the radio mast */
	protected static final int MAST_HEIGHT = 18;

	/**
	 * Lattice radio mast with a ladder along its core and a platform on top, the antenna is broken
	 */
	protected static void radioMast(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int y = b.averageGround(cx - 4, cz - 4, cx + 4, cz + 4);
		IBlockState concrete = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState core = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK);
		IBlockState deck = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_SCAFFOLD);
		IBlockState bars = RuinBuilder.IRON_BARS;
		level(b, cx - 3, cz - 3, cx + 3, cz + 3, y, concrete, MAST_HEIGHT + 6);

		int top = y + MAST_HEIGHT;
		for (int k = y + 1; k <= top; k++) {
			b.set(cx, k, cz, core);
			b.set(cx - 1, k, cz - 1, bars);
			b.set(cx + 1, k, cz - 1, bars);
			b.set(cx - 1, k, cz + 1, bars);
			b.set(cx + 1, k, cz + 1, bars);
			if ((k - y) % 4 == 0) {
				b.set(cx - 1, k, cz, bars);
				b.set(cx + 1, k, cz, bars);
				b.set(cx, k, cz - 1, bars);
			}
		}
		//ladder on the south side of the core, through a hole in the platform
		b.ladder(cx, y + 1, top + 1, cz + 1, EnumFacing.NORTH);
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				if (i == 0 && j == 1) {
					continue;
				}
				b.set(cx + i, top + 1, cz + j, deck);
				if (Math.abs(i) == 2 || Math.abs(j) == 2) {
					b.set(cx + i, top + 2, cz + j, bars);
				}
			}
		}
		//broken antenna: a stub and a torn cable
		b.set(cx, top + 2, cz - 1, bars);
		b.set(cx, top + 3, cz - 1, Blocks.WEB.getDefaultState());
		b.set(cx - 1, top + 2, cz - 1, Blocks.REDSTONE_LAMP.getDefaultState());

		//equipment hut east of the mast
		for (int i = 3; i <= 6; i++) {
			for (int j = -2; j <= 1; j++) {
				boolean edge = i == 3 || i == 6 || j == -2 || j == 1;
				b.foundation(cx + i, cz + j, y - 1, RuinBuilder.COBBLESTONE);
				b.set(cx + i, y, cz + j, concrete);
				for (int k = 1; k <= 2; k++) {
					b.set(cx + i, y + k, cz + j, edge ? concrete : AIR);
				}
				b.set(cx + i, y + 3, cz + j, concrete);
			}
		}
		b.set(cx + 3, y + 1, cz, AIR);
		b.set(cx + 3, y + 2, cz, AIR);
		b.lootChest(cx + 5, y + 1, cz - 1, EnumFacing.WEST, CAMP_LOOT);
		b.ceilingLamp(cx + 4, y + 2, cz - 1);

		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx, top + 2, cz);
		point.setArea(cx - 3, cz - 3, cx + 6, cz + 3);
	}

	/**
	 * the antenna parts were delivered: the mast works again
	 */
	public static void onDelivered(World world, CampaignPoint point) {
		if (point.site != CampaignSite.RADIO_MAST || point.poi == null) {
			return;
		}
		//the antenna stub stands north of the platform center
		BlockPos base = point.poi.add(0, 0, -1);
		for (int k = 0; k <= 4; k++) {
			world.setBlockState(base.up(k), Blocks.IRON_BARS.getDefaultState(), 2);
		}
		world.setBlockState(base.up(5), Blocks.GLOWSTONE.getDefaultState(), 2);
		//the signal lamp next to it gets power
		world.setBlockState(base.add(-1, 0, 0), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
		world.setBlockState(base.add(-1, 1, 0), Blocks.REDSTONE_LAMP.getDefaultState(), 3);
	}

	/*
	 * ------------------------------------------------- act II
	 */

	/**
	 * Two storey ruin of a hospital: red cross over the entrance, wards with beds, a ladder to the
	 * upper floor, three chests with medicine (one upstairs)
	 */
	protected static void hospital(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int x0 = cx - 8;
		int x1 = cx + 8;
		int z0 = cz - 5;
		int z1 = cz + 5;
		int y = b.averageGround(x0, z0, x1, z1);
		IBlockState white = RuinBuilder.concreteColor(EnumDyeColor.WHITE);
		IBlockState grey = RuinBuilder.concreteColor(EnumDyeColor.SILVER);
		IBlockState red = RuinBuilder.concreteColor(EnumDyeColor.RED);
		IBlockState floor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);

		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				b.foundation(x, z, y - 1, RuinBuilder.COBBLESTONE);
				b.set(x, y, z, floor);
				b.clearAbove(x, y + 1, z, 10);
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
				int along = (z == z0 || z == z1) ? x - x0 : z - z0;
				//the upper floor walls are partly gone
				int wallTop = rnd.nextInt(4) == 0 ? 5 + rnd.nextInt(2) : 7;
				for (int h = 1; h <= 7; h++) {
					if (h == 4) {
						if (!edge && rnd.nextInt(8) == 0) {
							continue;
						}
						b.set(x, y + h, z, floor);
						continue;
					}
					if (!edge || h > wallTop) {
						continue;
					}
					if (!corner && (h == 2 || h == 6) && along % 3 == 1) {
						b.set(x, y + h, z, rnd.nextInt(3) == 0 ? Blocks.GLASS_PANE.getDefaultState() : AIR);
					} else {
						b.set(x, y + h, z, corner ? grey : (rnd.nextInt(10) == 0 ? RuinBuilder.CRACKED_BRICKS : white));
					}
				}
				//what is left of the roof
				if (rnd.nextInt(3) != 0) {
					b.set(x, y + 8, z, rnd.nextInt(5) == 0 ? RuinBuilder.COBBLESTONE : floor);
				}
			}
		}
		//entrance and red cross on the north front
		for (int h = 1; h <= 2; h++) {
			b.set(cx, y + h, z0, AIR);
			b.set(cx + 1, y + h, z0, AIR);
		}
		b.set(cx, y + 5, z0, red);
		b.set(cx, y + 6, z0, red);
		b.set(cx, y + 7, z0, red);
		b.set(cx - 1, y + 6, z0, red);
		b.set(cx + 1, y + 6, z0, red);

		//wall between the two wards with a doorway
		for (int z = z0 + 1; z < z1; z++) {
			for (int h = 1; h <= 3; h++) {
				b.set(cx - 1, y + h, z, (z == cz || z == cz + 1) && h <= 2 ? AIR : white);
			}
		}
		//beds along the south wall
		IBlockState bed = Blocks.BED.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
		for (int x = x0 + 2; x <= cx - 3; x += 2) {
			b.set(x, y + 1, z1 - 2, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT));
			b.set(x, y + 1, z1 - 1, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD));
		}
		for (int x = cx + 3; x < x1 - 1; x += 2) {
			if (rnd.nextBoolean()) {
				b.set(x, y + 1, z1 - 1, RuinBuilder.IRON_BARS);
			}
		}
		//ladder to the upper floor along the west wall
		b.ladder(x0 + 1, y + 1, y + 4, cz, EnumFacing.WEST);
		b.set(x0 + 1, y + 5, cz, AIR);

		//rubble and cobwebs
		for (int i = 0; i < 6; i++) {
			int rx = x0 + 2 + rnd.nextInt(x1 - x0 - 3);
			int rz = z0 + 2 + rnd.nextInt(z1 - z0 - 3);
			if (b.isAir(rx, y + 1, rz)) {
				b.set(rx, y + 1, rz, rnd.nextBoolean() ? b.rubbleState(false) : Blocks.WEB.getDefaultState());
			}
		}

		//medicine: two chests in the wards, one upstairs
		BlockPos[] chests = { new BlockPos(x0 + 1, y + 1, z0 + 1), new BlockPos(x1 - 1, y + 1, z1 - 1), new BlockPos(cx + 4, y + 5, cz) };
		EnumFacing[] facings = { EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST };
		for (int i = 0; i < chests.length; i++) {
			BlockPos p = chests[i];
			b.set(p.getX(), p.getY() - 1, p.getZ(), floor);
			world.setBlockToAir(p.up());
			TileEntityChest chest = chest(world, p, false, facings[i]);
			if (chest != null) {
				chest.setInventorySlotContents(13, new ItemStack(TGItems.MEDICINE));
				chest.setLootTable(HOSPITAL_LOOT, rnd.nextLong());
			}
		}
		b.ceilingLamp(cx - 4, y + 3, cz);
		b.ceilingLamp(cx + 4, y + 3, cz);

		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx + 2, y + 1, cz);
		point.setArea(x0, z0, x1, z1);
	}

	/*
	 * ------------------------------------------------- act III
	 */

	/** size of the Legion base built by the campaign */
	protected static final int BASE_SIZE = 40;

	protected static void militaryBase(World world, CampaignPoint point, Random rnd) {
		int x = point.pos.getX() - BASE_SIZE / 2;
		int z = point.pos.getZ() - BASE_SIZE / 2;
		int y = spawnY(world, x, z, BASE_SIZE, BASE_SIZE, point.pos);
		MilitaryCamp camp = new MilitaryCamp(4, rnd);
		camp.init(x, y, z, BASE_SIZE, BASE_SIZE);
		camp.setBlocks(world, rnd);
		point.pos = new BlockPos(x + BASE_SIZE / 2, y - 1, z + BASE_SIZE / 2);
		point.setArea(x, z, x + BASE_SIZE - 1, z + BASE_SIZE - 1);
	}

	/**
	 * Legion vehicles stopped on an old road, with sandbag cover
	 */
	protected static void convoy(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int y = b.averageGround(cx - 16, cz - 3, cx + 15, cz + 3);
		for (int i = -16; i < 16; i += 16) {
			//road cells are 16x16 with the lanes in the middle
			WastelandRuins.road(b, cx + i, cz - 7, true);
		}
		WastelandRuins.car(b, cx - 12, cz - 1, EnumFacing.EAST, false, -1);
		WastelandRuins.car(b, cx - 3, cz - 1, EnumFacing.EAST, false, -1);
		WastelandRuins.car(b, cx + 6, cz + 1, EnumFacing.EAST, rnd.nextBoolean(), -1);
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		for (int i = -6; i <= 6; i += 3) {
			b.set(cx + i, b.ground(cx + i, cz - 4) + 1, cz - 4, sandbags);
			b.set(cx + i + 1, b.ground(cx + i + 1, cz + 4) + 1, cz + 4, sandbags);
		}
		b.crateStack(cx + 1, b.ground(cx + 1, cz + 3) + 1, cz + 3, 2);
		b.crateStack(cx - 8, b.ground(cx - 8, cz - 3) + 1, cz - 3, 2);
		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx, y + 1, cz);
		point.setArea(cx - 16, cz - 7, cx + 15, cz + 8);
	}

	/**
	 * Fenced Legion prison camp: two watch towers, barracks tent and a cage with the prisoners
	 */
	protected static void prisonCamp(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int r = 12;
		int y = b.averageGround(cx - r, cz - r, cx + r, cz + r);
		level(b, cx - r, cz - r, cx + r, cz + r, y, RuinBuilder.GRAVEL, 9);
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				if (rnd.nextInt(3) == 0) {
					b.set(x, y, z, RuinBuilder.COARSE_DIRT);
				}
				boolean edge = x == cx - r || x == cx + r || z == cz - r || z == cz + r;
				//gate in the south fence
				boolean gate = z == cz + r && Math.abs(x - cx) <= 1;
				if (edge && !gate) {
					for (int h = 1; h <= 3; h++) {
						b.set(x, y + h, z, RuinBuilder.IRON_BARS);
					}
					if ((x + z) % 6 == 0) {
						b.set(x, y + 4, z, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK));
					}
				}
			}
		}
		BiomeColorType color = colorTypeFor(world, point.pos);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx - r + 1, y, cz - r + 1, 3, 8, 3, 2, color, rnd);
		new WatchTowerSmall(3, 8, 3, 3, 8, 3, 3).setBlocks(world, cx + r - 3, y, cz + r - 3, 3, 8, 3, 0, color, rnd);
		new Tent(7, 4, 5, 7, 4, 5, 1).setBlocks(world, cx + 2, y, cz - r + 2, 7, 4, 5, 3, color, rnd);
		b.spawner(cx + 5, y, cz - r + 4, EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1, ArmySoldier.class);

		//cage: iron bars, a metal roof and a gate
		int gx = cx - 7;
		int gz = cz + 5;
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				boolean edge = Math.abs(i) == 2 || Math.abs(j) == 2;
				b.set(gx + i, y, gz + j, RuinBuilder.COBBLESTONE);
				for (int h = 1; h <= 2; h++) {
					b.set(gx + i, y + h, gz + j, edge ? RuinBuilder.IRON_BARS : AIR);
				}
				b.set(gx + i, y + 3, gz + j, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_SCAFFOLD));
			}
		}
		b.set(gx, y + 1, gz - 2, Blocks.OAK_FENCE_GATE.getDefaultState().withProperty(BlockFenceGate.FACING, EnumFacing.NORTH));
		b.set(gx, y + 2, gz - 2, AIR);
		b.ceilingLamp(gx, y + 3, gz);

		//supplies and floodlights
		b.crateStack(cx + 8, y + 1, cz + 2, 3);
		b.crateStack(cx + 8, y + 1, cz + 3, 2);
		b.lootChest(cx + 8, y + 1, cz + 5, EnumFacing.WEST, CAMP_LOOT);
		b.floorLamp(cx, y + 1, cz);
		b.floorLamp(cx - r + 1, y + 1, cz + r - 1);
		b.floorLamp(cx + r - 1, y + 1, cz - r + 1);

		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(gx, y + 1, gz);
		point.setArea(cx - r, cz - r, cx + r, cz + r);
	}

	/**
	 * Landing zone of the Resistance: a concrete pad with a yellow H and lamps
	 */
	protected static void evac(World world, CampaignPoint point, Random rnd) {
		RuinBuilder b = new RuinBuilder(world, rnd);
		int cx = point.pos.getX();
		int cz = point.pos.getZ();
		int y = b.averageGround(cx - 4, cz - 4, cx + 4, cz + 4);
		level(b, cx - 4, cz - 4, cx + 4, cz + 4, y, StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK), 6);
		IBlockState yellow = RuinBuilder.concreteColor(EnumDyeColor.YELLOW);
		for (int j = -2; j <= 2; j++) {
			b.set(cx - 2, y, cz + j, yellow);
			b.set(cx + 2, y, cz + j, yellow);
		}
		b.set(cx - 1, y, cz, yellow);
		b.set(cx, y, cz, yellow);
		b.set(cx + 1, y, cz, yellow);
		b.floorLamp(cx - 4, y + 1, cz - 4);
		b.floorLamp(cx + 4, y + 1, cz - 4);
		b.floorLamp(cx - 4, y + 1, cz + 4);
		b.floorLamp(cx + 4, y + 1, cz + 4);
		b.set(cx + 4, y + 1, cz, Blocks.STANDING_BANNER.getDefaultState());
		point.pos = new BlockPos(cx, y, cz);
		point.poi = new BlockPos(cx, y + 1, cz);
		point.setArea(cx - 4, cz - 4, cx + 4, cz + 4);
	}

	/*
	 * ------------------------------------------------- mission content
	 */

	/**
	 * Spawns the enemies, captives, boss and targets the mission needs at a placed point
	 */
	public static void arm(World world, EntityPlayerMP player, CampaignMission mission, CampaignPoint point, int index, Random rnd) {
		BlockPos c = point.pos;
		//guards of the place
		switch (point.site) {
		case STASH_HOUSE:
			group(world, CampaignTarget.ZOMBIE, c, 2, 3, 7);
			break;
		case RADIO_MAST:
			group(world, CampaignTarget.BANDIT, c, 2, 3, 8);
			group(world, CampaignTarget.ZOMBIE, c, 1, 3, 8);
			break;
		case HOSPITAL:
			for (int i = 0; i < 6; i++) {
				BlockPos p = new BlockPos(c.getX() - 6 + rnd.nextInt(13), c.getY() + 1, c.getZ() - 3 + rnd.nextInt(7));
				if (world.isAirBlock(p) && world.isAirBlock(p.up())) {
					TGCampaign.spawnMob(world, i < 4 ? CampaignTarget.ZOMBIE_SOLDIER : CampaignTarget.ZOMBIE, p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, null, true);
				}
			}
			break;
		case PRISON_CAMP:
			group(world, CampaignTarget.LEGION, c, 4, 3, 10);
			group(world, CampaignTarget.LEGION_ELITE, c, 2, 3, 10);
			break;
		default:
			break;
		}

		//what the mission is about
		switch (mission.type) {
		case KILL:
			if (mission.site != CampaignSite.NONE && mission.target != null) {
				group(world, mission.target, c, mission.count, 2, 8);
				if (point.site == CampaignSite.CONVOY) {
					group(world, CampaignTarget.LEGION, c, 2, 2, 8);
				}
			}
			break;
		case BOSS:
			if (index == 0 && mission.target != null) {
				spawnBoss(world, mission.target, point.poi != null ? point.poi : c.up());
			}
			break;
		case ESCORT:
			if (point.site != CampaignSite.EVAC) {
				spawnCaptives(world, point, mission.count);
			}
			break;
		case DESTROY:
			buildTargets(world, point, rnd);
			if (mission.target2 != null && !point.targets.isEmpty()) {
				group(world, mission.target2, point.targets.get(0), mission.count2, 3, 12);
			}
			break;
		default:
			break;
		}
		point.armed = true;
	}

	protected static void group(World world, CampaignTarget target, BlockPos center, int count, int minDist, int maxDist) {
		for (int i = 0; i < count; i++) {
			BlockPos p = TGCampaign.randomSurface(world, center, minDist, maxDist);
			TGCampaign.spawnMob(world, target, p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, null, true);
		}
	}

	public static void spawnBoss(World world, CampaignTarget target, BlockPos pos) {
		EntityLiving boss = TGCampaign.spawnMob(world, target, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, null, true);
		if (boss instanceof EntityCreature) {
			((EntityCreature) boss).setHomePosAndDistance(pos, 16);
		}
	}

	/**
	 * captives in their cell, only as many as are missing
	 */
	protected static void spawnCaptives(World world, CampaignPoint point, int count) {
		BlockPos pos = point.poi != null ? point.poi : point.pos.up();
		List<CapturedScientist> present = world.getEntitiesWithinAABB(CapturedScientist.class, new net.minecraft.util.math.AxisAlignedBB(pos).grow(12.0D, 8.0D, 12.0D));
		int missing = count;
		for (CapturedScientist c : present) {
			if (!c.isDelivered() && c.getRescuerPlayer() == null) {
				missing--;
			}
		}
		for (int i = 0; i < missing; i++) {
			CapturedScientist captive = point.site == CampaignSite.PRISON_CAMP ? new LegionPrisoner(world) : new CapturedScientist(world);
			double ox = count > 1 ? (i - 1) * 1.2D : 0.0D;
			captive.setLocationAndAngles(pos.getX() + 0.5D + ox, pos.getY(), pos.getZ() + 0.5D, world.rand.nextFloat() * 360.0f, 0.0f);
			captive.onInitialSpawn(world.getDifficultyForLocation(pos), null);
			captive.setHomePosAndDistance(pos, 4);
			world.spawnEntity(captive);
		}
	}

	/**
	 * Targets of sabotage missions: a fuel depot next to a Legion base, a launch point next to the airfield
	 */
	protected static void buildTargets(World world, CampaignPoint point, Random rnd) {
		point.targets.clear();
		RuinBuilder b = new RuinBuilder(world, rnd);
		IBlockState fuel = TGBlocks.CAMPAIGN_TARGET.getDefaultState().withProperty(TGBlocks.CAMPAIGN_TARGET.TYPE, EnumCampaignTargetType.FUEL_TANK);
		IBlockState console = TGBlocks.CAMPAIGN_TARGET.getDefaultState().withProperty(TGBlocks.CAMPAIGN_TARGET.TYPE, EnumCampaignTargetType.FLIGHT_CONTROL);
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		//east of the structure
		int x0 = point.maxX + 3;
		int z0 = (point.minZ + point.maxZ) / 2 - 6;
		int x1 = x0 + 11;
		int z1 = z0 + 11;
		int y = b.averageGround(x0, z0, x1, z1);
		level(b, x0, z0, x1, z1, y, StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK), 6);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				if (edge && (x + z) % 5 != 0) {
					b.set(x, y + 1, z, sandbags);
				}
			}
		}
		if (point.site == CampaignSite.AIRFIELD) {
			//launch point: flight control under a camo net and two fuel tanks
			int cx = x0 + 5;
			int cz = z0 + 5;
			BlockPos control = new BlockPos(cx, y + 1, cz);
			b.set(cx, y + 1, cz, console);
			point.targets.add(control);
			for (int i = -2; i <= 2; i++) {
				for (int j = -2; j <= 2; j++) {
					b.set(cx + i, y + 4, cz + j, TGBlocks.CAMONET_TOP.getDefaultState());
				}
			}
			b.set(cx - 2, y + 1, cz - 2, Blocks.OAK_FENCE.getDefaultState());
			b.set(cx - 2, y + 2, cz - 2, Blocks.OAK_FENCE.getDefaultState());
			b.set(cx - 2, y + 3, cz - 2, Blocks.OAK_FENCE.getDefaultState());
			b.set(cx + 2, y + 1, cz + 2, Blocks.OAK_FENCE.getDefaultState());
			b.set(cx + 2, y + 2, cz + 2, Blocks.OAK_FENCE.getDefaultState());
			b.set(cx + 2, y + 3, cz + 2, Blocks.OAK_FENCE.getDefaultState());
			b.floorLamp(cx + 1, y + 1, cz - 1);
			BlockPos[] tanks = { new BlockPos(x0 + 2, y + 1, z1 - 2), new BlockPos(x1 - 2, y + 1, z1 - 2) };
			for (BlockPos t : tanks) {
				b.set(t.getX(), t.getY(), t.getZ(), fuel);
				b.set(t.getX(), t.getY() + 1, t.getZ(), RuinBuilder.IRON_BARS);
				point.targets.add(t);
			}
			//radar mast
			for (int k = 1; k <= 6; k++) {
				b.set(x1 - 1, y + k, z0 + 1, RuinBuilder.IRON_BARS);
			}
			b.floorLamp(x1 - 1, y + 7, z0 + 1);
		} else {
			//fuel depot: four tanks with pipes, barrels in between
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					BlockPos t = new BlockPos(x0 + 3 + i * 5, y + 1, z0 + 3 + j * 5);
					b.set(t.getX(), t.getY(), t.getZ(), fuel);
					b.set(t.getX(), t.getY() + 1, t.getZ(), RuinBuilder.IRON_BARS);
					b.set(t.getX() + 1, t.getY(), t.getZ(), Blocks.CAULDRON.getDefaultState());
					point.targets.add(t);
				}
			}
			b.crateStack(x1 - 1, y + 1, z1 - 1, 2);
			b.floorLamp(x0 + 1, y + 1, z0 + 1);
			group(world, CampaignTarget.LEGION, new BlockPos(x0 + 5, y + 1, z0 + 5), 2, 2, 6);
			group(world, CampaignTarget.HEAVY, new BlockPos(x0 + 5, y + 1, z0 + 5), 1, 2, 6);
		}
	}

	/*
	 * ------------------------------------------------- test command
	 */

	public static final String[] TEST_SITES = { "stash_house", "bandit_camp", "radio_mast", "hospital", "convoy", "prison_camp", "evac", "fuel_depot", "launch_point" };

	/**
	 * /tgstructure: builds a campaign place around pos, returns false for unknown names
	 */
	public static boolean placeForTest(String name, World world, BlockPos pos, Random rnd) {
		CampaignPoint point;
		switch (name) {
		case "stash_house":
			point = new CampaignPoint(pos, CampaignSite.STASH_HOUSE);
			break;
		case "bandit_camp":
			point = new CampaignPoint(pos, CampaignSite.BANDIT_CAMP);
			break;
		case "radio_mast":
			point = new CampaignPoint(pos, CampaignSite.RADIO_MAST);
			break;
		case "hospital":
			point = new CampaignPoint(pos, CampaignSite.HOSPITAL);
			break;
		case "convoy":
			point = new CampaignPoint(pos, CampaignSite.CONVOY);
			break;
		case "prison_camp":
			point = new CampaignPoint(pos, CampaignSite.PRISON_CAMP);
			break;
		case "evac":
			point = new CampaignPoint(pos, CampaignSite.EVAC);
			break;
		case "fuel_depot":
		case "launch_point":
			point = new CampaignPoint(pos, "fuel_depot".equals(name) ? CampaignSite.MILITARY_BASE : CampaignSite.AIRFIELD);
			point.setArea(pos.getX() - 8, pos.getZ(), pos.getX() - 3, pos.getZ());
			buildTargets(world, point, rnd);
			return true;
		default:
			return false;
		}
		switch (point.site) {
		case STASH_HOUSE:
			stashHouse(world, point, rnd);
			break;
		case BANDIT_CAMP:
			banditCamp(world, point, rnd);
			break;
		case RADIO_MAST:
			radioMast(world, point, rnd);
			break;
		case HOSPITAL:
			hospital(world, point, rnd);
			break;
		case CONVOY:
			convoy(world, point, rnd);
			break;
		case PRISON_CAMP:
			prisonCamp(world, point, rnd);
			break;
		default:
			evac(world, point, rnd);
			break;
		}
		return true;
	}
}
