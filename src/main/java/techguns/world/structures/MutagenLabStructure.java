package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import techguns.TGItems;
import techguns.Techguns;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.MutantWarrior;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.entities.npcs.SuperMutantElite;
import techguns.tileentities.TGSpawnerTileEnt;

/**
 * Secret underground mutagen laboratory of the story campaign.
 * Three levels below a small surface shed: laboratory hall with holding cages,
 * a containment block with mutant cells and the sample safe, and the boss hall
 * where the Prototype is fought.
 */
public class MutagenLabStructure extends WorldgenStructure {

	private static final ResourceLocation LAB_LOOT = new ResourceLocation(Techguns.MODID, "chests/mutagen_lab");
	private static final ResourceLocation VAULT_LOOT = new ResourceLocation(Techguns.MODID, "chests/general_vault");

	protected static final int SIZE = 49;

	/** depths of the three levels below ground (walking levels are ground+1-DEPTH) */
	protected static final int DEPTH_LAB = 8;
	protected static final int DEPTH_CELLS = 15;
	protected static final int DEPTH_BOSS = 22;

	/** boss spawn position relative to the structure center at ground height */
	public static final int BOSS_OFFSET_X = 1;
	public static final int BOSS_OFFSET_Y = -21;
	public static final int BOSS_OFFSET_Z = -11;

	/** safe with the mutagen sample on the containment level, relative to the center at ground height */
	public static final int SAFE_OFFSET_X = 7;
	public static final int SAFE_OFFSET_Y = 1 - DEPTH_CELLS;
	public static final int SAFE_OFFSET_Z = -17;

	/** free spot in front of the mutagen tanks of the laboratory hall, where the Purifier filter is stored */
	public static final int FILTER_OFFSET_X = 7;
	public static final int FILTER_OFFSET_Y = 1 - DEPTH_LAB;
	public static final int FILTER_OFFSET_Z = -17;

	public MutagenLabStructure() {
		super(SIZE, 10, SIZE, SIZE, 10, SIZE);
		this.setXZSize(SIZE, SIZE);
		this.lootTier = techguns.world.EnumLootType.TIER2;
	}

	@Override
	protected int getStep() {
		return 8;
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);

