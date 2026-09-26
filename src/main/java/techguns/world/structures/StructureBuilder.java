package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import techguns.TGBlocks;
import techguns.blocks.BlockTGLadder;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.TGMetalPanelType;
import techguns.items.ItemTGDoor2x1;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.world.BlockRotator;

/**
 * Helper for procedurally generated structures, all coordinates are world coordinates
 */
public class StructureBuilder {

	public final World world;
	public final Random rnd;
	protected final MutableBlockPos p = new MutableBlockPos();

	public StructureBuilder(World world, Random rnd) {
		this.world = world;
		this.rnd = rnd;
	}

	public static IBlockState concrete(EnumConcreteType type) {
		return TGBlocks.CONCRETE.getDefaultState().withProperty(TGBlocks.CONCRETE.TYPE, type);
	}

	public static IBlockState metalPanel(TGMetalPanelType type) {
		return TGBlocks.METAL_PANEL.getDefaultState().withProperty(TGBlocks.METAL_PANEL.TYPE, type);
	}

	public static boolean isValidY(int y) {
		return y >= 1 && y < 255;
	}

	public void set(int x, int y, int z, IBlockState state) {
		if (isValidY(y)) {
			this.world.setBlockState(this.p.setPos(x, y, z), state, 2);
		}
	}

	public IBlockState get(int x, int y, int z) {
		return this.world.getBlockState(this.p.setPos(x, y, z));
	}

	public boolean isAir(int x, int y, int z) {
		return this.world.isAirBlock(this.p.setPos(x, y, z));
	}

	/**
	 * set only when the current block is not air, used to close tunnel walls without filling other rooms
	 */
	public void setIfNotAir(int x, int y, int z, IBlockState state) {
		if (isValidY(y) && !this.world.isAirBlock(this.p.setPos(x, y, z))) {
			this.world.setBlockState(this.p, state, 2);
		}
	}

	public void fill(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
					this.set(x, y, z, state);
				}
			}
		}
	}

	public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
		this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.getDefaultState());
	}

	/**
	 * Carves a closed room, inner space is x1..x2 / y1..y2 / z1..z2, shell is placed around it
	 */
	public void room(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState wall, IBlockState floor, IBlockState ceiling) {
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				this.set(x, y1 - 1, z, floor);
				this.set(x, y2 + 1, z, ceiling);
				for (int y = y1; y <= y2; y++) {
					if (x < x1 || x > x2 || z < z1 || z > z2) {
						this.set(x, y, z, wall);
					} else {
						this.set(x, y, z, Blocks.AIR.getDefaultState());
					}
				}
			}
		}
	}

	/**
	 * Carves a tunnel, only replaces the shell where there are blocks, so it can connect to rooms that were carved before
	 */
	public void tunnel(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState wall, IBlockState floor, IBlockState ceiling) {
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				this.setIfNotAir(x, y1 - 1, z, floor);
				this.setIfNotAir(x, y2 + 1, z, ceiling);
				for (int y = y1; y <= y2; y++) {
					if (x < x1 || x > x2 || z < z1 || z > z2) {
						this.setIfNotAir(x, y, z, wall);
					}
				}
			}
		}
		this.clear(x1, y1, z1, x2, y2, z2);
	}

	/**
	 * Fills the ground below a platform so it doesn't float, and clears everything above it
	 */
	public void prepareGround(int x1, int z1, int x2, int z2, int y, int clearHeight, IBlockState foundation) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				for (int dy = 1; dy <= 8; dy++) {
					IBlockState below = this.get(x, y - dy, z);
					if (below.getMaterial().blocksMovement() && below.getMaterial() != Material.LEAVES && below.getMaterial() != Material.WOOD) {
						break;
					}
					this.set(x, y - dy, z, foundation);
				}
				for (int dy = 0; dy < clearHeight; dy++) {
					this.set(x, y + dy, z, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	public void ceilingLamp(int x, int y, int z) {
		this.set(x, y, z, BlockRotator.getWithFacing(TGBlocks.LAMP_0.getDefaultState(), EnumFacing.UP));
	}

	public void floorLamp(int x, int y, int z) {
		this.set(x, y, z, BlockRotator.getWithFacing(TGBlocks.LAMP_0.getDefaultState(), EnumFacing.DOWN));
	}

	/**
	 * ladder on the block x/z that is attached to the wall in direction 'wallSide'
	 */
	public void ladder(int x, int y1, int y2, int z, EnumFacing wallSide) {
		IBlockState ladder = TGBlocks.LADDER_0.getDefaultState().withProperty(BlockTGLadder.FACING, wallSide.getOpposite());
		for (int y = y1; y <= y2; y++) {
			this.set(x, y, z, ladder);
		}
	}

	public void stairs(int x, int y, int z, IBlockState stairs, EnumFacing facing) {
		this.set(x, y, z, stairs.withProperty(BlockHorizontal.FACING, facing));
	}

	public void crate(int x, int y, int z) {
		this.set(x, y, z, TGBlocks.MILITARY_CRATE.getStateFromMeta(this.rnd.nextInt(9)));
	}

	public void crateStack(int x, int y, int z, int maxHeight) {
		int h = 1 + this.rnd.nextInt(maxHeight);
		for (int i = 0; i < h; i++) {
			this.crate(x, y + i, z);
		}
	}

	public void lootChest(int x, int y, int z, EnumFacing facing, ResourceLocation loottable) {
		if (!isValidY(y)) return;
		this.world.setBlockState(this.p.setPos(x, y, z), Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, facing), 2);
		TileEntity tile = this.world.getTileEntity(this.p);
		if (tile instanceof TileEntityChest) {
			((TileEntityChest) tile).setLootTable(loottable, this.rnd.nextLong());
		}
	}

	public void door(int x, int y, int z, EnumFacing facing) {
		ItemTGDoor2x1.placeDoor(this.world, this.p.setPos(x, y, z).toImmutable(), facing, TGBlocks.BUNKER_DOOR, true);
	}

	/**
	 * Places a Techguns monster spawner, mobs spawn one block above it
	 */
	public TGSpawnerTileEnt spawner(int x, int y, int z, EnumMonsterSpawnerType type, int mobsLeft, int maxActive, int delay, int range) {
		if (!isValidY(y)) return null;
		this.world.setBlockState(this.p.setPos(x, y, z), TGBlocks.MONSTER_SPAWNER.getDefaultState().withProperty(TGBlocks.MONSTER_SPAWNER.TYPE, type), 3);
		TileEntity tile = this.world.getTileEntity(this.p);
		if (tile instanceof TGSpawnerTileEnt) {
			TGSpawnerTileEnt spawner = (TGSpawnerTileEnt) tile;
			spawner.setParams(mobsLeft, maxActive, delay, range);
			return spawner;
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void addMob(TGSpawnerTileEnt spawner, Class clazz, int weight) {
		if (spawner != null) {
			spawner.addMobType(clazz, weight);
		}
	}

	/**
	 * Spawner with a single mob type
	 */
	@SuppressWarnings("rawtypes")
	public TGSpawnerTileEnt spawner(int x, int y, int z, EnumMonsterSpawnerType type, int mobsLeft, int maxActive, int delay, int range, Class clazz) {
		TGSpawnerTileEnt spawner = this.spawner(x, y, z, type, mobsLeft, maxActive, delay, range);
		addMob(spawner, clazz, 1);
		return spawner;
	}
}
