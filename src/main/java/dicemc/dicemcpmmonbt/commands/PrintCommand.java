package dicemc.dicemcpmmonbt.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;

public class PrintCommand implements Command<CommandSourceStack>{
	private static final PrintCommand CMD = new PrintCommand();
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nbtprint").executes(CMD));
	}

	@Override
	public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String nbt = player.getMainHandItem().getTag().toString();
		if (nbt == null) nbt = "{}";
		player.sendMessage(new TextComponent(nbt), player.getUUID());
		return 0;
	}

}
