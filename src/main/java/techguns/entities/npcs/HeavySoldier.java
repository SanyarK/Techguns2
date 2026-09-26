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
 * Slow but very tough soldier in riot armor carrying heavy weapons.
 */
public class HeavySoldier extends GenericNPCGearSpecificStats {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/heavysoldier");

	public HeavySoldier(World world) {
		super(world);
		this.experienceValue = 20;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.22D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(60);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(8);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {

		// Armors
		int camo = GenericArmorMultiCamo.getRandomCamoIndexFor((GenericArmorMultiCamo) TGArmors.t2_riot_Chestplate);
		this.setItemStackToSlot(EntityEquipmentSlot.HEAD, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t2_riot_Helmet, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.CHEST, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t2_riot_Chestplate, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.LEGS, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t2_riot_Leggings, camo));
		this.setItemStackToSlot(EntityEquipmentSlot.FEET, GenericArmorMultiCamo.getNewWithCamo(TGArmors.t2_riot_Boots, camo));

		// Weapons
		Item weapon = null;
		switch (this.rand.nextInt(4)) {
		case 0:
			weapon = TGuns.minigun;
			break;
		case 1:
			weapon = TGuns.rocketlauncher;
			break;
		case 2:
			weapon = TGuns.flamethrower;
			break;
		default:
			weapon = TGuns.lmg;
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
		return SoundEvents.ENTITY_IRONGOLEM_STEP;
	}

	@Override
    protected void playStepSound(BlockPos pos, Block blockIn)
    {
        this.playSound(this.getStepSound(), 0.25F, 1.2F);
    }

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
