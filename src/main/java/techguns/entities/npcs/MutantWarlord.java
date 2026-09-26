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
import net.minecraft.item.Item;
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
 * Giant super mutant boss. Smashes the ground around it, summons mutant warriors
 * and goes berserk at low health.
 */
public class MutantWarlord extends SuperMutantElite implements ITGBoss {

	public static final ResourceLocation LOOT = new ResourceLocation(Techguns.MODID, "entities/mutantwarlord");

	protected static final int SUMMON_INTERVAL = 400;
	protected static final int SLAM_INTERVAL = 120;
	protected static final double SLAM_RANGE = 6.0D;
	protected static final int MAX_MINIONS = 4;

	private static final String RAGE_MODIFIER_NAME = "techguns.warlord.rage";
	private static final java.util.UUID RAGE_MODIFIER_UUID = java.util.UUID.fromString("7c3f3b1e-5d2a-4f3e-9b8a-2f1d6c4e8a11");

	private final BossInfoServer bossInfo = (BossInfoServer) (new BossInfoServer(this.getDisplayName(), BossInfo.Color.GREEN, BossInfo.Overlay.NOTCHED_10));

	protected int summonTimer = 160;
	protected int slamTimer = SLAM_INTERVAL;
	protected boolean enraged = false;

	public MutantWarlord(World world) {
		super(world);
		setTGArmorStats(20.0f, 2f);
		this.experienceValue = 200;
	}

	@Override
	public int gettype() {
		return 4;
	}

	@Override
	public double getModelHeightOffset() {
		return 1.6d;
	}

	@Override
	public float getModelScale() {
		return 2.1f;
	}

	@Override
	protected float getMutantWidth() {
		return 1.8f;
	}

	@Override
	public float getWeaponPosX() {
		return 0.35f;
	}

	@Override
	public float getWeaponPosZ() {
		return -0.6f;
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageSource dmgsrc) {
		switch(dmgsrc.damageType){
		case EXPLOSION:
		case LIGHTNING:
		case ENERGY:
		case FIRE:
		case ICE:
			return 16.0f;
		case PHYSICAL:
		case PROJECTILE:
			return 13.0f;
		case POISON:
		case RADIATION:
			return 20.0f;
		case UNRESISTABLE:
		default:
			return 0.0f;
		}
	}

	@Override
	public int getTotalArmorValue() {
		return 14;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.24D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(400);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(16);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(4.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		Item weapon = this.rand.nextInt(3) == 0 ? TGuns.rocketlauncher : TGuns.minigun;
		if (weapon != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.world.isRemote) {
			return;
		}

		this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());

		if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.35f) {
			this.enrage();
		}

		EntityLivingBase target = this.getAttackTarget();
		if (target != null && target.isEntityAlive()) {
			if (--this.summonTimer <= 0) {
				this.summonTimer = this.enraged ? SUMMON_INTERVAL / 2 : SUMMON_INTERVAL;
				this.summonWarriors(target);
			}

			if (--this.slamTimer <= 0 && this.getDistanceSq(target) < SLAM_RANGE * SLAM_RANGE) {
				this.slamTimer = this.enraged ? SLAM_INTERVAL / 2 : SLAM_INTERVAL;
				this.groundSlam();
			}
		}
	}

	protected void enrage() {
		this.enraged = true;
		this.applyRageModifier();
		this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * 60 * 10, 1));
		this.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 10, 1));
		this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 3.0F, 0.6F);
		BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.warlord.rage").setStyle(new Style().setColor(TextFormatting.DARK_RED)), 48.0D);
	}

	protected void applyRageModifier() {
		IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		if (speed.getModifier(RAGE_MODIFIER_UUID) == null) {
			speed.applyModifier(new AttributeModifier(RAGE_MODIFIER_UUID, RAGE_MODIFIER_NAME, 0.35D, 1));
		}
	}

	protected void summonWarriors(EntityLivingBase target) {
		List<SuperMutantBasic> nearby = this.world.getEntitiesWithinAABB(SuperMutantBasic.class, this.getEntityBoundingBox().grow(32.0D));
		//nearby includes the warlord itself
		int count = Math.min(2, MAX_MINIONS + 1 - nearby.size());
		boolean spawned = false;
		for (int i = 0; i < count; i++) {
			SuperMutantBasic minion = this.rand.nextInt(3) == 0 ? new SuperMutantBasic(this.world) : new MutantWarrior(this.world);
			if (BossHelper.spawnMinionNear(this, minion, target, 6)) {
				spawned = true;
			}
		}
		if (spawned) {
			this.playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2.0F, 1.0F);
			BossHelper.notifyNearbyPlayers(this, new TextComponentTranslation("techguns.message.warlord.summon").setStyle(new Style().setColor(TextFormatting.GOLD)), 48.0D);
		}
	}

	protected void groundSlam() {
		this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 0.6F);
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX, this.posY + 0.5D, this.posZ, 8, SLAM_RANGE * 0.5D, 0.3D, SLAM_RANGE * 0.5D, 0.0D);
		((WorldServer) this.world).spawnParticle(EnumParticleTypes.CLOUD, this.posX, this.posY + 0.2D, this.posZ, 60, SLAM_RANGE * 0.6D, 0.1D, SLAM_RANGE * 0.6D, 0.05D);

		List<EntityLivingBase> entities = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(SLAM_RANGE, 2.0D, SLAM_RANGE));
		for (EntityLivingBase ent : entities) {
			if (ent == this || ent instanceof SuperMutantBasic) {
				continue;
			}
			double dx = ent.posX - this.posX;
			double dz = ent.posZ - this.posZ;
			double dist = MathHelper.sqrt(dx * dx + dz * dz);
			if (dist > SLAM_RANGE) {
				continue;
			}
			float damage = (float) (this.enraged ? 16.0D : 11.0D) * (float) (1.0D - (dist / (SLAM_RANGE * 1.5D)));
			if (ent.attackEntityFrom(DamageSource.causeMobDamage(this), damage)) {
				if (dist < 0.1D) {
					dx = this.rand.nextDouble() - 0.5D;
					dz = this.rand.nextDouble() - 0.5D;
					dist = MathHelper.sqrt(dx * dx + dz * dz);
				}
				ent.knockBack(this, 1.5F, -dx / dist, -dz / dist);
				ent.motionY += 0.6D;
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
		compound.setBoolean("enraged", this.enraged);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.enraged = compound.getBoolean("enraged");
		if (this.enraged) {
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
