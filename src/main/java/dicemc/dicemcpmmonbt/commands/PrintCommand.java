package dicemc.dicemcpmmonbt.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public class PrintCommand implements Command<CommandSource>{
	private static final PrintCommand CMD = new PrintCommand();
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("nbtprint").executes(CMD));
	}

	@Override
	public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrException();
		String nbt = player.getMainHandItem().getTag().toString();
		if (nbt == null) nbt = "{}";
		player.sendMessage(new StringTextComponent(nbt), player.getUUID());
		return 0;
	}

}
