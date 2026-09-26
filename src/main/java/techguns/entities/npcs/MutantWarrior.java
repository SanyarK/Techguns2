package techguns.entities.npcs;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import techguns.TGuns;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;

/**
 * Fast close combat super mutant, charges at its target with melee weapons or a flamethrower.
 */
public class MutantWarrior extends SuperMutantBasic {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/mutantwarrior");

	public MutantWarrior(World world) {
		super(world);
		setTGArmorStats(12.0f, 0.5f);
		this.experienceValue = 15;
	}

	@Override
	public int gettype() {
		return 3;
	}

	@Override
	public double getModelHeightOffset() {
		return 0.65d;
	}

	@Override
	public float getModelScale() {
		return 1.45f;
	}

	@Override
	protected float getMutantWidth() {
		return 1.1f;
	}

	@Override
	public float getWeaponPosX() {
		return 0.15f;
	}

	@Override
	public float getWeaponPosZ() {
		return -0.26f;
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		switch(dmgsrc.damageType){
		case EXPLOSION:
		case LIGHTNING:
		case ENERGY:
		case ICE:
			return 12.0f;
		case FIRE:
			return 16.0f;
		case PHYSICAL:
		case PROJECTILE:
			return 9.0f;
		case POISON:
		case RADIATION:
			return 16.0f;
		case UNRESISTABLE:
		default:
			return 0.0f;
		}
	}

	@Override
	public int getTotalArmorValue() {
		return 9;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.34D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(55);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(50.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(2.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		Item weapon = null;
		switch (this.rand.nextInt(4)) {
			case 0:
				weapon = TGuns.chainsaw;
				break;
			case 1:
				weapon = TGuns.powerhammer;
				break;
			case 2:
				weapon = TGuns.shishkebap;
				break;
			default:
				weapon = TGuns.flamethrower;
				break;
		}
		if (weapon != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
	}

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
