package techguns.packets;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import techguns.Techguns;

/**
 * Events of the campaign ending for the client: the restored world flag, the flash of the
 * Purifier launch and the epilogue screen
 */
public class PacketCampaignEvent implements IMessage {

	/** value: 1 = the world was restored by the Purifier, 0 = not */
	public static final int RESTORED = 0;
	/** value: length of the white flash in ticks */
	public static final int FLASH = 1;
	/** opens the epilogue screen */
	public static final int EPILOGUE = 2;

	protected int type;
	protected int value;

	public PacketCampaignEvent() {
	}

	public PacketCampaignEvent(int type, int value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.type = buf.readByte();
		this.value = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(this.type);
		buf.writeInt(this.value);
	}

	public static class Handler extends HandlerTemplate<PacketCampaignEvent> {

		@Override
		protected void handle(PacketCampaignEvent message, MessageContext ctx) {
			Techguns.proxy.handleCampaignEvent(message.type, message.value);
		}
	}
}
