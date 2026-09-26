package techguns.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class TGCampaignDataStorage implements IStorage<TGCampaignData> {

	@Override
	public NBTBase writeNBT(Capability<TGCampaignData> capability, TGCampaignData instance, EnumFacing side) {
		NBTTagCompound tags = new NBTTagCompound();
		instance.writeToNBT(tags);
		return tags;
	}

	@Override
	public void readNBT(Capability<TGCampaignData> capability, TGCampaignData instance, EnumFacing side, NBTBase nbt) {
		if (nbt instanceof NBTTagCompound) {
			instance.readFromNBT((NBTTagCompound) nbt);
		}
	}
}
