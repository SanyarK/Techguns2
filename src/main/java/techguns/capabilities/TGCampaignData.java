package techguns.capabilities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Player capability that stores the story campaign progress.
 * Lives on the server, a copy is synced to the owning client for the GPS navigator and the mission journal.
 */
public class TGCampaignData {

	public static final int FIRST_MISSION = 1;
	public static final int LAST_MISSION = 10;

	/** mission states */
	public static final byte STATE_OFFERED = 0;
	public static final byte STATE_ACTIVE = 1;
	public static final byte STATE_READY = 2;

	/** current mission number, 1..10; LAST_MISSION+1 = campaign finished */
	protected int mission = FIRST_MISSION;
	protected byte state = STATE_OFFERED;
	/** kill counter / generic progress of the current mission */
	protected int progress = 0;

	/** current objective marker for the GPS navigator */
	protected boolean hasObjective = false;
	protected int objectiveX;
	protected int objectiveY;
	protected int objectiveZ;

	protected boolean radioGiven = false;

	public static TGCampaignData get(EntityPlayer player) {
		return player.getCapability(TGCampaignDataCapProvider.TG_CAMPAIGN_DATA, null);
	}

	public int getMission() {
		return this.mission;
	}

	public byte getState() {
		return this.state;
	}

	public int getProgress() {
		return this.progress;
	}

	public boolean isFinished() {
		return this.mission > LAST_MISSION;
	}

	public boolean hasObjective() {
		return this.hasObjective;
	}

	public BlockPos getObjective() {
		return new BlockPos(this.objectiveX, this.objectiveY, this.objectiveZ);
	}

	public boolean isRadioGiven() {
		return this.radioGiven;
	}

	public void setRadioGiven(boolean given) {
		this.radioGiven = given;
	}

	public void setMission(int mission, byte state) {
		this.mission = mission;
		this.state = state;
		this.progress = 0;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setObjective(BlockPos pos) {
		this.hasObjective = true;
		this.objectiveX = pos.getX();
		this.objectiveY = pos.getY();
		this.objectiveZ = pos.getZ();
	}

	public void clearObjective() {
		this.hasObjective = false;
	}

	public void copyFrom(TGCampaignData other) {
		NBTTagCompound tags = new NBTTagCompound();
		other.writeToNBT(tags);
		this.readFromNBT(tags);
	}

	public void writeToNBT(NBTTagCompound tags) {
		tags.setInteger("mission", this.mission);
		tags.setByte("state", this.state);
		tags.setInteger("progress", this.progress);
		tags.setBoolean("hasObjective", this.hasObjective);
		tags.setInteger("objX", this.objectiveX);
		tags.setInteger("objY", this.objectiveY);
		tags.setInteger("objZ", this.objectiveZ);
		tags.setBoolean("radioGiven", this.radioGiven);
	}

	public void readFromNBT(NBTTagCompound tags) {
		this.mission = tags.getInteger("mission");
		if (this.mission < FIRST_MISSION) {
			this.mission = FIRST_MISSION;
		}
		this.state = tags.getByte("state");
		this.progress = tags.getInteger("progress");
		this.hasObjective = tags.getBoolean("hasObjective");
		this.objectiveX = tags.getInteger("objX");
		this.objectiveY = tags.getInteger("objY");
		this.objectiveZ = tags.getInteger("objZ");
		this.radioGiven = tags.getBoolean("radioGiven");
	}
}
