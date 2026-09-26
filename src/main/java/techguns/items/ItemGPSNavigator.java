package techguns.items;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.Techguns;
import techguns.capabilities.TGCampaignData;

/**
 * Shows direction and distance to the current campaign objective in the action bar while held.
 */
public class ItemGPSNavigator extends GenericItem {

	protected static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	public ItemGPSNavigator(String name) {
		super(name);
		this.setMaxStackSize(1);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!world.isRemote || !isSelected || !(entity instanceof EntityPlayer)) {
			return;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (player != Techguns.proxy.getPlayerClient() || player.ticksExisted % 10 != 0) {
			return;
		}
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		if (!data.hasObjective()) {
			player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.gps.noobjective"), true);
			return;
		}

		BlockPos target = data.getObjective();
		double dx = target.getX() + 0.5D - player.posX;
		double dz = target.getZ() + 0.5D - player.posZ;
		int dist = (int) Math.sqrt(dx * dx + dz * dz);

		if (dist < 16) {
			player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.gps.arrived"), true);
			return;
		}

		//minecraft yaw: 0 = south (+z), grows clockwise
		float yawToTarget = (float) (-MathHelper.atan2(dx, dz) * (180D / Math.PI));
		float relative = MathHelper.wrapDegrees(yawToTarget - MathHelper.wrapDegrees(player.rotationYaw));
		int arrowIdx = Math.floorMod(Math.round(relative / 45.0f), 8);
		int dirIdx = Math.floorMod(Math.round(yawToTarget / 45.0f), 8);

		player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.gps.distance",
				ARROWS[arrowIdx], dist, new TextComponentTranslation(Techguns.MODID + ".campaign.gps.dir." + dirIdx)), true);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		tooltip.add(ChatFormatting.GRAY + I18n.format(Techguns.MODID + ".campaign.item.gps.tooltip"));
	}
}
