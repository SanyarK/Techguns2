package techguns.entities.npcs;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import techguns.Techguns;
import techguns.api.npc.factions.TGNpcFaction;

/**
 * Peaceful settler that comes to the command post after the Purifier was launched.
 * Walks around the post, carries tools or food and thanks the hero when talked to.
 */
public class SettlerNPC extends GenericNPC {

	public static final int VARIANTS = 3;
	/** number of chat lines techguns.campaign.settler.N in the lang files */
	protected static final int LINES = 6;

	protected static final Item[] HELD = { Items.IRON_HOE, Items.BREAD, Items.WHEAT, Item.getItemFromBlock(Blocks.SAPLING), Items.WATER_BUCKET, Items.IRON_SHOVEL };

	public SettlerNPC(World world) {
		super(world);
		this.targetTasks.taskEntries.clear();
		this.tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 0.8D));
		this.experienceValue = 0;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
	}

	/** skin variant, the same on server and client */
	public int getVariant() {
		return Math.floorMod(this.getUniqueID().hashCode(), VARIANTS);
	}

	@Override
	public void setCombatTask() {
		//settlers don't fight
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		Item item = HELD[this.rand.nextInt(HELD.length)];
		if (item != null) {
			this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(item));
		}
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.NEUTRAL;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	/** wanders in the sun, not in the dark like the monsters */
	@Override
	public float getBlockPathWeight(BlockPos pos) {
		return 0.0F;
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (hand != EnumHand.MAIN_HAND) {
			return false;
		}
		if (!this.world.isRemote && player instanceof EntityPlayerMP) {
			ITextComponent name = new TextComponentTranslation(Techguns.MODID + ".campaign.settler.name");
			name.setStyle(new Style().setColor(TextFormatting.GREEN));
			ITextComponent text = new TextComponentTranslation(Techguns.MODID + ".campaign.settler." + (1 + this.rand.nextInt(LINES)), player.getName());
			player.sendMessage(name.appendSibling(new TextComponentTranslation(Techguns.MODID + ".campaign.chat.separator")).appendSibling(text));
			this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
			this.getLookHelper().setLookPositionWithEntity(player, 30.0f, 30.0f);
		}
		return true;
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

	/**
	 * the home position is not saved by minecraft, the settlers have to stay at the post
	 */
	@Override
	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		if (this.hasHome()) {
			BlockPos home = this.getHomePosition();
			compound.setIntArray("settlerHome", new int[] { home.getX(), home.getY(), home.getZ(), (int) this.getMaximumHomeDistance() });
		}
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		int[] home = compound.getIntArray("settlerHome");
		if (home.length == 4) {
			this.setHomePosAndDistance(new BlockPos(home[0], home[1], home[2]), home[3]);
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
}
