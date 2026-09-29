package techguns.campaign;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * World saved data of the story campaign. Stores for every player the current mission objective
 * point, whether the objective structure was already placed, and the position of the player's
 * command post (where the commander NPC lives).
 */
public class CampaignWorldData extends WorldSavedData {

	public static final String DATA_NAME = "techguns_campaign";

	/** entries are stored as compounds keyed by the player uuid */
	protected NBTTagCompound players = new NBTTagCompound();

	public CampaignWorldData() {
		super(DATA_NAME);
	}

	public CampaignWorldData(String name) {
		super(name);
	}

	public static CampaignWorldData get(World world) {
		MapStorage storage = world.getPerWorldStorage();
		CampaignWorldData data = (CampaignWorldData) storage.getOrLoadData(CampaignWorldData.class, DATA_NAME);
		if (data == null) {
			data = new CampaignWorldData();
			storage.setData(DATA_NAME, data);
		}
		return data;
	}

	protected NBTTagCompound getEntry(EntityPlayer player) {
		UUID id = player.getUniqueID();
		if (!this.players.hasKey(id.toString())) {
			this.players.setTag(id.toString(), new NBTTagCompound());
		}
		return this.players.getCompoundTag(id.toString());
	}

	public void setObjective(EntityPlayer player, int mission, BlockPos pos) {
		NBTTagCompound entry = this.getEntry(player);
		entry.setInteger("mission", mission);
		entry.setInteger("x", pos.getX());
		entry.setInteger("y", pos.getY());
		entry.setInteger("z", pos.getZ());
		entry.setBoolean("placed", false);
		this.markDirty();
	}

	public boolean hasObjective(EntityPlayer player) {
		return this.getEntry(player).hasKey("mission");
	}

	public int getObjectiveMission(EntityPlayer player) {
		return this.getEntry(player).getInteger("mission");
	}

	public BlockPos getObjectivePos(EntityPlayer player) {
		NBTTagCompound entry = this.getEntry(player);
		return new BlockPos(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z"));
	}

	public boolean isObjectivePlaced(EntityPlayer player) {
		return this.getEntry(player).getBoolean("placed");
	}

	public void setObjectivePlaced(EntityPlayer player, BlockPos actualPos) {
		NBTTagCompound entry = this.getEntry(player);
		entry.setBoolean("placed", true);
		entry.setInteger("x", actualPos.getX());
		entry.setInteger("y", actualPos.getY());
		entry.setInteger("z", actualPos.getZ());
		this.markDirty();
	}

	public void clearObjective(EntityPlayer player) {
		NBTTagCompound entry = this.getEntry(player);
		entry.removeTag("mission");
		entry.removeTag("placed");
		this.markDirty();
	}

	public boolean hasCommandPost(EntityPlayer player) {
		return this.getEntry(player).hasKey("postX");
	}

	public BlockPos getCommandPost(EntityPlayer player) {
		NBTTagCompound entry = this.getEntry(player);
		return new BlockPos(entry.getInteger("postX"), entry.getInteger("postY"), entry.getInteger("postZ"));
	}

	public void setCommandPost(EntityPlayer player, BlockPos pos) {
		NBTTagCompound entry = this.getEntry(player);
		entry.setInteger("postX", pos.getX());
		entry.setInteger("postY", pos.getY());
		entry.setInteger("postZ", pos.getZ());
		this.markDirty();
	}

	public void clearAll(EntityPlayer player) {
		this.players.removeTag(player.getUniqueID().toString());
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.players = nbt.getCompoundTag("players");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("players", this.players);
		return compound;
	}
}
