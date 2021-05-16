package dicemc.dicemcpmmonbt.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PrintCommand implements Command<CommandSource>{
	private static final PrintCommand CMD = new PrintCommand();
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("nbtprint").executes(CMD));
	}

	@Override
	public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
		//TODO improve entity tracing
		ServerPlayerEntity player = context.getSource().getPlayerOrException();
		if (player.getMainHandItem().isEmpty()) {
			BlockRayTraceResult brtr = (BlockRayTraceResult) player.pick(10, 0f , false);
			EntityRayTraceResult ertr = ProjectileHelper.getEntityHitResult(
					context.getSource().getEntityOrException().getCommandSenderWorld(), 
					player, 
					player.getEyePosition(1), 
					player.getViewVector(1), 
					new AxisAlignedBB(player.getEyePosition(1), player.getViewVector(1)), 
					(p) -> {return true;});
			TileEntity te = context.getSource().getEntityOrException().getCommandSenderWorld().getBlockEntity(brtr.getBlockPos());
			if (te != null) {
				CompoundNBT nbt = te.serializeNBT();
				player.sendMessage(new StringTextComponent(te.getBlockState().getBlock().getRegistryName().toString()+" NBT="+nbt.toString()), player.getUUID());
			}
			if (ertr != null)
				player.sendMessage(new StringTextComponent(ertr.getEntity().getEncodeId()+" NBT="+ertr.getEntity().getTags().toString()), player.getUUID());
			if (ertr == null && te == null)
				player.sendMessage(new TranslationTextComponent("pmmonbt.commands.print.error"), player.getUUID());
		}
		String nbt = player.getMainHandItem().getTag().toString();
		if (nbt == null) nbt = "{}";
		player.sendMessage(new StringTextComponent(nbt), player.getUUID());
		return 0;
	}

}
