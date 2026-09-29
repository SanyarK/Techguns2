package techguns.blocks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.Techguns;
import techguns.campaign.TGCampaign;

/**
 * Wall mounted radio: right click opens the commander dialog of the story campaign, like the radio item
 */
public class BlockRadioStation extends GenericBlock {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	//the box sits against the wall behind the front
	protected static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.1875D, 0.125D, 0.6875D, 0.8125D, 0.875D, 1.0D);
	protected static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.1875D, 0.125D, 0.0D, 0.8125D, 0.875D, 0.3125D);
	protected static final AxisAlignedBB AABB_WEST = new AxisAlignedBB(0.6875D, 0.125D, 0.1875D, 1.0D, 0.875D, 0.8125D);
	protected static final AxisAlignedBB AABB_EAST = new AxisAlignedBB(0.0D, 0.125D, 0.1875D, 0.3125D, 0.875D, 0.8125D);

	protected GenericItemBlock itemblock;

	public BlockRadioStation(String name) {
		super(name, Material.IRON);
		this.setSoundType(SoundType.METAL);
		this.setHardness(3.0f);
		this.setResistance(10.0f);
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex();
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}

	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		EnumFacing front = facing.getAxis().isHorizontal() ? facing : placer.getHorizontalFacing().getOpposite();
		return this.getDefaultState().withProperty(FACING, front);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		switch (state.getValue(FACING)) {
		case SOUTH:
			return AABB_SOUTH;
		case WEST:
			return AABB_WEST;
		case EAST:
			return AABB_EAST;
		default:
			return AABB_NORTH;
		}
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!worldIn.isRemote && playerIn instanceof EntityPlayerMP) {
			worldIn.playSound(null, pos, SoundEvents.BLOCK_NOTE_HAT, SoundCategory.BLOCKS, 0.6f, 1.6f);
			boolean atCommander = TGCampaign.findCommanderNear(playerIn, 6.0D) != null;
			TGCampaign.openDialog((EntityPlayerMP) playerIn, atCommander);
		}
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		tooltip.add(TextFormatting.GRAY + I18n.format(Techguns.MODID + ".radio_station.tooltip"));
	}

	@Override
	public ItemBlock createItemBlock() {
		this.itemblock = new GenericItemBlock(this);
		return this.itemblock;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerItemBlockModels() {
		ModelLoader.setCustomModelResourceLocation(this.itemblock, 0, new ModelResourceLocation(this.getRegistryName(), "inventory"));
	}
}
