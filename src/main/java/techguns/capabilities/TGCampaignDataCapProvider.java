package techguns.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import techguns.Techguns;

public class TGCampaignDataCapProvider implements ICapabilitySerializable<NBTBase> {

	@CapabilityInject(TGCampaignData.class)
	public static final Capability<TGCampaignData> TG_CAMPAIGN_DATA = null;

	public static final ResourceLocation ID = new ResourceLocation(Techguns.MODID, "campaignData");

	private final TGCampaignData instance = new TGCampaignData();

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == TG_CAMPAIGN_DATA;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == TG_CAMPAIGN_DATA ? TG_CAMPAIGN_DATA.<T>cast(this.instance) : null;
	}

	@Override
	public NBTBase serializeNBT() {
		return TG_CAMPAIGN_DATA.getStorage().writeNBT(TG_CAMPAIGN_DATA, this.instance, null);
	}

	@Override
	public void deserializeNBT(NBTBase nbt) {
		TG_CAMPAIGN_DATA.getStorage().readNBT(TG_CAMPAIGN_DATA, this.instance, null, nbt);
	}
}
