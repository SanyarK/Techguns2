package techguns.entities.npcs;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFindEntityNearestPlayer;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.TGPackets;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.client.audio.TGSoundCategory;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGExplosion;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.RocketProjectile;
import techguns.packets.PacketPlaySound;
import techguns.packets.PacketSpawnParticle;
import techguns.util.MathUtil;

/**
 * Military ground attack jet. Flies attack runs over its target, strafes with its cannon,
 * fires rockets downwards when passing over the target and drops paratroopers.
 */
public class MilitaryJet extends GenericFlyingMob {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/militaryjet");

	public static final int MAX_DEATH_TIME = 80;

	protected static final int FLY_HEIGHT = 26;
	protected static final double SPEED = 0.8D;
	protected static final float TURN_RATE = 4.5F;
	protected static final double PATROL_RADIUS = 40.0D;
	protected static final int BURST_SIZE = 8;

	protected int paratroopers = 0;
	protected int attackRunTimer = 0;
	protected int gunTimer = 0;
	protected int burstShots = 0;
	protected int bombTimer = 0;
	protected int dropTimer = 0;
	protected int engineSoundTimer = 0;

	protected boolean hasHome = false;
	protected double homeX;
	protected double homeZ;

	public MilitaryJet(World worldIn) {
		super(worldIn);
		this.setSize(4.0F, 2.0F);
		this.isImmuneToFire = true;
		this.experienceValue = 30;
	}

