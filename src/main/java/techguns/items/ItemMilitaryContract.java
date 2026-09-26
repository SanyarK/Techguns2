package techguns.items;

import java.util.List;
import java.util.Random;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.Techguns;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.AttackHelicopter;
import techguns.entities.npcs.Bandit;
import techguns.entities.npcs.Commando;
import techguns.entities.npcs.CyberDemon;
import techguns.entities.npcs.DictatorDave;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.General;
import techguns.entities.npcs.GenericNPCUndead;
import techguns.entities.npcs.HeavySoldier;
import techguns.entities.npcs.ITGBoss;
import techguns.entities.npcs.MilitaryJet;
import techguns.entities.npcs.Outcast;
import techguns.entities.npcs.PsychoSteve;
import techguns.entities.npcs.StormTrooper;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.util.MathUtil;

/**
 * Quest item. Right click an unsigned contract to get a random kill assignment,
 * kills are counted while the contract is in the inventory,
 * right click a completed contract to claim loot, experience and combat buffs.
 */
public class ItemMilitaryContract extends GenericItem {

	public static final String NBT_TYPE = "contractType";
	public static final String NBT_TIER = "contractTier";
	public static final String NBT_REQUIRED = "contractRequired";
	public static final String NBT_PROGRESS = "contractProgress";

	public static final int MAX_TIER = 2;

	public static final ResourceLocation[] REWARD_LOOT = {
		new ResourceLocation(Techguns.MODID, "contracts/reward_tier0"),
		new ResourceLocation(Techguns.MODID, "contracts/reward_tier1"),
		new ResourceLocation(Techguns.MODID, "contracts/reward_tier2")
	};

	protected static final int[] REWARD_XP = {40, 100, 250};

	protected static final String[] TIER_NAMES = {"I", "II", "III"};

	public enum ContractType {
		SOLDIERS(8, 16, 30),
		MUTANTS(6, 12, 20),
		UNDEAD(12, 24, 40),
		AIRCRAFT(1, 2, 4),
		BOSS(1, 1, 1),
		HOSTILES(20, 40, 80);

		protected final int[] required;

		ContractType(int... required) {
			this.required = required;
		}

		public int getRequired(int tier) {
			return this.required[MathUtil.clamp(tier, 0, this.required.length - 1)];
		}

		public String getTranslationKey() {
			return Techguns.MODID + ".contract.type." + this.name().toLowerCase();
		}

		public boolean matches(EntityLivingBase victim) {
			switch (this) {
			case SOLDIERS:
				return victim instanceof ArmySoldier || victim instanceof Commando || victim instanceof EliteSoldier || victim instanceof HeavySoldier
						|| victim instanceof StormTrooper || victim instanceof Bandit || victim instanceof DictatorDave || victim instanceof General
						|| victim instanceof Outcast;
			case MUTANTS:
				return victim instanceof SuperMutantBasic;
			case UNDEAD:
				return victim instanceof GenericNPCUndead || victim.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD;
			case AIRCRAFT:
				return victim instanceof AttackHelicopter || victim instanceof MilitaryJet;
			case BOSS:
				return victim instanceof ITGBoss || victim instanceof DictatorDave || victim instanceof PsychoSteve || victim instanceof CyberDemon
						|| victim instanceof EntityWither || victim instanceof EntityDragon;
			case HOSTILES:
				return victim instanceof IMob;
			default:
				return false;
			}
		}

		public static ContractType fromOrdinal(int i) {
			ContractType[] values = ContractType.values();
			return values[MathUtil.clamp(i, 0, values.length - 1)];
		}
	}

	protected static final ContractType[] RANDOM_TYPES = {ContractType.SOLDIERS, ContractType.MUTANTS, ContractType.UNDEAD, ContractType.AIRCRAFT, ContractType.HOSTILES};

	public ItemMilitaryContract(String name) {
		super(name);
		this.setMaxStackSize(1);
	}

