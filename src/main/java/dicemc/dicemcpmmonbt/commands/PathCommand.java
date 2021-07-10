package dicemc.dicemcpmmonbt.commands;

import java.util.List;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dicemc.dicemcpmmonbt.readers.PathReader;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;

public class PathCommand implements Command<CommandSource>{
	public static final PathCommand CMD = new PathCommand();
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("nbtpath")
				.then(Commands.argument("path", StringArgumentType.greedyString())
						.executes(CMD)));
	}

	@Override
	public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
		String path = StringArgumentType.getString(context, "path");
		CompoundNBT nbt = context.getSource().getPlayerOrException().getMainHandItem().getTag();
		if (nbt.isEmpty()) return 0;
		List<String> output = PathReader.getNBTValues(path, nbt);
		for (int i = 0; i < output.size(); i++) {
			context.getSource().sendSuccess(new StringTextComponent(output.get(i)), false);
		}
		return 0;
	}
}
