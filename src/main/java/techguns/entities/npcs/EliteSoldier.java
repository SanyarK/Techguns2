package techguns.entities.npcs;

import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import techguns.TGArmors;
import techguns.TGuns;
import techguns.Techguns;
import techguns.items.armors.GenericArmorMultiCamo;

/**
 * Special forces soldier, much tougher than the regular army soldier.
 * Wears T3 combat armor and uses modern assault rifles.
 */
public class EliteSoldier extends GenericNPCGearSpecificStats {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/elitesoldier");

	public EliteSoldier(World world) {
		super(world);
		this.experienceValue = 15;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(45);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(80.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {

		// Armors, all pieces use the same camo
		int camo = this.rand.nextInt(3);
		this.setItemStackToSlot(EntityEquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t3_combat_Helmet, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t3_combat_Chestplate, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t3_combat_Leggings, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t3_combat_Boots, camo));

		// Weapons
		Item weapon = null;
		switch (this.rand.nextInt(5)) {
		case 0:
			weapon = TGuns.scar;
			break;
		case 1:
			weapon = TGuns.aug;
			break;
		case 2:
			weapon = TGuns.vector;
			break;
		case 3:
			weapon = difficulty >= 2 ? TGuns.grenadelauncher : TGuns.m4_infiltrator;
			break;
		default:
			weapon = TGuns.as50;
			break;
		}
		if (weapon != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VILLAGER_AMBIENT;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.ENTITY_VILLAGER_HURT;
	}

	@Override
	public SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VILLAGER_DEATH;
	}

	public SoundEvent getStepSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP;
	}

	@Override
    protected void playStepSound(BlockPos pos, Block blockIn)
    {
        this.playSound(this.getStepSound(), 0.15F, 1.0F);
    }

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
