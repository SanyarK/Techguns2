package techguns.entities.npcs;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import techguns.TGArmors;
import techguns.TGuns;
import techguns.Techguns;

/**
 * Soldier that gets dropped by military aircraft, slowly glides down on a parachute.
 */
public class Paratrooper extends ArmySoldier {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/paratrooper");

	private static final DataParameter<Boolean> PARACHUTE = EntityDataManager.<Boolean>createKey(Paratrooper.class, DataSerializers.BOOLEAN);

	public static final double PARACHUTE_FALL_SPEED = 0.15D;

	public Paratrooper(World world) {
		super(world);
		this.experienceValue = 8;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(PARACHUTE, false);
	}

	public boolean hasParachute() {
		return this.dataManager.get(PARACHUTE).booleanValue();
	}

	public void setParachute(boolean parachute) {
		this.dataManager.set(PARACHUTE, Boolean.valueOf(parachute));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.hasParachute()) {
			this.fallDistance = 0f;
			if (this.motionY < -PARACHUTE_FALL_SPEED) {
				this.motionY = -PARACHUTE_FALL_SPEED;
			}

			if (!this.world.isRemote && (this.onGround || this.isInWater() || this.isInLava())) {
				this.setParachute(false);
			}
		}
	}

	@Override
	protected void addRandomArmor(int difficulty) {

		// Armors
		this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(TGArmors.t2_combat_Helmet));
		this.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(TGArmors.t2_combat_Chestplate));
		if (this.rand.nextBoolean()) {
			this.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(TGArmors.t2_combat_Leggings));
		}
		this.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(TGArmors.t2_combat_Boots));

		// Weapons
		Item weapon = null;
		switch (this.rand.nextInt(4)) {
		case 0:
			weapon = TGuns.pdw;
			break;
		case 1:
			weapon = TGuns.mac10;
			break;
		case 2:
			weapon = TGuns.combatshotgun;
			break;
		default:
			weapon = TGuns.m4;
			break;
		}
		if (weapon != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setBoolean("parachute", this.hasParachute());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.setParachute(compound.getBoolean("parachute"));
	}

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
