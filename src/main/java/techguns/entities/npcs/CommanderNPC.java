package techguns.entities.npcs;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import techguns.TGArmors;
import techguns.api.npc.factions.TGNpcFaction;
import techguns.campaign.TGCampaign;

/**
 * Friendly quest giver of the story campaign. Lives at the player's command post
 * (and can be found on military bases), opens the mission dialog when right clicked.
 */
public class CommanderNPC extends GenericNPCGearSpecificStats {

	public CommanderNPC(World world) {
		super(world);
		//the commander does not fight anybody
		this.targetTasks.taskEntries.clear();
		this.experienceValue = 0;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(300);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
	}

	@Override
	public void setCombatTask() {
		//no combat AI
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(TGArmors.t2_beret));
		this.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(TGArmors.t3_combat_Chestplate));
		this.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(TGArmors.t3_combat_Leggings));
		this.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(TGArmors.t3_combat_Boots));
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.NEUTRAL;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) {
			return false;
		}
		if (!this.world.isRemote && player instanceof EntityPlayerMP) {
			TGCampaign.openDialog((EntityPlayerMP) player, true);
		}
		return true;
	}

	/**
	 * EntityMob removes itself on peaceful difficulty, but the commander is friendly
	 * and has to survive difficulty switches.
	 */
	@Override
	public void setDead() {
		if (this.world != null && !this.world.isRemote && this.getHealth() > 0
				&& this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			return;
		}
		super.setDead();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.ENTITY_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VILLAGER_DEATH;
	}
}
