package techguns.world.wasteland;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Remembers whether the start bunker was built, so it is only built once per world
 */
public class ApocalypseWorldData extends WorldSavedData {

	public static final String DATA_NAME = "techguns_apocalypse";

	protected boolean bunkerBuilt = false;
	protected BlockPos bunkerPos = null;

	public ApocalypseWorldData() {
		super(DATA_NAME);
	}

	public ApocalypseWorldData(String name) {
		super(name);
	}

	public static ApocalypseWorldData get(World world) {
		MapStorage storage = world.getPerWorldStorage();
		ApocalypseWorldData data = (ApocalypseWorldData) storage.getOrLoadData(ApocalypseWorldData.class, DATA_NAME);
		if (data == null) {
			data = new ApocalypseWorldData();
			storage.setData(DATA_NAME, data);
		}
		return data;
	}

	public boolean isBunkerBuilt() {
		return this.bunkerBuilt;
	}

	@Nullable
	public BlockPos getBunkerPos() {
		return this.bunkerPos;
	}

	/**
	 * @param spawn position inside the bunker, null when building failed (no second try)
	 */
	public void setBunker(@Nullable BlockPos spawn) {
		this.bunkerBuilt = true;
		this.bunkerPos = spawn;
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.bunkerBuilt = nbt.getBoolean("bunkerBuilt");
		if (nbt.hasKey("bunkerX")) {
			this.bunkerPos = new BlockPos(nbt.getInteger("bunkerX"), nbt.getInteger("bunkerY"), nbt.getInteger("bunkerZ"));
		} else {
			this.bunkerPos = null;
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("bunkerBuilt", this.bunkerBuilt);
		if (this.bunkerPos != null) {
			nbt.setInteger("bunkerX", this.bunkerPos.getX());
			nbt.setInteger("bunkerY", this.bunkerPos.getY());
			nbt.setInteger("bunkerZ", this.bunkerPos.getZ());
		}
		return nbt;
	}
}
