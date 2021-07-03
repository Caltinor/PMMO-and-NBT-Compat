package dicemc.dicemcpmmonbt.commands;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dicemc.dicemcpmmonbt.PMMONBT;
import dicemc.dicemcpmmonbt.ReqChecker;
import dicemc.dicemcpmmonbt.network.Networking;
import dicemc.dicemcpmmonbt.network.PacketSync;
import dicemc.dicemcpmmonbt.readers.JsonParser;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;

public class ReloadCommand implements Command<CommandSource> {
	private static final ReloadCommand CMD = new ReloadCommand();
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("nbtreload")
				.requires((p) -> p.hasPermission(2))
				.executes(CMD));
	}

	@Override
	public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
		JsonParser.readRawData();
		JsonParser.parseTags(context.getSource().getServer());
		PMMONBT.registerLogic();
		List<ServerPlayerEntity> playerlist = context.getSource().getServer().getPlayerList().getPlayers();
		PacketSync packet = new PacketSync(ReqChecker.src);
		for (ServerPlayerEntity spe : playerlist) {
			Networking.sendToClient(packet , spe);
		}
		return 0;
	}
}
