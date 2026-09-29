package techguns.blocks;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.Techguns;

/**
 * Sabotage targets of the story campaign. Fuel tanks blow up a few seconds after they were
 * broken (or at once when hit by an explosion), the flight console bursts into sparks.
 * They never drop anything, the campaign checks if they are still there.
 */
public class BlockCampaignTarget extends GenericBlockMetaEnum<EnumCampaignTargetType> {

	public BlockCampaignTarget(String name) {
		super(name, Material.IRON, MapColor.RED, SoundType.METAL, EnumCampaignTargetType.class);
		this.setHardness(1.5f);
		this.setResistance(2.0f);
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Items.AIR;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public int damageDropped(IBlockState state) {
		return 0;
	}

	@Override
	public boolean canDropFromExplosion(Explosion explosionIn) {
		return false;
	}

	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		super.onBlockHarvested(worldIn, pos, state, player);
		if (worldIn.isRemote) {
			return;
		}
		if (state.getValue(this.TYPE) == EnumCampaignTargetType.FUEL_TANK) {
			EntityTNTPrimed tnt = new EntityTNTPrimed(worldIn, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player);
			tnt.setFuse(60);
			worldIn.spawnEntity(tnt);
			worldIn.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 0.8f);
			player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.target.fuel_tank.primed").setStyle(new Style().setColor(TextFormatting.RED)), true);
		} else {
			this.sparks(worldIn, pos);
		}
	}

	@Override
	public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
		IBlockState state = world.getBlockState(pos);
		boolean fuel = state.getBlock() == this && state.getValue(this.TYPE) == EnumCampaignTargetType.FUEL_TANK;
		world.setBlockToAir(pos);
		if (world.isRemote) {
			return;
		}
		if (fuel) {
			//chain reaction like tnt
			EntityTNTPrimed tnt = new EntityTNTPrimed(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, explosion.getExplosivePlacedBy());
			tnt.setFuse(10 + world.rand.nextInt(10));
			world.spawnEntity(tnt);
		} else {
			this.sparks(world, pos);
		}
	}

	protected void sparks(World world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8f, 1.6f);
		if (world instanceof WorldServer) {
			((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 2, 0.3D, 0.3D, 0.3D, 0.0D);
			((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 12, 0.3D, 0.2D, 0.3D, 0.0D);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		tooltip.add(TextFormatting.GRAY + I18n.format(Techguns.MODID + ".campaign.target.tooltip"));
	}
}
