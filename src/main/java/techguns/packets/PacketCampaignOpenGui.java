package techguns.packets;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import techguns.Techguns;

/**
 * Tells the client to open the commander dialog screen
 */
public class PacketCampaignOpenGui implements IMessage {

	protected int mission;
	protected byte state;
	protected int progress;
	protected boolean atCommander;

	public PacketCampaignOpenGui() {
	}

	public PacketCampaignOpenGui(int mission, byte state, int progress, boolean atCommander) {
		this.mission = mission;
		this.state = state;
		this.progress = progress;
		this.atCommander = atCommander;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.mission = buf.readInt();
		this.state = buf.readByte();
		this.progress = buf.readInt();
		this.atCommander = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.mission);
		buf.writeByte(this.state);
		buf.writeInt(this.progress);
		buf.writeBoolean(this.atCommander);
	}

	public static class Handler extends HandlerTemplate<PacketCampaignOpenGui> {

		@Override
		protected void handle(PacketCampaignOpenGui message, MessageContext ctx) {
			Techguns.proxy.openCampaignGui(message.mission, message.state, message.progress, message.atCommander);
		}
	}
}
