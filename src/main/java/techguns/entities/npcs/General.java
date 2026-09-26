package techguns.entities.npcs;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.TGArmors;
import techguns.TGSounds;
import techguns.TGuns;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.RocketProjectile;

/**
 * Military boss. Calls in elite soldier reinforcements, switches to a rocket launcher
 * at half health and orders airstrikes on his target when outdoors.
 */
public class General extends GenericNPCGearSpecificStats implements ITGBoss {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/general");

	protected static final int REINFORCEMENT_INTERVAL = 500;
	protected static final int AIRSTRIKE_INTERVAL = 360;
	protected static final int AIRSTRIKE_DELAY = 50;
	protected static final int MAX_REINFORCEMENTS = 4;

	private final BossInfoServer bossInfo = (BossInfoServer) (new BossInfoServer(this.getDisplayName(), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10));

	protected int reinforcementTimer = 200;
	protected int airstrikeTimer = 100;
	protected int airstrikeCountdown = -1;
	protected double strikeX;
	protected double strikeY;
	protected double strikeZ;
	protected boolean secondPhase = false;

	public General(World world) {
		super(world);
		this.experienceValue = 150;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(350);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(80.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		this.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(TGArmors.t2_beret));
		this.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(TGArmors.t4_power_Chestplate));
		this.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(TGArmors.t4_power_Leggings));
		this.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(TGArmors.t4_power_Boots));

		if (TGuns.scar != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(TGuns.scar));
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		//the beret is weak, give the boss some extra protection
		return super.getTotalArmorAgainstType(dmgsrc) + 4.0f;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.world.isRemote) {
			return;
		}

		this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());

		if (!this.secondPhase && this.getHealth() < this.getMaxHealth() * 0.5f) {
			this.enterSecondPhase();
		}

		EntityLivingBase target = this.getAttackTarget();
		if (target != null && target.isEntityAlive()) {

			if (--this.reinforcementTimer <= 0) {
				this.reinforcementTimer = REINFORCEMENT_INTERVAL;
				this.callReinforcements(target);
			}

			if (this.secondPhase && this.airstrikeCountdown < 0 && --this.airstrikeTimer <= 0) {
				this.airstrikeTimer = AIRSTRIKE_INTERVAL;
				this.markAirstrike(target);
			}
		}

		if (this.airstrikeCountdown >= 0) {
			if (this.airstrikeCountdown == 0) {
				this.launchAirstrike();
			} else if (this.airstrikeCountdown % 5 == 0) {
				((WorldServer) this.world).spawnParticle(EnumParticleTypes.REDSTONE, this.strikeX, this.strikeY + 0.5D, this.strikeZ, 20, 1.5D, 0.3D, 1.5D, 0.0D);
			}
			this.airstrikeCountdown--;
		}
	}

	protected void enterSecondPhase() {
		this.secondPhase = true;
		if (TGuns.rocketlauncher != null) {
			this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(TGuns.rocketlauncher));
			this.setCombatTask();
		}
		this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.0F, 0.8F);
		BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.general.phase2").setStyle(new Style().setColor(TextFormatting.RED)), 48.0D);
	}

	protected void callReinforcements(EntityLivingBase target) {
		List<EliteSoldier> nearby = this.world.getEntitiesWithinAABB(EliteSoldier.class, this.getEntityBoundingBox().grow(32.0D));
		int count = Math.min(2, MAX_REINFORCEMENTS - nearby.size());
		boolean spawned = false;
		for (int i = 0; i < count; i++) {
			if (BossHelper.spawnMinionNear(this, new EliteSoldier(this.world), target, 5)) {
				spawned = true;
			}
		}
		if (spawned) {
			BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.general.reinforcements").setStyle(new Style().setColor(TextFormatting.GOLD)), 48.0D);
		}
	}

	protected void markAirstrike(EntityLivingBase target) {
		//no airstrikes in bunkers and caves
		if (!this.world.canSeeSky(new BlockPos(target.posX, target.posY + target.getEyeHeight(), target.posZ))) {
			this.reinforcementTimer = Math.min(this.reinforcementTimer, 40);
			return;
		}
		this.strikeX = target.posX;
		this.strikeY = target.posY;
		this.strikeZ = target.posZ;
		this.airstrikeCountdown = AIRSTRIKE_DELAY;

		if (target instanceof EntityPlayer) {
			((EntityPlayer) target).sendStatusMessage(new TextComponentTranslation("techguns.message.general.airstrike").setStyle(new Style().setColor(TextFormatting.RED)), true);
		}
	}

	protected void launchAirstrike() {
		this.world.playSound(null, this.strikeX, this.strikeY, this.strikeZ, TGSounds.ROCKET_FIRE, SoundCategory.HOSTILE, 4.0F, 0.7F);
		for (int i = 0; i < 6; i++) {
			double x = this.strikeX + (this.rand.nextDouble() * 2.0D - 1.0D) * 6.0D;
			double z = this.strikeZ + (this.rand.nextDouble() * 2.0D - 1.0D) * 6.0D;
			double y = Math.min(this.strikeY + 32.0D + i * 4.0D, this.world.getActualHeight() - 2);
			RocketProjectile rocket = new RocketProjectile(this.world, x, y, z, 0.0F, 90.0F, 16.0f, 1.2f, 100, 0.0f, 3.0f, 5.0f, 6.0f, 0.25f, false, EnumBulletFirePos.CENTER, 4.0f, 0.0D);
			this.world.spawnEntity(rocket);
		}
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	public boolean isNonBoss() {
		return false;
	}

	public void addTrackingPlayer(EntityPlayerMP player) {
		super.addTrackingPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	public void removeTrackingPlayer(EntityPlayerMP player) {
		super.removeTrackingPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	@Override
	public void setCustomNameTag(String name) {
		super.setCustomNameTag(name);
		this.bossInfo.setName(this.getDisplayName());
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setBoolean("secondPhase", this.secondPhase);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.secondPhase = compound.getBoolean("secondPhase");
		if (this.hasCustomName()) {
			this.bossInfo.setName(this.getDisplayName());
		}
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

	@Override
    protected void playStepSound(BlockPos pos, Block blockIn)
    {
        this.playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 0.3F, 1.2F);
    }

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
