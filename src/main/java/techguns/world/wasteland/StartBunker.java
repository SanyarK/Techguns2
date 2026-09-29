package techguns.world.wasteland;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import techguns.TGBlocks;
import techguns.TGItems;
import techguns.TGuns;
import techguns.blocks.BlockRadioStation;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.TGMetalPanelType;
import techguns.world.structures.StructureBuilder;

/**
 * Small shelter the players start in: bed, supplies, crafting table, furnace, a radio to the
 * commander of the campaign and a ladder up to a hatch.
 */
public class StartBunker {

	/**
	 * Builds the bunker under the surface at x/z.
	 * @param prepareChunks generate the chunks around first, so their decoration can't overwrite the bunker later
	 * @return the position inside the bunker where players spawn, null if the ground is too low
	 */
	@Nullable
	public static BlockPos build(World world, int x, int z, Random rnd, boolean prepareChunks) {
		if (prepareChunks) {
			int cx = x >> 4;
			int cz = z >> 4;
			for (int i = cx - 2; i <= cx + 2; i++) {
				for (int j = cz - 2; j <= cz + 2; j++) {
					world.getChunkFromChunkCoords(i, j);
				}
			}
		}

		RuinBuilder b = new RuinBuilder(world, rnd);
		int sy = b.ground(x, z);
		int fy = sy - 9;
		if (fy < 4) {
			return null;
		}

		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState ceiling = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState floor = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);

		//room, inner space x-3..x+3, z-2..z+2, 3 blocks high
		b.room(x - 3, fy + 1, z - 2, x + 3, fy + 3, z + 2, wall, floor, ceiling);

		//bed along the west wall
		IBlockState bed = Blocks.BED.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
		b.set(x - 3, fy + 1, z + 1, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT));
		b.set(x - 3, fy + 1, z + 2, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD));

		//supplies, workbench, furnace and the radio on the north wall
		b.set(x - 3, fy + 1, z - 2, Blocks.CHEST.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.EAST));
		fillStarterChest(world, new BlockPos(x - 3, fy + 1, z - 2));
		b.set(x - 1, fy + 1, z - 2, Blocks.CRAFTING_TABLE.getDefaultState());
		b.set(x + 1, fy + 1, z - 2, Blocks.FURNACE.getDefaultState().withProperty(BlockFurnace.FACING, EnumFacing.SOUTH));
		b.set(x, fy + 1, z - 2, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN));
		if (TGBlocks.RADIO_STATION != null) {
			b.set(x, fy + 2, z - 2, TGBlocks.RADIO_STATION.getDefaultState().withProperty(BlockRadioStation.FACING, EnumFacing.SOUTH));
		}

		//a bit of comfort
		IBlockState rug = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY);
		b.set(x - 2, fy + 1, z + 1, rug);
		b.set(x - 2, fy + 1, z + 2, rug);
		b.set(x - 1, fy + 1, z + 2, rug);
		b.crate(x + 2, fy + 1, z + 2);
		b.crate(x + 3, fy + 1, z + 2);
		b.crate(x + 3, fy + 2, z + 2);
		b.ceilingLamp(x - 1, fy + 3, z);
		b.ceilingLamp(x + 2, fy + 3, z - 1);

		//shaft with a ladder up to the hatch
		int sx = x + 3;
		for (int y = fy + 4; y <= sy; y++) {
			b.set(sx, y, z, RuinBuilder.AIR);
			if (y > fy + 4 && y < sy) {
				b.set(sx - 1, y, z, wall);
				b.set(sx + 1, y, z, wall);
				b.set(sx, y, z - 1, wall);
				b.set(sx, y, z + 1, wall);
			}
		}
		b.ladder(sx, fy + 1, sy - 1, z, EnumFacing.EAST);
		b.set(sx, sy, z, Blocks.TRAPDOOR.getDefaultState().withProperty(BlockTrapDoor.FACING, EnumFacing.WEST)
				.withProperty(BlockTrapDoor.HALF, BlockTrapDoor.DoorHalf.TOP).withProperty(BlockTrapDoor.OPEN, false));

		//concrete collar around the hatch and a lantern post, so the entrance can be found again
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i != 0 || j != 0) {
					b.set(sx + i, sy, z + j, ceiling);
				}
				b.clearAbove(sx + i, sy + 1, z + j, 3);
			}
		}
		b.foundation(sx + 2, z + 2, sy, RuinBuilder.COBBLESTONE);
		b.set(sx + 2, sy + 1, z + 2, Blocks.COBBLESTONE_WALL.getDefaultState());
		b.set(sx + 2, sy + 2, z + 2, Blocks.COBBLESTONE_WALL.getDefaultState());
		b.floorLamp(sx + 2, sy + 3, z + 2);

		return new BlockPos(x, fy + 1, z);
	}

	protected static void fillStarterChest(World world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileEntityChest)) {
			return;
		}
		TileEntityChest chest = (TileEntityChest) tile;
		int slot = 0;
		chest.setInventorySlotContents(slot++, new ItemStack(Items.BREAD, 8));
		chest.setInventorySlotContents(slot++, new ItemStack(Items.COOKED_BEEF, 6));
		chest.setInventorySlotContents(slot++, new ItemStack(Items.APPLE, 4));
		if (TGItems.BANDAGE != null) {
			chest.setInventorySlotContents(slot++, new ItemStack(TGItems.BANDAGE, 6));
		}
		if (TGuns.pistol != null) {
			chest.setInventorySlotContents(slot++, new ItemStack(TGuns.pistol));
		}
		chest.setInventorySlotContents(slot++, TGItems.newStack(TGItems.PISTOL_MAGAZINE, 6));
		chest.setInventorySlotContents(slot++, new ItemStack(Blocks.TORCH, 16));
		chest.setInventorySlotContents(slot++, new ItemStack(Items.WATER_BUCKET));
	}
}
