package techguns.campaign;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextFormatting;
import techguns.Techguns;
import techguns.items.guns.GenericGun;

/**
 * The unique named rewards of the last mission: the armor and the weapons of the "Wasteland Hero".
 * The stacks carry the hero id in their NBT, the client shows the translated name and description
 * in the tooltip (see CampaignClientEvents), the stack name is set for the hotbar.
 */
public class HeroItems {

	public static final String NBT_HERO = "techguns_hero";
	/** number of description lines of every hero item in the lang files */
	public static final int LORE_LINES = 2;

	public static Supplier<ItemStack> hero(Supplier<Item> item, String id) {
		return () -> {
			Item i = item.get();
			return i == null ? ItemStack.EMPTY : create(new ItemStack(i), id);
		};
	}

	@SuppressWarnings("deprecation")
	public static ItemStack create(ItemStack stack, String id) {
		if (stack.getItem() instanceof GenericGun) {
			//guns fill their ammo tags only while the stack has no tags at all
			stack.getItem().onCreated(stack, null, null);
		}
		stack.setTagInfo(NBT_HERO, new NBTTagString(id));
		stack.setStackDisplayName(TextFormatting.GOLD + net.minecraft.util.text.translation.I18n.translateToLocal(nameKey(id)));
		stack.addEnchantment(Enchantments.UNBREAKING, 3);
		return stack;
	}

	@Nullable
	public static String getHeroId(ItemStack stack) {
		if (stack.isEmpty() || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(NBT_HERO)) {
			return null;
		}
		return stack.getTagCompound().getString(NBT_HERO);
	}

	public static String nameKey(String id) {
		return Techguns.MODID + ".hero." + id + ".name";
	}

	public static String loreKey(String id, int line) {
		return Techguns.MODID + ".hero." + id + ".lore" + line;
	}
}
