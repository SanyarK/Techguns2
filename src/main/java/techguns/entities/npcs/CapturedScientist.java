package techguns.entities.npcs;

import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import techguns.TGConfig;
import techguns.api.npc.factions.TGNpcFaction;
import techguns.campaign.CampaignMission;
import techguns.campaign.TGCampaign;
import techguns.capabilities.TGCampaignData;

/**
 * Friendly NPC of the rescue mission. Held captive in a bunker; once freed he follows
 * his rescuer and has to be escorted to the commander.
 */
public class CapturedScientist extends GenericNPC {

	protected boolean following = false;
	protected UUID rescuer = null;

	public CapturedScientist(World world) {
		super(world);
		this.targetTasks.taskEntries.clear();
		this.tasks.addTask(3, new EntityAIFollowRescuer(this, 1.15D, 3.0f));
		this.experienceValue = 0;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
	}

	@Override
	public void setCombatTask() {
		//scientists don't fight
	}

	@Override
	protected void addRandomArmor(int difficulty) {
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.NEUTRAL;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	public boolean isFollowing() {
		return this.following;
	}

	public boolean isRescuer(EntityPlayer player) {
		return this.rescuer != null && this.rescuer.equals(player.getUniqueID());
	}

	public EntityPlayer getRescuerPlayer() {
		return this.rescuer == null ? null : this.world.getPlayerEntityByUUID(this.rescuer);
	}

	/**
	 * Escort finished: stop following and stay at the current position
	 */
	public void stayHere() {
		this.following = false;
		this.rescuer = null;
		this.setHomePosAndDistance(new BlockPos(this), 8);
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) {
			return false;
		}
		if (!this.world.isRemote && player instanceof EntityPlayerMP) {
			if (this.rescuer == null) {
				TGCampaignData data = TGCampaignData.get(player);
				if (TGConfig.campaignEnabled && data != null && data.getMission() == CampaignMission.RESCUE.id
						&& data.getState() == TGCampaignData.STATE_ACTIVE) {
					this.rescuer = player.getUniqueID();
					this.following = true;
					player.sendMessage(new TextComponentTranslation("techguns.campaign.msg.scientist_freed").setStyle(new Style().setColor(TextFormatting.GREEN)));
				} else {
					player.sendMessage(new TextComponentTranslation("techguns.campaign.msg.scientist_idle"));
				}
			} else if (this.isRescuer(player)) {
				this.following = !this.following;
				player.sendMessage(new TextComponentTranslation(this.following ? "techguns.campaign.msg.scientist_follow" : "techguns.campaign.msg.scientist_wait"));
			}
		}
		return true;
	}

	@Override
	public void onDeath(DamageSource cause) {
		if (!this.world.isRemote && this.rescuer != null) {
			EntityPlayer player = this.getRescuerPlayer();
			if (player instanceof EntityPlayerMP) {
				TGCampaign.onScientistDied((EntityPlayerMP) player);
			}
		}
		super.onDeath(cause);
	}

	/**
	 * see {@link CommanderNPC#setDead()}: survive peaceful difficulty
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
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setBoolean("following", this.following);
		if (this.rescuer != null) {
			compound.setUniqueId("rescuer", this.rescuer);
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.following = compound.getBoolean("following");
		if (compound.hasUniqueId("rescuer")) {
			this.rescuer = compound.getUniqueId("rescuer");
		}
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

	/**
	 * Follows the rescuing player while the scientist is in follow mode
	 */
	static class EntityAIFollowRescuer extends EntityAIBase {

		protected final CapturedScientist scientist;
		protected final double speed;
		protected final float minDist;
		protected EntityPlayer target;
		protected int timer;

		public EntityAIFollowRescuer(CapturedScientist scientist, double speed, float minDist) {
			this.scientist = scientist;
			this.speed = speed;
			this.minDist = minDist;
			this.setMutexBits(3);
		}

		@Override
		public boolean shouldExecute() {
			if (!this.scientist.isFollowing()) {
				return false;
			}
			EntityPlayer player = this.scientist.getRescuerPlayer();
			if (player == null || !player.isEntityAlive()) {
				return false;
			}
			if (this.scientist.getDistanceSq(player) < this.minDist * this.minDist) {
				return false;
			}
			this.target = player;
			return true;
		}

		@Override
		public boolean shouldContinueExecuting() {
			return this.scientist.isFollowing() && this.target != null && this.target.isEntityAlive()
					&& this.scientist.getDistanceSq(this.target) > this.minDist * this.minDist;
		}

		@Override
		public void startExecuting() {
			this.timer = 0;
		}

		@Override
		public void resetTask() {
			this.target = null;
			this.scientist.getNavigator().clearPath();
		}

		@Override
		public void updateTask() {
			this.scientist.getLookHelper().setLookPositionWithEntity(this.target, 10.0f, this.scientist.getVerticalFaceSpeed());
			if (--this.timer <= 0) {
				this.timer = 10;
				//teleport back to the player when left too far behind
				if (this.scientist.getDistanceSq(this.target) > 32.0D * 32.0D) {
					this.scientist.setLocationAndAngles(this.target.posX, this.target.posY, this.target.posZ, this.scientist.rotationYaw, this.scientist.rotationPitch);
					this.scientist.getNavigator().clearPath();
					return;
				}
				this.scientist.getNavigator().tryMoveToEntityLiving(this.target, this.speed);
			}
		}
	}
}
