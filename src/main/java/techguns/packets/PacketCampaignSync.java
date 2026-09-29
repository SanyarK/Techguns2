package techguns.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import techguns.TGPackets;
import techguns.capabilities.TGCampaignData;

/**
 * Syncs the campaign progress capability to the owning client (GPS navigator, mission journal)
 */
public class PacketCampaignSync implements IMessage {

	protected NBTTagCompound tags;

	public PacketCampaignSync() {
	}

	public PacketCampaignSync(TGCampaignData data) {
		this.tags = new NBTTagCompound();
		data.writeToNBT(this.tags);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.tags = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, this.tags);
	}

	public static class Handler extends HandlerTemplate<PacketCampaignSync> {

		@Override
		protected void handle(PacketCampaignSync message, MessageContext ctx) {
			EntityPlayer player = TGPackets.getPlayerFromContext(ctx);
			if (player != null && message.tags != null) {
				TGCampaignData data = TGCampaignData.get(player);
				if (data != null) {
					data.readFromNBT(message.tags);
				}
			}
		}
	}
}
