package techguns.server;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import techguns.campaign.CampaignMission;
import techguns.campaign.TGCampaign;
import techguns.capabilities.TGCampaignData;

/**
 * Operator command to test the story campaign:
 * /tgcampaign set <1-10> jumps to a mission, /tgcampaign reset restarts, /tgcampaign info shows the state.
 */
public class CommandTGCampaign extends CommandBase {

	protected static final String[] OPTIONS = {"set", "reset", "info"};

	@Override
	public String getName() {
		return "tgcampaign";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/tgcampaign <set <1-" + TGCampaignData.LAST_MISSION + ">|reset|info>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			throw new WrongUsageException(this.getUsage(sender));
		}
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			throw new CommandException("No campaign data");
		}

		switch (args[0]) {
		case "set":
			if (args.length < 2) {
				throw new WrongUsageException(this.getUsage(sender));
			}
			int mission = parseInt(args[1], TGCampaignData.FIRST_MISSION, TGCampaignData.LAST_MISSION);
			TGCampaign.setMission(player, mission);
			sender.sendMessage(new TextComponentString("Campaign mission set to " + mission + ". Use the radio to accept the mission briefing."));
			break;
		case "reset":
			TGCampaign.reset(player);
			sender.sendMessage(new TextComponentString("Campaign progress reset. Use the radio to start over."));
			break;
		case "info":
			CampaignMission m = CampaignMission.byId(data.getMission());
			StringBuilder sb = new StringBuilder();
			sb.append("Mission ").append(data.getMission());
			if (m != null) {
				sb.append(" (").append(m.name()).append(")");
			}
			sb.append(" state=").append(data.getState()).append(" progress=").append(data.getProgress());
			if (data.hasObjective()) {
				BlockPos o = data.getObjective();
				sb.append(" objective=").append(o.getX()).append(",").append(o.getY()).append(",").append(o.getZ());
			}
			if (data.isFinished()) {
				sb.append(" [FINISHED]");
			}
			sender.sendMessage(new TextComponentString(sb.toString()));
			break;
		default:
			throw new WrongUsageException(this.getUsage(sender));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, OPTIONS);
		}
		return Collections.emptyList();
	}
}