	@Override
	protected void initEntityAI() {
		this.targetTasks.addTask(1, new EntityAIFindEntityNearestPlayer(this));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(120.0D);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(96.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(10.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(4.0D);
	}

	@Override
	@Nullable
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
		livingdata = super.onInitialSpawn(difficulty, livingdata);
		this.paratroopers = 2 + this.rand.nextInt(2);
		return livingdata;
	}

	public void setParatroopers(int count) {
		this.paratroopers = count;
	}

	@Override
	@Nullable
	protected ResourceLocation getLootTable() {
		return LOOT;
	}

	@Override
	public void onLivingUpdate() {
		if (!this.world.isRemote && this.isEntityAlive()) {
			this.updateFlight();
		}
		super.onLivingUpdate();
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		//the jet always faces its flight direction
		this.renderYawOffset = this.rotationYaw;
		this.prevRenderYawOffset = this.prevRotationYaw;
		this.rotationYawHead = this.rotationYaw;
		this.prevRotationYawHead = this.prevRotationYaw;
	}

	protected void updateFlight() {

		if (!this.hasHome) {
			this.homeX = this.posX;
			this.homeZ = this.posZ;
			this.hasHome = true;
		}

		EntityLivingBase target = this.getAttackTarget();
		if (target != null && (!target.isEntityAlive() || (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage))) {
			this.setAttackTarget(null);
			target = null;
		}

		boolean steer = true;
		double tx;
		double tz;

		if (target != null) {
			double dx = target.posX - this.posX;
			double dz = target.posZ - this.posZ;
			double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

			tx = target.posX;
			tz = target.posZ;
			if (this.attackRunTimer > 0) {
				//fly straight on after a pass, then turn around for the next one
				this.attackRunTimer--;
				steer = false;
			} else if (horizontalDistance < 8.0D) {
				this.attackRunTimer = 45;
			}

			this.updateCombat(target, horizontalDistance);

		} else {
			//patrol in circles around the spawn point
			double angle = Math.atan2(this.posZ - this.homeZ, this.posX - this.homeX) + 0.5D;
			tx = this.homeX + Math.cos(angle) * PATROL_RADIUS;
			tz = this.homeZ + Math.sin(angle) * PATROL_RADIUS;
		}

		if (steer) {
			float desiredYaw = (float) (-MathHelper.atan2(tx - this.posX, tz - this.posZ) * (180D / Math.PI));
			float delta = MathUtil.clamp(wrapDegrees(desiredYaw - this.rotationYaw), -TURN_RATE, TURN_RATE);
			this.rotationYaw += delta;
		}

		double yawRad = this.rotationYaw * Math.PI / 180.0D;
		double dirX = -Math.sin(yawRad);
		double dirZ = Math.cos(yawRad);

		int ground = Math.max(this.getGroundHeight(this.posX, this.posZ), this.getGroundHeight(this.posX + dirX * 16.0D, this.posZ + dirZ * 16.0D));
		double desiredY = ground + FLY_HEIGHT;
		if (target != null) {
			desiredY = Math.max(desiredY, target.posY + 14.0D);
		}
		desiredY = Math.min(desiredY, this.world.getActualHeight() - 4);

		this.motionX = dirX * SPEED;
		this.motionZ = dirZ * SPEED;
		this.motionY = MathUtil.clamp((desiredY - this.posY) * 0.05D, -0.35D, 0.35D);
		this.rotationPitch = (float) MathUtil.clamp(-this.motionY * 60.0D, -20.0D, 20.0D);

		if (--this.engineSoundTimer <= 0) {
			this.engineSoundTimer = 30;
			this.playSound(TGSounds.JETPACK_LOOP, 6.0F, 0.5F);
		}
	}

	protected static float wrapDegrees(float value) {
		value = value % 360.0F;
		if (value >= 180.0F) {
			value -= 360.0F;
		}
		if (value < -180.0F) {
			value += 360.0F;
		}
		return value;
	}
	
	protected int getGroundHeight(double x, double z) {
		return this.world.getHeight(MathHelper.floor(x), MathHelper.floor(z));
	}

	protected void updateCombat(EntityLivingBase target, double horizontalDistance) {
		if (this.gunTimer > 0) this.gunTimer--;
		if (this.bombTimer > 0) this.bombTimer--;
		if (this.dropTimer > 0) this.dropTimer--;

		//strafing run with the cannon
		if (this.attackRunTimer == 0 && this.gunTimer <= 0 && horizontalDistance > 10.0D && horizontalDistance < 56.0D) {
			double dx = target.posX - this.posX;
			double dz = target.posZ - this.posZ;
			float yawToTarget = (float) (-MathHelper.atan2(dx, dz) * (180D / Math.PI));
			if (Math.abs(wrapDegrees(yawToTarget - this.rotationYaw)) < 25.0F && this.canEntityBeSeen(target)) {
				this.fireBullet(target);
			}
		}

		//drop rockets when right above the target
		if (horizontalDistance < 6.0D && this.bombTimer <= 0) {
			this.bombTimer = 60;
			this.dropRockets();
		}

		//drop paratroopers when closing in
		if (this.paratroopers > 0 && this.dropTimer <= 0 && horizontalDistance < 30.0D) {
			this.dropTimer = 8;
			this.dropParatrooper(target);
		}
	}

	protected void fireBullet(EntityLivingBase target) {
		if (this.burstShots == 0) {
			TGPackets.network.sendToAllAround(new PacketPlaySound(TGSounds.HELICOPTER_BURST, this, 8.0f, 1.2f, false, false, TGSoundCategory.GUN_FIRE), TGPackets.targetPointAroundEnt(this, 100.0f));
		}

		double dx = target.posX - this.posX;
		double dy = (target.posY + target.height * 0.5D) - (this.posY + this.getEyeHeight());
		double dz = target.posZ - this.posZ;
		double horizontal = MathHelper.sqrt(dx * dx + dz * dz);

		float oldYawHead = this.rotationYawHead;
		float oldPitch = this.rotationPitch;
		this.rotationYawHead = (float) (-MathHelper.atan2(dx, dz) * (180D / Math.PI));
		this.rotationPitch = (float) (-MathHelper.atan2(dy, horizontal) * (180D / Math.PI));

		GenericProjectile bullet = new GenericProjectile(this.world, this, 8.0f, 2.5f, 60, 0.04f, 30, 50, 5.0f, 0.2f, false, EnumBulletFirePos.CENTER);
		this.world.spawnEntity(bullet);

		this.rotationYawHead = oldYawHead;
		this.rotationPitch = oldPitch;

		this.burstShots++;
		if (this.burstShots >= BURST_SIZE) {
			this.burstShots = 0;
			this.gunTimer = 40;
		} else {
			this.gunTimer = 2;
		}
	}

	protected void dropRockets() {
		TGPackets.network.sendToAllAround(new PacketPlaySound(TGSounds.ROCKET_FIRE, this, 8.0f, 0.8f, false, false, TGSoundCategory.GUN_FIRE), TGPackets.targetPointAroundEnt(this, 100.0f));

		float oldYawHead = this.rotationYawHead;
		float oldPitch = this.rotationPitch;
		this.rotationYawHead = this.rotationYaw;
		this.rotationPitch = 90.0F;

		RocketProjectile left = new RocketProjectile(this.world, this, 14.0f, 1.0f, 80, 0.08f, 3.0f, 5.0f, 6.0f, 0.25f, false, EnumBulletFirePos.LEFT, 4.0f, 0.0D);
		this.world.spawnEntity(left);
		RocketProjectile right = new RocketProjectile(this.world, this, 14.0f, 1.0f, 80, 0.08f, 3.0f, 5.0f, 6.0f, 0.25f, false, EnumBulletFirePos.RIGHT, 4.0f, 0.0D);
		this.world.spawnEntity(right);

		this.rotationYawHead = oldYawHead;
		this.rotationPitch = oldPitch;
	}

	protected void dropParatrooper(EntityLivingBase target) {
		Paratrooper trooper = new Paratrooper(this.world);
		trooper.setLocationAndAngles(this.posX, this.posY - 2.0D, this.posZ, this.rotationYaw, 0.0F);
		if (this.world.getCollisionBoxes(trooper, trooper.getEntityBoundingBox()).isEmpty()) {
			trooper.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(trooper)), (IEntityLivingData) null);
			trooper.setParachute(true);
			trooper.motionX = this.motionX * 0.3D;
			trooper.motionZ = this.motionZ * 0.3D;
			this.world.spawnEntity(trooper);
			trooper.setAttackTarget(target);
			this.paratroopers--;
		}
	}

	@Override
	protected void onDeathUpdate() {
		if (this.deathTime == 0) {
			this.playSound(TGSounds.HELICOPTER_DEATH, 6.0F, 1.2F);
		}
		++this.deathTime;

		if (!this.world.isRemote) {
			//keep gliding forward while going down
			double yawRad = this.rotationYaw * Math.PI / 180.0D;
			this.motionX = -Math.sin(yawRad) * SPEED * 0.7D;
			this.motionZ = Math.cos(yawRad) * SPEED * 0.7D;
			this.motionY = Math.max(this.motionY - 0.04D, -1.2D);
			this.rotationPitch = 25.0F;

			if (this.deathTime % 3 == 0) {
				((WorldServer) this.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + 1.0D, this.posZ, 6, 0.6D, 0.4D, 0.6D, 0.02D);
				((WorldServer) this.world).spawnParticle(EnumParticleTypes.FLAME, this.posX, this.posY + 1.0D, this.posZ, 3, 0.4D, 0.3D, 0.4D, 0.02D);
			}
		}

		if (this.deathTime >= MAX_DEATH_TIME || (this.onGround && this.deathTime > 5)) {
			if (!this.world.isRemote) {
				if (this.recentlyHit > 0 && this.canDropLoot() && this.world.getGameRules().getBoolean("doMobLoot")) {
					int i = this.getExperiencePoints(this.attackingPlayer);
					i = net.minecraftforge.event.ForgeEventFactory.getExperienceDrop(this, this.attackingPlayer, i);
					while (i > 0) {
						int j = EntityXPOrb.getXPSplit(i);
						i -= j;
						this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, j));
					}
				}

				TGPackets.network.sendToAllAround(new PacketSpawnParticle("RocketExplosion", this.posX, this.posY + 1.0D, this.posZ), TGPackets.targetPointAroundEnt(this, 100.0f));
				this.world.playSound(null, this.posX, this.posY, this.posZ, TGSounds.HELICOPTER_EXPLODE, SoundCategory.HOSTILE, 6.0F, 1.0F);

				TGExplosion explosion = new TGExplosion(this.world, this, this, this.posX, this.posY + 1.0D, this.posZ, 16.0D, 6.0D, 3.0D, 6.0D, 0.0D);
				explosion.doExplosion(true);
			}
			this.setDead();
		}
	}

	@Override
	public float getEyeHeight() {
		return 0.8f;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return TGSounds.HELICOPTER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected float getSoundVolume() {
		return 6.0F;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setByte("paratroopers", (byte) this.paratroopers);
		compound.setBoolean("hasHome", this.hasHome);
		compound.setDouble("homeX", this.homeX);
		compound.setDouble("homeZ", this.homeZ);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.paratroopers = compound.getByte("paratroopers");
		this.hasHome = compound.getBoolean("hasHome");
		this.homeX = compound.getDouble("homeX");
		this.homeZ = compound.getDouble("homeZ");
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		switch(dmgsrc.damageType){
			case EXPLOSION:
				return 0.0f;
			case LIGHTNING:
			case ENERGY:
				return 8.0f;
			case FIRE:
			case ICE:
			case PHYSICAL:
			case PROJECTILE:
			case POISON:
			case RADIATION:
				return 16.0f;
			case UNRESISTABLE:
			default:
				return 0.0f;
		}
	}

	@Override
	public float getPenetrationResistance(TGDamageSource dmgsrc) {
		return dmgsrc.damageType == techguns.api.damagesystem.DamageType.PROJECTILE ? 0.35f : 0.0f;
	}

	@Override
	public float getWeaponPosX() {
		return 0;
	}

	@Override
	public float getWeaponPosY() {
		return 0;
	}

	@Override
	public float getWeaponPosZ() {
		return 0;
	}

	@Override
	protected int getTargetFlyHeight() {
		return FLY_HEIGHT;
	}
}
