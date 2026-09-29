package techguns.server;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import techguns.Techguns;
import techguns.world.wasteland.ApocalypseWorldData;
import techguns.world.wasteland.StartBunker;
import techguns.world.wasteland.WorldTypeApocalypse;

/**
 * Operator command: builds the start bunker under the player, moves the world spawn into it
 * (unless "nospawn" is given) and teleports the player inside. Works in every world type.
 */
public class CommandTGStartBunker extends CommandBase {

	@Override
	public String getName() {
		return "tgstartbunker";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/tgstartbunker [nospawn]";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		World world = sender.getEntityWorld();
		BlockPos pos = sender.getPosition();
		boolean setSpawn = world.provider.getDimension() == 0 && !(args.length > 0 && "nospawn".equals(args[0]));

		BlockPos inside = StartBunker.build(world, pos.getX(), pos.getZ(), new Random(), false);
		if (inside == null) {
			sender.sendMessage(new TextComponentTranslation(Techguns.MODID + ".apocalypse.bunker.too_low"));
			return;
		}

		if (setSpawn) {
			world.setSpawnPoint(inside);
			ApocalypseWorldData.get(world).setBunker(inside);
			//other world types spread players around the spawn point
			if (!(world.getWorldType() instanceof WorldTypeApocalypse)) {
				world.getGameRules().setOrCreateGameRule("spawnRadius", "0");
			}
		}

		Entity entity = sender.getCommandSenderEntity();
		if (entity != null) {
			entity.setPositionAndUpdate(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
		}
		sender.sendMessage(new TextComponentTranslation(Techguns.MODID + (setSpawn ? ".apocalypse.bunker.built_spawn" : ".apocalypse.bunker.built"), inside.getX(), inside.getY(), inside.getZ()));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "nospawn");
		}
		return Collections.emptyList();
	}
}