	public static boolean isSigned(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_TYPE);
	}

	public static ContractType getType(ItemStack stack) {
		return isSigned(stack) ? ContractType.fromOrdinal(stack.getTagCompound().getByte(NBT_TYPE)) : ContractType.HOSTILES;
	}

	public static int getTier(ItemStack stack) {
		return isSigned(stack) ? MathUtil.clamp(stack.getTagCompound().getByte(NBT_TIER), 0, MAX_TIER) : 0;
	}

	public static int getRequired(ItemStack stack) {
		return isSigned(stack) ? Math.max(1, stack.getTagCompound().getShort(NBT_REQUIRED)) : 1;
	}

	public static int getProgress(ItemStack stack) {
		return isSigned(stack) ? stack.getTagCompound().getShort(NBT_PROGRESS) : 0;
	}

	public static boolean isComplete(ItemStack stack) {
		return isSigned(stack) && getProgress(stack) >= getRequired(stack);
	}

	/**
	 * Assign a random contract to this stack
	 */
	public static void signContract(ItemStack stack, Random rnd) {
		int roll = rnd.nextInt(10);
		int tier = roll < 6 ? 0 : (roll < 9 ? 1 : 2);

		ContractType type;
		if (tier == MAX_TIER && rnd.nextInt(3) == 0) {
			type = ContractType.BOSS;
		} else {
			type = RANDOM_TYPES[rnd.nextInt(RANDOM_TYPES.length)];
		}

		NBTTagCompound tags = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
		tags.setByte(NBT_TYPE, (byte) type.ordinal());
		tags.setByte(NBT_TIER, (byte) tier);
		tags.setShort(NBT_REQUIRED, (short) type.getRequired(tier));
		tags.setShort(NBT_PROGRESS, (short) 0);
		stack.setTagCompound(tags);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);

		if (!world.isRemote) {
			if (!isSigned(stack)) {
				signContract(stack, world.rand);
				player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".contract.signed", new TextComponentTranslation(getType(stack).getTranslationKey()), getRequired(stack)).setStyle(new Style().setColor(TextFormatting.GOLD)), true);
				world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 1.0F, 1.0F);

			} else if (isComplete(stack)) {
				giveRewards(world, player, getTier(stack));
				stack.shrink(1);

			} else {
				player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".contract.progress", new TextComponentTranslation(getType(stack).getTranslationKey()), getProgress(stack), getRequired(stack)).setStyle(new Style().setColor(TextFormatting.YELLOW)), true);
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	protected static void giveRewards(World world, EntityPlayer player, int tier) {
		tier = MathUtil.clamp(tier, 0, MAX_TIER);

		//loot
		LootTable table = world.getLootTableManager().getLootTableFromLocation(REWARD_LOOT[tier]);
		LootContext context = new LootContext.Builder((WorldServer) world).withPlayer(player).build();
		for (ItemStack loot : table.generateLootForPools(world.rand, context)) {
			if (!player.addItemStackToInventory(loot)) {
				world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, loot));
			}
		}

		//experience
		int xp = REWARD_XP[tier];
		while (xp > 0) {
			int j = EntityXPOrb.getXPSplit(xp);
			xp -= j;
			world.spawnEntity(new EntityXPOrb(world, player.posX, player.posY + 0.5D, player.posZ, j));
		}

		//combat buffs
		switch (tier) {
		case 0:
			player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 20, 0));
			player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 20 * 60 * 3, 0));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 30, 1));
			player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * 60 * 5, 0));
			player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 20 * 60 * 5, 0));
			break;
		default:
			player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 20 * 60 * 2, 2));
			player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * 60 * 10, 1));
			player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 20 * 60 * 10, 1));
			player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 20 * 60 * 10, 0));
			break;
		}

		world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
		player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".contract.rewarded", TIER_NAMES[tier]).setStyle(new Style().setColor(TextFormatting.GREEN)));
	}

	/**
	 * Called when a player killed an entity, counts the kill for all matching contracts in the inventory
	 */
	public static void onEntityKilled(EntityPlayer player, EntityLivingBase victim) {
		for (ItemStack stack : player.inventory.mainInventory) {
			updateContract(player, stack, victim);
		}
		for (ItemStack stack : player.inventory.offHandInventory) {
			updateContract(player, stack, victim);
		}
	}

	protected static void updateContract(EntityPlayer player, ItemStack stack, EntityLivingBase victim) {
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemMilitaryContract) || !isSigned(stack) || isComplete(stack)) {
			return;
		}
		ContractType type = getType(stack);
		if (type.matches(victim)) {
			int progress = getProgress(stack) + 1;
			stack.getTagCompound().setShort(NBT_PROGRESS, (short) progress);

			if (progress >= getRequired(stack)) {
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".contract.completed", new TextComponentTranslation(type.getTranslationKey())).setStyle(new Style().setColor(TextFormatting.GREEN)));
				player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7F, 1.4F);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack stack) {
		return isComplete(stack);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if (!isSigned(stack)) {
			tooltip.add(ChatFormatting.GRAY + I18n.format(Techguns.MODID + ".contract.tooltip.unsigned"));
		} else {
			tooltip.add(ChatFormatting.GOLD + I18n.format(Techguns.MODID + ".contract.tooltip.task", I18n.format(getType(stack).getTranslationKey())));
			tooltip.add(ChatFormatting.GRAY + I18n.format(Techguns.MODID + ".contract.tooltip.progress", getProgress(stack), getRequired(stack)));
			tooltip.add(ChatFormatting.AQUA + I18n.format(Techguns.MODID + ".contract.tooltip.tier", TIER_NAMES[getTier(stack)]));
			if (isComplete(stack)) {
				tooltip.add(ChatFormatting.GREEN + I18n.format(Techguns.MODID + ".contract.tooltip.complete"));
			}
		}
	}

}
