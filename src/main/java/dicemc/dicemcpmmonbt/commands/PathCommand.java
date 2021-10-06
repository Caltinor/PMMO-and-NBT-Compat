package dicemc.dicemcpmmonbt.commands;

import java.util.List;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dicemc.dicemcpmmonbt.readers.PathReader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;

public class PathCommand implements Command<CommandSourceStack>{
	public static final PathCommand CMD = new PathCommand();
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nbtpath")
				.then(Commands.argument("path", StringArgumentType.greedyString())
						.executes(CMD)));
	}

	@Override
	public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String path = StringArgumentType.getString(context, "path");
		CompoundTag nbt = context.getSource().getPlayerOrException().getMainHandItem().getTag();
		if (nbt.isEmpty()) return 0;
		List<String> output = PathReader.getNBTValues(path, nbt);
		for (int i = 0; i < output.size(); i++) {
			context.getSource().sendSuccess(new TextComponent(output.get(i)), false);
		}
		return 0;
	}
}
