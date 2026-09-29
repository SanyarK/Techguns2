package techguns.items;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Simple campaign quest item (intel documents, mutagen sample) with a tooltip and glow.
 */
public class ItemQuestItem extends GenericItem {

	protected final String tooltipKey;
	protected final boolean glowing;

	public ItemQuestItem(String name, String tooltipKey, boolean glowing) {
		super(name);
		this.setMaxStackSize(4);
		this.tooltipKey = tooltipKey;
		this.glowing = glowing;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack stack) {
		return this.glowing;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		tooltip.add(ChatFormatting.GOLD + I18n.format(this.tooltipKey));
	}
}
