package techguns.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import techguns.TGPackets;
import techguns.campaign.TGCampaign;

/**
 * A button press in the commander dialog (accept mission / turn in mission)
 */
public class PacketCampaignAction implements IMessage {

	protected byte action;

	public PacketCampaignAction() {
	}

	public PacketCampaignAction(int action) {
		this.action = (byte) action;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.action = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(this.action);
	}

	public static class Handler extends HandlerTemplate<PacketCampaignAction> {

		@Override
		protected void handle(PacketCampaignAction message, MessageContext ctx) {
			EntityPlayer player = TGPackets.getPlayerFromContext(ctx);
			if (player instanceof EntityPlayerMP) {
				TGCampaign.handleDialogAction((EntityPlayerMP) player, message.action);
			}
		}
	}
}
