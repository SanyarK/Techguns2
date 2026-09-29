package techguns.entities.npcs;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import techguns.TGuns;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource;

/**
 * Final boss of the story campaign: a super soldier mutated by the mutagen.
 * Three phases: brawler, minigun + mutant summons, berserk with shockwaves.
 */
public class PrototypeBoss extends SuperMutantElite implements ITGBoss {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/prototype");

	protected static final int SUMMON_INTERVAL = 400;
	protected static final int SHOCKWAVE_INTERVAL = 90;
	protected static final double SHOCKWAVE_RANGE = 6.0D;
	protected static final int MAX_MINIONS = 3;

	private static final String RAGE_MODIFIER_NAME = "techguns.prototype.rage";
	private static final java.util.UUID RAGE_MODIFIER_UUID = java.util.UUID.fromString("52b7cbe8-9d43-4c8f-8a5e-7b1f0e2d3c99");

	private final BossInfoServer bossInfo = (BossInfoServer) (new BossInfoServer(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.NOTCHED_10));

	/** 1 = melee, 2 = minigun + summons, 3 = berserk */
	protected int phase = 1;
	protected int summonTimer = 200;
	protected int shockwaveTimer = SHOCKWAVE_INTERVAL;

	public PrototypeBoss(World world) {
		super(world);
		setTGArmorStats(18.0f, 2f);
		this.experienceValue = 250;
	}

	@Override
	public int gettype() {
		return 3;
	}

	@Override
	public double getModelHeightOffset() {
		return 1.15d;
	}

	@Override
	public float getModelScale() {
		return 1.8f;
	}

	@Override
	protected float getMutantWidth() {
		return 1.5f;
	}

	@Override
	public float getWeaponPosX() {
		return 0.28f;
	}

	@Override
	public float getWeaponPosZ() {
		return -0.45f;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.27D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(500);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(14);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(4.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		switch (dmgsrc.damageType) {
		case POISON:
		case RADIATION:
			return 20.0f;
		case PHYSICAL:
		case PROJECTILE:
			return 12.0f;
		case UNRESISTABLE:
			return 0.0f;
		default:
			return 15.0f;
		}
	}

	@Override
	public int getTotalArmorValue() {
		return 12;
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		//phase 1 fights with bare fists
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.world.isRemote) {
			return;
		}

		this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());

		if (this.phase < 2 && this.getHealth() < this.getMaxHealth() * 0.66f) {
			this.enterPhase2();
		}
		if (this.phase < 3 && this.getHealth() < this.getMaxHealth() * 0.33f) {
			this.enterPhase3();
		}

		EntityLivingBase target = this.getAttackTarget();
		if (target != null && target.isEntityAlive()) {
			if (this.phase >= 2 && --this.summonTimer <= 0) {
				this.summonTimer = this.phase >= 3 ? SUMMON_INTERVAL / 2 : SUMMON_INTERVAL;
				this.summonMutants(target);
			}
			if (this.phase >= 3 && --this.shockwaveTimer <= 0 && this.getDistanceSq(target) < SHOCKWAVE_RANGE * SHOCKWAVE_RANGE) {
				this.shockwaveTimer = SHOCKWAVE_INTERVAL;
				this.shockwave();
			}
		}
	}

	protected void enterPhase2() {
		this.phase = 2;
		this.equipMinigun();
		this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0f, 1.4f);
		BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.prototype.phase2").setStyle(new Style().setColor(TextFormatting.LIGHT_PURPLE)), 48.0D);
	}

	protected void equipMinigun() {
		if (TGuns.minigun != null) {
			this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(TGuns.minigun));
			this.setCombatTask();
		}
	}

	protected void enterPhase3() {
		this.phase = 3;
		this.applyRageModifier();
		this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * 60 * 10, 1));
		this.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 8, 1));
		this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 3.0f, 0.5f);
		BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.prototype.phase3").setStyle(new Style().setColor(TextFormatting.DARK_RED)), 48.0D);
	}

	protected void applyRageModifier() {
		IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		if (speed.getModifier(RAGE_MODIFIER_UUID) == null) {
			speed.applyModifier(new AttributeModifier(RAGE_MODIFIER_UUID, RAGE_MODIFIER_NAME, 0.4D, 1));
		}
	}

	protected void summonMutants(EntityLivingBase target) {
		List<SuperMutantBasic> nearby = this.world.getEntitiesWithinAABB(SuperMutantBasic.class, this.getEntityBoundingBox().grow(24.0D));
		//nearby includes the boss itself
		int count = Math.min(2, MAX_MINIONS + 1 - nearby.size());
		boolean spawned = false;
		for (int i = 0; i < count; i++) {
			if (BossHelper.spawnMinionNear(this, new SuperMutantBasic(this.world), target, 5)) {
				spawned = true;
			}
		}
		if (spawned) {
			this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2.0f, 0.7f);
			BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.prototype.summon").setStyle(new Style().setColor(TextFormatting.GOLD)), 48.0D);
		}
	}

	protected void shockwave() {
		this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX, this.posY + 0.5D, this.posZ, 6, SHOCKWAVE_RANGE * 0.5D, 0.3D, SHOCKWAVE_RANGE * 0.5D, 0.0D);

		List<EntityLivingBase> entities = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(SHOCKWAVE_RANGE, 2.0D, SHOCKWAVE_RANGE));
		for (EntityLivingBase ent : entities) {
			if (ent == this || ent instanceof SuperMutantBasic) {
				continue;
			}
			double dx = ent.posX - this.posX;
			double dz = ent.posZ - this.posZ;
			double dist = MathHelper.sqrt(dx * dx + dz * dz);
			if (dist > SHOCKWAVE_RANGE) {
				continue;
			}
			if (ent.attackEntityFrom(DamageSource.causeMobDamage(this), 12.0f)) {
				if (dist < 0.1D) {
					dx = this.rand.nextDouble() - 0.5D;
					dz = this.rand.nextDouble() - 0.5D;
					dist = MathHelper.sqrt(dx * dx + dz * dz);
				}
				ent.knockBack(this, 1.4f, -dx / dist, -dz / dist);
				ent.motionY += 0.5D;
				ent.velocityChanged = true;
			}
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
		compound.setInteger("phase", this.phase);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.phase = Math.max(1, compound.getInteger("phase"));
		if (this.phase >= 2 && this.getHeldItemMainhand().isEmpty()) {
			this.equipMinigun();
		}
		if (this.phase >= 3) {
			this.applyRageModifier();
		}
		if (this.hasCustomName()) {
			this.bossInfo.setName(this.getDisplayName());
		}
	}

	@Override
	protected ResourceLocation getLootTable() {
		return LOOT;
	}
}
