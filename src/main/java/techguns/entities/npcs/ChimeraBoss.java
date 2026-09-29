package techguns.entities.npcs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;
import techguns.entities.projectiles.BioGunProjectile;
import techguns.entities.projectiles.EnumBulletFirePos;
import techguns.entities.projectiles.LaserProjectile;
import techguns.util.MathUtil;

/**
 * Final boss of the story campaign: Chimera, a mutant grown together with the machines of the
 * Hive. Three phases with their own boss bar color:
 * 1 "Flesh" - claws, acid spit and mutant servants,
 * 2 "Machine" - armor plates, eye laser, missile barrage from the back launcher, ground slam,
 * 3 "Fury" - faster, leaps with a landing shockwave, EMP nova, the Hive heals her while servants live.
 * Changing the phase makes her invulnerable for two seconds.
 */
public class ChimeraBoss extends GenericNPC implements ITGBoss {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/chimera");

	private static final DataParameter<Integer> PHASE = EntityDataManager.<Integer>createKey(ChimeraBoss.class, DataSerializers.VARINT);
	/** ticks of the current smash animation, for the model */
	private static final DataParameter<Integer> SMASH = EntityDataManager.<Integer>createKey(ChimeraBoss.class, DataSerializers.VARINT);

	protected static final double SLAM_RANGE = 6.0D;
	protected static final double NOVA_RANGE = 10.0D;
	protected static final int MAX_MINIONS = 5;
	protected static final int SHIELD_TICKS = 40;

	private static final String RAGE_MODIFIER_NAME = "techguns.chimera.rage";
	private static final UUID RAGE_MODIFIER_UUID = UUID.fromString("0d8c3b4e-7a61-4f0e-a5c2-3e9b1f6d2a47");

	private final BossInfoServer bossInfo = (BossInfoServer) (new BossInfoServer(this.getDisplayName(), BossInfo.Color.GREEN, BossInfo.Overlay.NOTCHED_10)).setDarkenSky(true);

	protected int summonTimer = 160;
	protected int spitTimer = 60;
	protected int laserTimer = 80;
	protected int barrageTimer = 120;
	protected int slamTimer = 90;
	protected int leapTimer = 200;
	protected int novaTimer = 240;
	protected int hiveTimer = 200;
	/** invulnerable while the phase changes */
	protected int shieldTicks = 0;
	protected boolean leaping = false;
	protected int leapStart = 0;
	/** marked missile impacts: x, y, z, ticks left */
	protected final List<double[]> strikes = new ArrayList<>();

	public ChimeraBoss(World world) {
		super(world);
		this.setSize(2.0F, 3.6F);
		this.setTGArmorStats(20.0f, 2.0f);
		this.experienceValue = 500;
		this.isImmuneToFire = true;
		this.hasAimedBowAnim = false;
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(PHASE, 1);
		this.dataManager.register(SMASH, 0);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.26D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(900);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(16);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(4.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
	}

	public int getPhase() {
		return this.dataManager.get(PHASE);
	}

	public int getSmashTicks() {
		return this.dataManager.get(SMASH);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		//claws and the built-in weapons, nothing to hold
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		int phase = this.getPhase();
		switch (dmgsrc.damageType) {
		case UNRESISTABLE:
			return 0.0f;
		case POISON:
		case RADIATION:
			return 20.0f;
		case ENERGY:
		case LIGHTNING:
			//the machine parts are weak against energy weapons
			return phase == 2 ? 8.0f : 12.0f;
		case FIRE:
			return phase == 1 ? 6.0f : 12.0f;
		case PHYSICAL:
		case PROJECTILE:
			return phase == 2 ? 16.0f : phase == 3 ? 9.0f : 11.0f;
		default:
			return phase == 2 ? 15.0f : 11.0f;
		}
	}

	@Override
	public int getTotalArmorValue() {
		return this.getPhase() == 2 ? 16 : 12;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source == DamageSource.FALL || source == DamageSource.IN_WALL || source == DamageSource.DROWN) {
			return false;
		}
		if (this.shieldTicks > 0 && source != DamageSource.OUT_OF_WORLD) {
			if (!this.world.isRemote && this.ticksExisted % 5 == 0) {
				this.playSound(SoundEvents.ENTITY_IRONGOLEM_HURT, 1.0f, 1.8f);
			}
			return false;
		}
		return super.attackEntityFrom(source, amount);
	}

	@Override
	public void fall(float distance, float damageMultiplier) {
		//lands on her claws
	}

