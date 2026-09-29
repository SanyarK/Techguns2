package techguns.campaign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * World saved data of the story campaign (overworld). Stores for every player the objective points
 * of the current mission, the home (start bunker), the command post and remembered sites
 * (e.g. the airfield scouted in act II that is attacked at the end of act III).
 */
public class CampaignWorldData extends WorldSavedData {

	public static final String DATA_NAME = "techguns_campaign";

	/** entries are stored as compounds keyed by the player uuid */
	protected NBTTagCompound players = new NBTTagCompound();

	/** parsed objective points of the players, written back with {@link #savePoints(EntityPlayer)} */
	protected final Map<UUID, List<CampaignPoint>> pointCache = new HashMap<>();

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
		String id = player.getUniqueID().toString();
		if (!this.players.hasKey(id)) {
			this.players.setTag(id, new NBTTagCompound());
		}
		NBTTagCompound entry = this.players.getCompoundTag(id);
		//objective of the old 10 mission campaign
		if (entry.hasKey("placed")) {
			entry.removeTag("mission");
			entry.removeTag("placed");
			entry.removeTag("x");
			entry.removeTag("y");
			entry.removeTag("z");
			this.markDirty();
		}
		return entry;
	}

	protected static BlockPos readPos(NBTTagCompound tag, String prefix) {
		return new BlockPos(tag.getInteger(prefix + "X"), tag.getInteger(prefix + "Y"), tag.getInteger(prefix + "Z"));
	}

	protected static void writePos(NBTTagCompound tag, String prefix, BlockPos pos) {
		tag.setInteger(prefix + "X", pos.getX());
		tag.setInteger(prefix + "Y", pos.getY());
		tag.setInteger(prefix + "Z", pos.getZ());
	}

	/*
	 * ------------------------------------------------- objective points of the current mission
	 */

	/**
	 * @return the mission the stored points belong to, 0 = none
	 */
	public int getPointsMission(EntityPlayer player) {
		return this.getEntry(player).getInteger("pointsMission");
	}

	/**
	 * the list is cached, change the points and call {@link #savePoints(EntityPlayer)}
	 */
	public List<CampaignPoint> getPoints(EntityPlayer player) {
		List<CampaignPoint> list = this.pointCache.get(player.getUniqueID());
		if (list == null) {
			list = new ArrayList<>();
			NBTTagList tags = this.getEntry(player).getTagList("points", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < tags.tagCount(); i++) {
				list.add(CampaignPoint.fromNBT(tags.getCompoundTagAt(i)));
			}
			this.pointCache.put(player.getUniqueID(), list);
		}
		return list;
	}

	public void setPoints(EntityPlayer player, int mission, List<CampaignPoint> points) {
		this.pointCache.put(player.getUniqueID(), new ArrayList<>(points));
		this.getEntry(player).setInteger("pointsMission", mission);
		this.savePoints(player);
	}

	public void savePoints(EntityPlayer player) {
		NBTTagList tags = new NBTTagList();
		for (CampaignPoint p : this.getPoints(player)) {
			tags.appendTag(p.toNBT());
		}
		this.getEntry(player).setTag("points", tags);
		this.markDirty();
	}

	public void clearPoints(EntityPlayer player) {
		NBTTagCompound entry = this.getEntry(player);
		entry.removeTag("points");
		entry.removeTag("pointsMission");
		this.pointCache.remove(player.getUniqueID());
		this.markDirty();
	}

	/*
	 * ------------------------------------------------- home, command post, remembered sites
	 */

	public boolean hasHome(EntityPlayer player) {
		return this.getEntry(player).hasKey("homeX");
	}

	public BlockPos getHome(EntityPlayer player) {
		return readPos(this.getEntry(player), "home");
	}

	public void setHome(EntityPlayer player, BlockPos pos) {
		writePos(this.getEntry(player), "home", pos);
		this.markDirty();
	}

	public boolean hasCommandPost(EntityPlayer player) {
		return this.getEntry(player).hasKey("postX");
	}

	/**
	 * ground level at the center of the post, the commander stands one block above
	 */
	public BlockPos getCommandPost(EntityPlayer player) {
		return readPos(this.getEntry(player), "post");
	}

	public void setCommandPost(EntityPlayer player, BlockPos pos) {
		writePos(this.getEntry(player), "post", pos);
		this.markDirty();
	}

	@Nullable
	public CampaignPoint getSite(EntityPlayer player, String key) {
		NBTTagCompound sites = this.getEntry(player).getCompoundTag("sites");
		return sites.hasKey(key) ? CampaignPoint.fromNBT(sites.getCompoundTag(key)) : null;
	}

	public void setSite(EntityPlayer player, String key, CampaignPoint point) {
		NBTTagCompound entry = this.getEntry(player);
		NBTTagCompound sites = entry.getCompoundTag("sites");
		sites.setTag(key, point.toNBT());
		entry.setTag("sites", sites);
		this.markDirty();
	}

	public void clearAll(EntityPlayer player) {
		this.players.removeTag(player.getUniqueID().toString());
		this.pointCache.remove(player.getUniqueID());
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.players = nbt.getCompoundTag("players");
		this.pointCache.clear();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("players", this.players);
		return compound;
	}
}
