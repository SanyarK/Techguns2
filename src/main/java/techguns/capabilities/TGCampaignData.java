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
	/** number of missions of the story, see techguns.campaign.CampaignMissions */
	public static final int LAST_MISSION = 30;

	/** version of the saved data: 1 = the old 10 mission campaign (no version tag), 2 = the 30 mission story */
	public static final int DATA_VERSION = 2;

	/** mission states */
	public static final byte STATE_OFFERED = 0;
	public static final byte STATE_ACTIVE = 1;
	public static final byte STATE_READY = 2;

	/** current mission number, 1..30; LAST_MISSION+1 = campaign finished */
	protected int mission = FIRST_MISSION;
	protected byte state = STATE_OFFERED;
	/** kill counter / generic progress of the current mission */
	protected int progress = 0;
	/** second counter, e.g. garrison kills of a destroy mission or the wave of a defense */
	protected int progress2 = 0;

	/** current objective marker for the GPS navigator */
	protected boolean hasObjective = false;
	protected int objectiveX;
	protected int objectiveY;
	protected int objectiveZ;

	protected boolean radioGiven = false;

	/** old campaign mission this data was converted from, 0 = none; server only, cleared after the notice */
	protected int migratedFrom = 0;

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

	public int getProgress2() {
		return this.progress2;
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

	public int getMigratedFrom() {
		return this.migratedFrom;
	}

	public void clearMigrated() {
		this.migratedFrom = 0;
	}

	public void setMission(int mission, byte state) {
		this.mission = mission;
		this.state = state;
		this.progress = 0;
		this.progress2 = 0;
	}

	public void setState(byte state) {
		this.state = state;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setProgress2(int progress2) {
		this.progress2 = progress2;
	}

	/**
	 * @return true when the marker changed
	 */
	public boolean setObjective(BlockPos pos) {
		boolean changed = !this.hasObjective || this.objectiveX != pos.getX() || this.objectiveY != pos.getY() || this.objectiveZ != pos.getZ();
		this.hasObjective = true;
		this.objectiveX = pos.getX();
		this.objectiveY = pos.getY();
		this.objectiveZ = pos.getZ();
		return changed;
	}

	/**
	 * @return true when there was a marker
	 */
	public boolean clearObjective() {
		boolean had = this.hasObjective;
		this.hasObjective = false;
		return had;
	}

	public void copyFrom(TGCampaignData other) {
		NBTTagCompound tags = new NBTTagCompound();
		other.writeToNBT(tags);
		this.readFromNBT(tags);
		this.migratedFrom = other.migratedFrom;
	}

	public void writeToNBT(NBTTagCompound tags) {
		tags.setInteger("version", DATA_VERSION);
		tags.setInteger("mission", this.mission);
		tags.setByte("state", this.state);
		tags.setInteger("progress", this.progress);
		tags.setInteger("progress2", this.progress2);
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
		this.progress2 = tags.getInteger("progress2");
		this.hasObjective = tags.getBoolean("hasObjective");
		this.objectiveX = tags.getInteger("objX");
		this.objectiveY = tags.getInteger("objY");
		this.objectiveZ = tags.getInteger("objZ");
		this.radioGiven = tags.getBoolean("radioGiven");

		//progress of the old 10 mission campaign: continue at the matching mission of the new story
		if (!tags.hasKey("version") && tags.hasKey("mission")) {
			int old = this.mission;
			this.mission = convertOldMission(old);
			this.state = STATE_OFFERED;
			this.progress = 0;
			this.progress2 = 0;
			this.hasObjective = false;
			this.migratedFrom = old > FIRST_MISSION ? old : 0;
		}
	}

	/**
	 * old campaign: 1 contact, 2 baptism, 3 airfield recon, 4 clear skies, 5 rescue, 6 General,
	 * 7 find the lab, 8 sample, 9 Prototype, 10 ceremony, 11 finished
	 */
	public static int convertOldMission(int old) {
		switch (old) {
		case 1:
			return 1;
		case 2:
			return 7;
		case 3:
			return 9;
		case 4:
			return 10;
		case 5:
			return 11;
		case 6:
			return 16;
		default:
			//the General is dead, continue with the rest of act III
			return old > 6 ? 17 : FIRST_MISSION;
		}
	}
}