	@Override
	public boolean attackEntityAsMob(Entity entityIn) {
		this.dataManager.set(SMASH, 12);
		return super.attackEntityAsMob(entityIn);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.world.isRemote) {
			return;
		}
		this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());

		int smash = this.dataManager.get(SMASH);
		if (smash > 0) {
			this.dataManager.set(SMASH, smash - 1);
		}
		if (this.shieldTicks > 0) {
			this.shieldTicks--;
			((WorldServer) this.world).spawnParticle(EnumParticleTypes.SPELL_WITCH, this.posX, this.posY + this.height * 0.5D, this.posZ, 6,
					this.width * 0.6D, this.height * 0.4D, this.width * 0.6D, 0.02D);
		}

		int phase = this.getPhase();
		if (phase < 2 && this.getHealth() < this.getMaxHealth() * 0.66f) {
			this.enterPhase(2);
		} else if (phase < 3 && this.getHealth() < this.getMaxHealth() * 0.33f) {
			this.enterPhase(3);
		}
		phase = this.getPhase();

		this.tickStrikes();

		if (this.leaping && this.onGround && this.ticksExisted - this.leapStart > 5) {
			this.leaping = false;
			this.slam(8.0D, 14.0f);
		}

		EntityLivingBase target = this.getAttackTarget();
		if (target == null || !target.isEntityAlive() || this.shieldTicks > 0) {
			return;
		}
		double distSq = this.getDistanceSq(target);

		if (--this.summonTimer <= 0) {
			this.summonTimer = phase == 2 ? 360 : 300;
			this.summonServants(target, phase);
		}
		if (phase != 2 && --this.spitTimer <= 0 && distSq > 16.0D && distSq < 30.0D * 30.0D) {
			this.spitTimer = phase == 1 ? 70 : 90;
			this.acidSpit(target);
		}
		if (phase >= 2) {
			if (--this.laserTimer <= 0 && distSq > 9.0D && distSq < 40.0D * 40.0D && this.canEntityBeSeen(target)) {
				this.laserTimer = phase == 2 ? 50 : 60;
				this.fireLaser(target);
			}
			if (--this.barrageTimer <= 0) {
				this.barrageTimer = phase == 2 ? 200 : 150;
				this.missileBarrage(target, phase == 2 ? 4 : 6);
			}
			if (--this.slamTimer <= 0 && distSq < SLAM_RANGE * SLAM_RANGE) {
				this.slamTimer = 90;
				this.slam(SLAM_RANGE, 12.0f);
			}
		}
		if (phase == 3) {
			if (--this.leapTimer <= 0 && distSq > 36.0D && distSq < 24.0D * 24.0D && this.onGround) {
				this.leapTimer = 160;
				this.leapAt(target);
			}
			if (--this.novaTimer <= 0 && distSq < NOVA_RANGE * NOVA_RANGE) {
				this.novaTimer = 220;
				this.nova();
			}
			if (--this.hiveTimer <= 0) {
				this.hiveTimer = 200;
				this.hiveHeal();
			}
		}
	}

	protected void enterPhase(int phase) {
		this.dataManager.set(PHASE, phase);
		this.shieldTicks = SHIELD_TICKS;
		this.updateBarName();
		if (phase == 2) {
			this.bossInfo.setColor(BossInfo.Color.YELLOW);
			this.playSound(SoundEvents.BLOCK_PISTON_EXTEND, 3.0f, 0.5f);
			this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0f, 1.6f);
			this.notifyPlayers("techguns.message.chimera.phase2", TextFormatting.YELLOW);
		} else {
			this.bossInfo.setColor(BossInfo.Color.RED);
			this.applyRageModifier();
			this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * 60 * 10, 0));
			this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 3.0f, 0.6f);
			this.notifyPlayers("techguns.message.chimera.phase3", TextFormatting.DARK_RED);
		}
	}

	protected void updateBarName() {
		ITextComponent name = new TextComponentTranslation("techguns.message.chimera.bar" + this.getPhase(), this.getDisplayName());
		this.bossInfo.setName(name);
	}

	protected void applyRageModifier() {
		IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		if (speed.getModifier(RAGE_MODIFIER_UUID) == null) {
			speed.applyModifier(new AttributeModifier(RAGE_MODIFIER_UUID, RAGE_MODIFIER_NAME, 0.35D, 1));
		}
	}

	protected void notifyPlayers(String key, TextFormatting color) {
		BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation(key).setStyle(new Style().setColor(color)), 64.0D);
	}

	/**
	 * points the head (and so the projectiles) at the target
	 */
	protected void aimAt(EntityLivingBase target) {
		double dx = target.posX - this.posX;
		double dy = target.posY + target.height * 0.5D - (this.posY + this.getEyeHeight());
		double dz = target.posZ - this.posZ;
		double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
		this.rotationYawHead = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
		this.rotationPitch = (float) (-(MathHelper.atan2(dy, horizontal) * (180D / Math.PI)));
	}

	protected void summonServants(EntityLivingBase target, int phase) {
		List<SuperMutantBasic> nearby = this.world.getEntitiesWithinAABB(SuperMutantBasic.class, this.getEntityBoundingBox().grow(24.0D));
		int free = MAX_MINIONS - nearby.size();
		if (free <= 0) {
			return;
		}
		List<EntityLiving> servants = new ArrayList<>();
		if (phase == 1) {
			servants.add(new SuperMutantBasic(this.world));
			servants.add(new SuperMutantBasic(this.world));
		} else if (phase == 2) {
			servants.add(new MutantWarrior(this.world));
			servants.add(new MutantWarrior(this.world));
		} else {
			servants.add(new SuperMutantElite(this.world));
			servants.add(new SuperMutantBasic(this.world));
			servants.add(new MutantWarrior(this.world));
		}
		boolean spawned = false;
		for (int i = 0; i < servants.size() && i < free; i++) {
			if (BossHelper.spawnMinionNear(this, servants.get(i), target, 6)) {
				spawned = true;
			}
		}
		if (spawned) {
			this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0f, 0.5f);
			this.notifyPlayers("techguns.message.chimera.summon", TextFormatting.GOLD);
		}
	}

	/**
	 * three blobs of acid and a poison cloud where the target stands
	 */
	protected void acidSpit(EntityLivingBase target) {
		this.aimAt(target);
		for (int i = 0; i < 3; i++) {
			BioGunProjectile acid = new BioGunProjectile(this.world, this, 7.0f, 1.3f, 60, 0.06f, 10.0f, 25.0f, 4.0f, 0.5f, false, EnumBulletFirePos.CENTER, 0.015D, 2);
			this.world.spawnEntity(acid);
		}
		EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(this.world, target.posX, target.posY, target.posZ);
		cloud.setOwner(this);
		cloud.setRadius(2.5f);
		cloud.setRadiusPerTick(-0.01f);
		cloud.setWaitTime(20);
		cloud.setDuration(120);
		cloud.setColor(0x7FBF3F);
		cloud.addEffect(new PotionEffect(MobEffects.POISON, 80, 1));
		this.world.spawnEntity(cloud);
		this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 2.0f, 0.6f);
	}

	protected void fireLaser(EntityLivingBase target) {
		this.aimAt(target);
		LaserProjectile laser = new LaserProjectile(this.world, this, 9.0f, 48.0f, 7, 0.02f, 30.0f, 48.0f, 6.0f, 1.0f, false, EnumBulletFirePos.CENTER);
		this.world.spawnEntity(laser);
		this.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
	}

	/**
	 * marks impact points around the target, the missiles hit 1.5 seconds later
	 */
	protected void missileBarrage(EntityLivingBase target, int count) {
		for (int i = 0; i < count; i++) {
			double x = target.posX;
			double z = target.posZ;
			if (i > 0) {
				x += (this.rand.nextDouble() - 0.5D) * 10.0D;
				z += (this.rand.nextDouble() - 0.5D) * 10.0D;
			}
			this.strikes.add(new double[] { x, target.posY, z, 30 + i * 4 });
		}
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + this.height, this.posZ, 12, 0.5D, 0.3D, 0.5D, 0.05D);
		this.playSound(SoundEvents.ENTITY_FIREWORK_LAUNCH, 3.0f, 0.5f);
		this.notifyPlayers("techguns.message.chimera.barrage", TextFormatting.RED);
	}

	protected void tickStrikes() {
		if (this.strikes.isEmpty()) {
			return;
		}
		WorldServer ws = (WorldServer) this.world;
		Iterator<double[]> it = this.strikes.iterator();
		while (it.hasNext()) {
			double[] s = it.next();
			s[3]--;
			if (s[3] <= 0) {
				it.remove();
				this.world.newExplosion(this, s[0], s[1], s[2], 2.2f, false, false);
			} else if ((int) s[3] % 4 == 0) {
				ws.spawnParticle(EnumParticleTypes.REDSTONE, s[0], s[1] + 0.2D, s[2], 10, 0.8D, 0.05D, 0.8D, 0.0D);
				ws.spawnParticle(EnumParticleTypes.FLAME, s[0], s[1] + 0.1D, s[2], 3, 0.6D, 0.02D, 0.6D, 0.01D);
			}
		}
	}

	/**
	 * shockwave around the boss: damage and knockback
	 */
	protected void slam(double range, float damage) {
		this.dataManager.set(SMASH, 12);
		this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX, this.posY + 0.5D, this.posZ, 8, range * 0.5D, 0.3D, range * 0.5D, 0.0D);
		BlockPos below = new BlockPos(this.posX, this.posY - 0.2D, this.posZ);
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.BLOCK_DUST, this.posX, this.posY + 0.1D, this.posZ, 40, range * 0.4D, 0.1D, range * 0.4D, 0.15D,
				Block.getStateId(this.world.getBlockState(below)));
		for (EntityLivingBase ent : this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(range, 2.0D, range))) {
			if (ent == this || ent instanceof SuperMutantBasic) {
				continue;
			}
			double dx = ent.posX - this.posX;
			double dz = ent.posZ - this.posZ;
			double dist = MathHelper.sqrt(dx * dx + dz * dz);
			if (dist > range) {
				continue;
			}
			if (ent.attackEntityFrom(DamageSource.causeMobDamage(this), damage)) {
				if (dist < 0.1D) {
					dx = this.rand.nextDouble() - 0.5D;
					dz = this.rand.nextDouble() - 0.5D;
					dist = MathHelper.sqrt(dx * dx + dz * dz);
				}
				ent.knockBack(this, 1.5f, -dx / dist, -dz / dist);
				ent.motionY += 0.5D;
				ent.velocityChanged = true;
			}
		}
	}

	protected void leapAt(EntityLivingBase target) {
		double dx = target.posX - this.posX;
		double dz = target.posZ - this.posZ;
		double dist = Math.max(MathHelper.sqrt(dx * dx + dz * dz), 0.1D);
		double power = Math.min(dist / 12.0D, 1.6D);
		this.motionX = dx / dist * power;
		this.motionZ = dz / dist * power;
		this.motionY = 0.85D;
		this.velocityChanged = true;
		this.leaping = true;
		this.leapStart = this.ticksExisted;
		this.playSound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 3.0f, 0.6f);
	}

	/**
	 * electromagnetic pulse of the reactor on her back: damage, knockback, slowness and weakness
	 */
	protected void nova() {
		WorldServer ws = (WorldServer) this.world;
		ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, this.posX, this.posY + this.height * 0.6D, this.posZ, 80, 1.0D, 1.0D, 1.0D, 0.6D);
		this.playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2.0f, 1.6f);
		this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 1.2f);
		for (EntityLivingBase ent : this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(NOVA_RANGE, 4.0D, NOVA_RANGE))) {
			if (ent == this || ent instanceof SuperMutantBasic || this.getDistanceSq(ent) > NOVA_RANGE * NOVA_RANGE) {
				continue;
			}
			if (ent.attackEntityFrom(DamageSource.causeMobDamage(this), 8.0f)) {
				double dx = ent.posX - this.posX;
				double dz = ent.posZ - this.posZ;
				double dist = Math.max(MathHelper.sqrt(dx * dx + dz * dz), 0.1D);
				ent.knockBack(this, 1.2f, -dx / dist, -dz / dist);
				ent.velocityChanged = true;
			}
			ent.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
			ent.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 0));
		}
		this.notifyPlayers("techguns.message.chimera.nova", TextFormatting.AQUA);
	}

	/**
	 * the Hive feeds her while her servants are alive
	 */
	protected void hiveHeal() {
		int servants = this.world.getEntitiesWithinAABB(SuperMutantBasic.class, this.getEntityBoundingBox().grow(24.0D)).size();
		if (servants <= 0 || this.getHealth() >= this.getMaxHealth() * 0.33f) {
			return;
		}
		this.heal(Math.min(40.0f, 12.0f * servants));
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX, this.posY + this.height * 0.5D, this.posZ, 20,
				this.width * 0.6D, this.height * 0.4D, this.width * 0.6D, 0.0D);
		this.playSound(SoundEvents.ENTITY_SLIME_SQUISH, 2.0f, 0.4f);
		this.notifyPlayers("techguns.message.chimera.heal", TextFormatting.GREEN);
	}

	@Override
	public void onDeath(DamageSource cause) {
		super.onDeath(cause);
		if (!this.world.isRemote) {
			this.strikes.clear();
			((WorldServer) this.world).spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.posX, this.posY + this.height * 0.5D, this.posZ, 4, 1.0D, 1.0D, 1.0D, 0.0D);
			this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.ENTITY_IRONGOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, Block blockIn) {
		this.playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 1.0F, 0.6F);
	}

	@Override
	protected float getSoundPitch() {
		return 0.6f + this.rand.nextFloat() * 0.1f;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public boolean isNonBoss() {
		return false;
	}

	@Override
	public void addTrackingPlayer(EntityPlayerMP player) {
		super.addTrackingPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void removeTrackingPlayer(EntityPlayerMP player) {
		super.removeTrackingPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	@Override
	public void setCustomNameTag(String name) {
		super.setCustomNameTag(name);
		this.updateBarName();
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setInteger("phase", this.getPhase());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		int phase = MathUtil.clamp(compound.getInteger("phase"), 1, 3);
		this.dataManager.set(PHASE, phase);
		if (phase >= 2) {
			this.bossInfo.setColor(phase == 2 ? BossInfo.Color.YELLOW : BossInfo.Color.RED);
		}
		if (phase >= 3) {
			this.applyRageModifier();
		}
		this.updateBarName();
	}

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
