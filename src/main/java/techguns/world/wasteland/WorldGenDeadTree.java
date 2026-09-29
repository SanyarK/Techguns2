package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

/**
 * Leafless dead tree: a trunk with a few bare branches, charred trees use dark oak logs
 */
public class WorldGenDeadTree extends WorldGenAbstractTree {

	protected final boolean charred;
	protected final boolean tall;

	public WorldGenDeadTree(boolean notify, boolean charred, boolean tall) {
		super(notify);
		this.charred = charred;
		this.tall = tall;
	}

	protected IBlockState pickLog(Random rand) {
		if (this.charred) {
			return Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK);
		}
		switch (rand.nextInt(5)) {
		case 0:
			return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH);
		case 1:
			return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE);
		case 2:
			return Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.ACACIA);
		case 3:
			return Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK);
		default:
			return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.OAK);
		}
	}

	protected static boolean canReplace(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		Material m = state.getMaterial();
		return m == Material.AIR || m == Material.PLANTS || m == Material.VINE;
	}

	@Override
	public boolean generate(World world, Random rand, BlockPos pos) {
		Material soil = world.getBlockState(pos.down()).getMaterial();
		if (soil != Material.GROUND && soil != Material.GRASS && soil != Material.SAND && soil != Material.CLAY) {
			return false;
		}
		if (!canReplace(world, pos)) {
			return false;
		}
		IBlockState log = this.pickLog(rand);
		int height = this.tall ? 6 + rand.nextInt(5) : 3 + rand.nextInt(4);
		for (int i = 0; i < height; i++) {
			BlockPos p = pos.up(i);
			if (!canReplace(world, p)) {
				height = i;
				break;
			}
			this.setBlockAndNotifyAdequately(world, p, log.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y));
		}
		if (height < 3) {
			return height > 0;
		}

		int branches = 1 + rand.nextInt(this.tall ? 4 : 3);
		for (int b = 0; b < branches; b++) {
			EnumFacing dir = EnumFacing.getHorizontal(rand.nextInt(4));
			IBlockState branch = log.withProperty(BlockLog.LOG_AXIS, dir.getAxis() == EnumFacing.Axis.X ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.Z);
			BlockPos p = pos.up(height / 2 + rand.nextInt(height - height / 2));
			int length = 1 + rand.nextInt(3);
			for (int l = 1; l <= length; l++) {
				p = p.offset(dir);
				if (l > 1 && rand.nextBoolean()) {
					p = p.up();
				}
				if (!world.isAirBlock(p)) {
					break;
				}
				this.setBlockAndNotifyAdequately(world, p, branch);
			}
		}
		return true;
	}
}
