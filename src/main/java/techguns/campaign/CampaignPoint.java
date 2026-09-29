package techguns.campaign;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * One objective point of the current mission: where its structure stands (or will be built),
 * whether it is built / manned / done and its points of interest.
 */
public class CampaignPoint {

	/** center of the site, y is a placeholder until the structure is placed, then the ground level */
	public BlockPos pos;
	public CampaignSite site;
	/** the structure is built */
	public boolean placed = false;
	/** the mission content (enemies, captives, boss, targets) is spawned */
	public boolean armed = false;
	/** stash searched, antenna repaired... */
	public boolean done = false;
	/** special position: stash chest, top of the mast, boss / captive spawn */
	@Nullable
	public BlockPos poi = null;
	/** reach objectives: the player has to be at or below this height, MIN_VALUE = any height */
	public int reachY = Integer.MIN_VALUE;
	/** blocks that have to be destroyed */
	public final List<BlockPos> targets = new ArrayList<>();
	/** area of the built structure, to build next to it later */
	public int minX, minZ, maxX, maxZ;

	public CampaignPoint(BlockPos pos, CampaignSite site) {
		this.pos = pos;
		this.site = site;
		this.minX = this.maxX = pos.getX();
		this.minZ = this.maxZ = pos.getZ();
	}

	public void setArea(int minX, int minZ, int maxX, int maxZ) {
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}

	public CampaignPoint copy() {
		return fromNBT(this.toNBT());
	}

	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		writePos(tag, "", this.pos);
		tag.setString("site", this.site.name());
		tag.setBoolean("placed", this.placed);
		tag.setBoolean("armed", this.armed);
		tag.setBoolean("done", this.done);
		if (this.poi != null) {
			writePos(tag, "poi", this.poi);
		}
		tag.setInteger("reachY", this.reachY);
		tag.setIntArray("area", new int[] { this.minX, this.minZ, this.maxX, this.maxZ });
		NBTTagList list = new NBTTagList();
		for (BlockPos t : this.targets) {
			NBTTagCompound tt = new NBTTagCompound();
			writePos(tt, "", t);
			list.appendTag(tt);
		}
		tag.setTag("targets", list);
		return tag;
	}

	public static CampaignPoint fromNBT(NBTTagCompound tag) {
		CampaignPoint p = new CampaignPoint(readPos(tag, ""), CampaignSite.byName(tag.getString("site")));
		p.placed = tag.getBoolean("placed");
		p.armed = tag.getBoolean("armed");
		p.done = tag.getBoolean("done");
		if (tag.hasKey("poiX")) {
			p.poi = readPos(tag, "poi");
		}
		p.reachY = tag.hasKey("reachY") ? tag.getInteger("reachY") : Integer.MIN_VALUE;
		int[] area = tag.getIntArray("area");
		if (area.length == 4) {
			p.setArea(area[0], area[1], area[2], area[3]);
		}
		NBTTagList list = tag.getTagList("targets", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++) {
			p.targets.add(readPos(list.getCompoundTagAt(i), ""));
		}
		return p;
	}

	protected static void writePos(NBTTagCompound tag, String prefix, BlockPos pos) {
		tag.setInteger(prefix + "X", pos.getX());
		tag.setInteger(prefix + "Y", pos.getY());
		tag.setInteger(prefix + "Z", pos.getZ());
	}

	protected static BlockPos readPos(NBTTagCompound tag, String prefix) {
		return new BlockPos(tag.getInteger(prefix + "X"), tag.getInteger(prefix + "Y"), tag.getInteger(prefix + "Z"));
	}
}
