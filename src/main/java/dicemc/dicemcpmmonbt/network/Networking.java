package dicemc.dicemcpmmonbt.network;

import dicemc.dicemcpmmonbt.PMMONBT;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

public class Networking {
	private static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(PMMONBT.MOD_ID, "net"),
		() -> "1.0", 
		s -> true, 
		s -> true);
	
    public static void registerMessages() { 
        int ID = 0;
        
        INSTANCE.messageBuilder(PacketSync.class, ID++)
            .encoder(PacketSync::toBytes) 
            .decoder(PacketSync::new)
            .consumer(PacketSync::handle)
            .add();
    }
    
    public static void sendToClient(Object packet, ServerPlayer player) {
		INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
	}
	public static void sendToServer(Object packet) {
		INSTANCE.sendToServer(packet);
	}
} 