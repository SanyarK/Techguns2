package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import techguns.TGBlocks;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.TGMetalPanelType;
import techguns.campaign.TGCampaign;

/**
 * Small allied command post of the story campaign. A fortified hut with the
 * commander NPC inside, sandbag perimeter and a flag pole.
 */
public class CommandPost extends WorldgenStructure {

	protected static final int SIZE = 13;

	public CommandPost() {
		super(SIZE, 8, SIZE, SIZE, 8, SIZE);
		this.setXZSize(SIZE, SIZE);
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);

		int ground = posY;
		int cx = posX + SIZE / 2;
		int cz = posZ + SIZE / 2;

		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN);
		IBlockState floor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		IBlockState radio = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN);
		IBlockState table = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);

		b.prepareGround(posX + 1, posZ + 1, posX + SIZE - 2, posZ + SIZE - 2, ground + 1, 6, Blocks.DIRT.getDefaultState());
		b.fill(posX + 1, ground, posZ + 1, posX + SIZE - 2, ground, posZ + SIZE - 2, floor);

		//sandbag perimeter with a gap on the south side
		for (int i = 1; i <= SIZE - 2; i++) {
			b.set(posX + i, ground + 1, posZ + 1, sandbags);
			if (Math.abs(posX + i - cx) > 1) {
				b.set(posX + i, ground + 1, posZ + SIZE - 2, sandbags);
			}
			b.set(posX + 1, ground + 1, posZ + i, sandbags);
			b.set(posX + SIZE - 2, ground + 1, posZ + i, sandbags);
		}

		//command hut with an open doorway to the south
		b.room(cx - 2, ground + 1, cz - 3, cx + 2, ground + 3, cz + 1, wall, floor, wall);
		b.clear(cx, ground + 1, cz + 2, cx, ground + 2, cz + 2);

		//radio station and map table inside
		for (int x = cx - 2; x <= cx + 2; x++) {
			b.set(x, ground + 1, cz - 3, radio);
		}
		b.set(cx - 2, ground + 1, cz - 1, table);
		b.set(cx + 2, ground + 1, cz - 1, table);
		b.ceilingLamp(cx, ground + 3, cz - 1);

		//flag pole
		int fx = posX + 2;
		int fz = posZ + 2;
		for (int i = 1; i <= 5; i++) {
			b.set(fx, ground + i, fz, Blocks.OAK_FENCE.getDefaultState());
		}
		b.set(fx + 1, ground + 5, fz, Blocks.WOOL.getStateFromMeta(13));

		//supplies
		b.crateStack(posX + SIZE - 3, ground + 1, posZ + 2, 2);
		b.crateStack(posX + SIZE - 3, ground + 1, posZ + 3, 1);
		b.floorLamp(posX + 2, ground + 1, posZ + SIZE - 3);
		b.floorLamp(posX + SIZE - 3, ground + 1, posZ + SIZE - 3);

		//the commander himself
		if (!world.isRemote) {
			TGCampaign.spawnCommander(world, cx, ground + 1, cz);
		}
	}
}
