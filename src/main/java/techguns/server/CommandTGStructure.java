package techguns.server;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import techguns.events.MilitaryExpansionEventHandler;
import techguns.world.structures.Airfield;
import techguns.world.structures.BunkerComplex;
import techguns.world.structures.CommandPost;
import techguns.world.structures.MutagenLabStructure;
import techguns.world.structures.MutantLair;
import techguns.world.structures.UndergroundMilitaryMine;
import techguns.world.structures.WorldgenStructure;
import techguns.world.structures.WorldgenStructure.BiomeColorType;

/**
 * Operator command that places a military expansion structure around the player or starts an air raid.
 * Useful to test the structures without searching for them.
 */
public class CommandTGStructure extends CommandBase {

	protected static final String[] OPTIONS = {"underground_mine", "airfield", "bunker", "mutant_lair", "mutagen_lab", "command_post", "airraid"};

	@Override
	public String getName() {
		return "tgstructure";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/tgstructure <underground_mine|airfield|bunker|mutant_lair|mutagen_lab|command_post|airraid>";
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
		World world = sender.getEntityWorld();
		BlockPos pos = sender.getPosition();
		Random rnd = new Random();

		if ("airraid".equals(args[0])) {
			EntityPlayer player = getCommandSenderAsPlayer(sender);
			if (MilitaryExpansionEventHandler.startAirRaid(world, player)) {
				sender.sendMessage(new TextComponentString("Air raid started."));
			} else {
				sender.sendMessage(new TextComponentString("Could not spawn the jet, try again or increase the render distance."));
			}
			return;
		}

		WorldgenStructure structure;
		switch (args[0]) {
		case "underground_mine":
			structure = new UndergroundMilitaryMine();
			break;
		case "airfield":
			structure = new Airfield();
			break;
		case "bunker":
			structure = new BunkerComplex();
			break;
		case "mutant_lair":
			structure = new MutantLair();
			break;
		case "mutagen_lab":
			structure = new MutagenLabStructure();
			break;
		case "command_post":
			structure = new CommandPost();
			break;
		default:
			throw new WrongUsageException(this.getUsage(sender));
		}

		int sizeX = structure.getSizeX(rnd);
		int sizeZ = structure.getSizeZ(rnd);
		int x = pos.getX() - sizeX / 2;
		int y = pos.getY() - 1;
		int z = pos.getZ() - sizeZ / 2;

		structure.setBlocks(world, x, y, z, sizeX, structure.getSizeY(rnd), sizeZ, 0, BiomeColorType.WOODLAND, rnd);
		sender.sendMessage(new TextComponentString("Placed " + args[0] + " at " + x + " " + y + " " + z + "."));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, OPTIONS);
		}
		return Collections.emptyList();
	}
}