		int ground = posY;
		int cx = posX + SIZE / 2;
		int cz = posZ + SIZE / 2;
		int y1 = ground + 1 - DEPTH_LAB; //laboratory level
		int y2 = ground + 1 - DEPTH_CELLS; //containment level
		int y3 = ground + 1 - DEPTH_BOSS; //boss hall level

		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState darkFloor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState pipes = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_PIPES);
		IBlockState labPanel = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_BLUE);
		IBlockState table = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);
		IBlockState vat = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN);
		IBlockState bars = Blocks.IRON_BARS.getDefaultState();
		IBlockState limeGlass = Blocks.STAINED_GLASS.getStateFromMeta(5);

		/*
		 * LEVEL 1: entry room and laboratory hall
		 */
		b.room(cx - 3, y1, cz - 3, cx + 3, y1 + 3, cz + 3, wall, darkFloor, wall);
		b.ceilingLamp(cx, y1 + 3, cz);
		b.crateStack(cx + 2, y1, cz + 2, 2);

		//corridor to the lab hall
		b.tunnel(cx - 1, y1, cz - 9, cx + 1, y1 + 2, cz - 4, wall, darkFloor, wall);
		b.ceilingLamp(cx, y1 + 2, cz - 7);

		//laboratory hall
		b.room(cx - 9, y1, cz - 22, cx + 9, y1 + 3, cz - 10, labPanel, darkFloor, wall);
		b.ceilingLamp(cx - 5, y1 + 3, cz - 16);
		b.ceilingLamp(cx + 5, y1 + 3, cz - 16);
		b.ceilingLamp(cx, y1 + 3, cz - 12);
		b.ceilingLamp(cx, y1 + 3, cz - 20);

		//holding cages with mutants
		this.cage(b, cx - 8, y1, cz - 13, SuperMutantBasic.class, 2);
		this.cage(b, cx - 4, y1, cz - 21, MutantWarrior.class, 2);
		this.cage(b, cx + 4, y1, cz - 21, SuperMutantElite.class, 1);

		//lab benches with equipment
		for (int x = cx - 5; x <= cx - 1; x++) {
			b.set(x, y1, cz - 11, table);
		}
		b.set(cx - 3, y1 + 1, cz - 11, Blocks.BREWING_STAND.getDefaultState());
		b.set(cx + 2, y1, cz - 11, Blocks.CAULDRON.getDefaultState());
		b.set(cx + 9, y1, cz - 11, Blocks.BOOKSHELF.getDefaultState());
		b.set(cx + 9, y1 + 1, cz - 11, Blocks.BOOKSHELF.getDefaultState());
		b.set(cx + 9, y1, cz - 12, Blocks.BOOKSHELF.getDefaultState());

		//mutagen tanks
		for (int i = 0; i < 3; i++) {
			b.set(cx + 8, y1 + i, cz - 18, vat);
			b.set(cx + 8, y1 + i, cz - 16, vat);
			b.set(cx + 8, y1 + i, cz - 17, limeGlass);
		}

		//free mutants in the hall
		TGSpawnerTileEnt hall = b.spawner(cx, y1, cz - 16, EnumMonsterSpawnerType.HOLE, 4, 2, 300, 1);
		StructureBuilder.addMob(hall, SuperMutantBasic.class, 2);
		StructureBuilder.addMob(hall, MutantWarrior.class, 1);

		b.lootChest(cx - 1, y1, cz - 22, EnumFacing.SOUTH, LAB_LOOT);
		b.lootChest(cx + 1, y1, cz - 22, EnumFacing.SOUTH, LAB_LOOT);
		b.crateStack(cx - 9, y1, cz - 10, 2);
		b.crateStack(cx + 9, y1, cz - 22, 2);

		/*
		 * LEVEL 2: containment block with mutant cells and the sample safe
		 */
		b.room(cx - 8, y2, cz - 22, cx + 8, y2 + 2, cz - 16, wall, darkFloor, wall);
		b.ceilingLamp(cx - 4, y2 + 2, cz - 19);
		b.ceilingLamp(cx + 4, y2 + 2, cz - 19);

		//north row of cells behind iron bars
		b.fill(cx - 8, y2, cz - 20, cx + 8, y2 + 2, cz - 20, bars);
		b.fill(cx - 3, y2, cz - 22, cx - 3, y2 + 2, cz - 20, wall);
		b.fill(cx + 3, y2, cz - 22, cx + 3, y2 + 2, cz - 20, wall);
		b.spawner(cx - 6, y2, cz - 21, EnumMonsterSpawnerType.HOLE, 2, 1, 300, 0, SuperMutantBasic.class);
		b.spawner(cx, y2, cz - 22, EnumMonsterSpawnerType.HOLE, 2, 1, 300, 0, MutantWarrior.class);
		b.spawner(cx + 6, y2, cz - 21, EnumMonsterSpawnerType.HOLE, 2, 1, 300, 0, SuperMutantBasic.class);

		//south side: the safe room
		b.fill(cx - 8, y2, cz - 18, cx + 8, y2 + 2, cz - 16, wall);
		b.clear(cx + 3, y2, cz - 18, cx + 7, y2 + 1, cz - 17);
		b.door(cx + 4, y2, cz - 18, EnumFacing.NORTH);
		b.ceilingLamp(cx + 5, y2 + 2, cz - 17);
		this.sampleChest(b, cx + 7, y2, cz - 17, EnumFacing.WEST);
		b.lootChest(cx + 6, y2, cz - 17, EnumFacing.WEST, LAB_LOOT);

		/*
		 * shafts between the levels
		 */
		//surface shed -> entry room
		b.fill(cx - 1, y1 + 4, cz - 3, cx + 1, ground - 1, cz - 1, wall);
		b.clear(cx, y1, cz - 2, cx, ground + 2, cz - 2);
		b.ladder(cx, y1, ground + 1, cz - 2, EnumFacing.NORTH);

		//lab hall -> containment (east shaft)
		b.fill(cx + 11, y2 - 1, cz - 20, cx + 13, y1 + 3, cz - 18, wall);
		b.clear(cx + 12, y2, cz - 19, cx + 12, y1 + 2, cz - 19);
		b.tunnel(cx + 10, y1, cz - 19, cx + 11, y1 + 2, cz - 19, wall, darkFloor, wall);
		b.tunnel(cx + 9, y2, cz - 19, cx + 11, y2 + 2, cz - 19, wall, darkFloor, wall);
		b.ladder(cx + 12, y2, y1 + 1, cz - 19, EnumFacing.EAST);

		//containment -> boss hall (west shaft)
		b.fill(cx - 13, y3 - 1, cz - 20, cx - 11, y2 + 3, cz - 18, wall);
		b.clear(cx - 12, y3, cz - 19, cx - 12, y2 + 2, cz - 19);
		b.tunnel(cx - 11, y2, cz - 19, cx - 9, y2 + 2, cz - 19, wall, darkFloor, wall);
		b.ladder(cx - 12, y3, y2 + 1, cz - 19, EnumFacing.WEST);

		/*
		 * LEVEL 3: boss hall of the Prototype
		 */
		b.room(cx - 8, y3, cz - 20, cx + 10, y3 + 5, cz - 2, pipes, darkFloor, wall);
		//connect the west shaft to the hall
		b.tunnel(cx - 11, y3, cz - 19, cx - 9, y3 + 2, cz - 19, wall, darkFloor, wall);

		//pillars
		for (int px = cx - 4; px <= cx + 6; px += 10) {
			for (int pz = cz - 16; pz <= cz - 6; pz += 10) {
				b.fill(px, y3, pz, px, y3 + 4, pz, wall);
				b.ceilingLamp(px, y3 + 5, pz + 1);
			}
		}
		b.ceilingLamp(cx + 1, y3 + 5, cz - 11);

		//giant mutagen vats along the north wall
		for (int x = cx - 2; x <= cx + 4; x += 3) {
			for (int i = 0; i < 4; i++) {
				b.set(x, y3 + i, cz - 20, vat);
				b.set(x + 1, y3 + i, cz - 20, limeGlass);
			}
		}

		//guards and reward chests
		b.spawner(cx - 4, y3, cz - 18, EnumMonsterSpawnerType.HOLE, 2, 1, 300, 1, MutantWarrior.class);
		b.spawner(cx + 6, y3, cz - 4, EnumMonsterSpawnerType.HOLE, 2, 1, 300, 1, MutantWarrior.class);
		b.lootChest(cx + 10, y3, cz - 3, EnumFacing.WEST, VAULT_LOOT);
		b.lootChest(cx + 10, y3, cz - 4, EnumFacing.WEST, VAULT_LOOT);

		/*
		 * SURFACE: small shed above the entry shaft
		 */
		b.prepareGround(cx - 5, cz - 5, cx + 5, cz + 5, ground + 1, 6, Blocks.DIRT.getDefaultState());
		b.fill(cx - 4, ground, cz - 4, cx + 4, ground, cz + 4, StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK));
		b.room(cx - 2, ground + 1, cz - 3, cx + 2, ground + 3, cz + 1, wall, darkFloor, wall);
		b.clear(cx, ground + 1, cz + 2, cx, ground + 2, cz + 2);
		//hole in the shed floor above the ladder
		b.clear(cx, ground, cz - 2, cx, ground, cz - 2);
		b.ceilingLamp(cx, ground + 3, cz);
		b.set(cx - 2, ground + 4, cz - 1, StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK));
		b.crateStack(cx + 4, ground + 1, cz + 4, 2);
		b.crateStack(cx - 4, ground + 1, cz - 4, 1);
		b.floorLamp(cx - 4, ground + 1, cz + 4);
	}

	protected void cage(StructureBuilder b, int x, int y, int z, Class<?> mobClass, int count) {
		IBlockState bars = Blocks.IRON_BARS.getDefaultState();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					b.fill(x + dx, y, z + dz, x + dx, y + 3, z + dz, bars);
				}
			}
		}
		b.spawner(x, y, z, EnumMonsterSpawnerType.HOLE, count, 1, 300, 0, mobClass);
	}

	/**
	 * The safe chest that holds the mutagen sample quest item
	 */
	protected void sampleChest(StructureBuilder b, int x, int y, int z, EnumFacing facing) {
		if (!StructureBuilder.isValidY(y)) {
			return;
		}
		BlockPos pos = new BlockPos(x, y, z);
		b.world.setBlockState(pos, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, facing), 2);
		TileEntity tile = b.world.getTileEntity(pos);
		if (tile instanceof TileEntityChest) {
			TileEntityChest chest = (TileEntityChest) tile;
			chest.setInventorySlotContents(13, new ItemStack(TGItems.MUTAGEN_SAMPLE));
			chest.setInventorySlotContents(11, new ItemStack(TGItems.RAD_AWAY));
		}
	}
}
