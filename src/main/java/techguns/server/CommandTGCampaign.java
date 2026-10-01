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
import net.minecraft.util.text.TextComponentTranslation;
import techguns.Techguns;
import techguns.campaign.CampaignFinale;
import techguns.campaign.CampaignMission;
import techguns.campaign.CampaignMissions;
import techguns.campaign.TGCampaign;
import techguns.capabilities.TGCampaignData;

/**
 * Operator command to test the story campaign:
 * /tgcampaign set <1-30> jumps to a mission, /tgcampaign complete finishes the current mission,
 * /tgcampaign reset restarts, /tgcampaign info shows the state, /tgcampaign ending plays the
 * ending (launch of the Purifier, restored world, settlers, epilogue) without the rewards.
 */
public class CommandTGCampaign extends CommandBase {

	protected static final String[] OPTIONS = {"set", "complete", "reset", "info", "ending"};

	@Override
	public String getName() {
		return "tgcampaign";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/tgcampaign <set <1-" + TGCampaignData.LAST_MISSION + ">|complete|reset|info|ending>";
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
		case "set": {
			if (args.length < 2) {
				throw new WrongUsageException(this.getUsage(sender));
			}
			int mission = parseInt(args[1], TGCampaignData.FIRST_MISSION, TGCampaignData.LAST_MISSION);
			TGCampaign.setMission(player, mission);
			CampaignMission m = CampaignMissions.byId(mission);
			sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.set", mission, new TextComponentTranslation(m.getTitleKey())));
			if (!m.playable) {
				sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.planned", CampaignMissions.lastPlayable()));
			}
			break;
		}
		case "complete": {
			int before = data.getMission();
			if (TGCampaign.completeCurrent(player)) {
				sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.completed", before));
			} else {
				sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.nothing"));
			}
			break;
		}
		case "ending":
			if (CampaignFinale.isRunning(player)) {
				throw new CommandException(Techguns.MODID + ".campaign.cmd.ending_running");
			}
			CampaignFinale.start(player);
			sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.ending"));
			break;
		case "reset":
			TGCampaign.reset(player);
			sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.cmd.reset"));
			break;
		case "info": {
			CampaignMission m = CampaignMissions.byId(data.getMission());
			StringBuilder sb = new StringBuilder();
			sb.append("Mission ").append(data.getMission()).append("/").append(TGCampaignData.LAST_MISSION);
			if (m != null) {
				sb.append(" (act ").append(m.act).append(", ").append(m.type.name()).append(m.playable ? "" : ", planned").append(")");
			}
			sb.append(" state=").append(data.getState()).append(" progress=").append(data.getProgress()).append("/").append(data.getProgress2());
			if (data.hasObjective()) {
				BlockPos o = data.getObjective();
				sb.append(" objective=").append(o.getX()).append(",").append(o.getY()).append(",").append(o.getZ());
			}
			if (data.isFinished()) {
				sb.append(" [FINISHED]");
			}
			sender.sendMessage(new TextComponentString(sb.toString()));
			break;
		}
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
