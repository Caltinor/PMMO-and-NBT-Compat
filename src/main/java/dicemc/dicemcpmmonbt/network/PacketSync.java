package dicemc.dicemcpmmonbt.network;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.PMMONBT;
import dicemc.dicemcpmmonbt.ReqChecker;
import harmonised.pmmo.config.JType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

public class PacketSync {
	private final  Map<JType, Map<ResourceLocation, JsonObject>> src;
	
	public PacketSync(FriendlyByteBuf buf) {
		Gson gson = new Gson();
		src = new ConcurrentHashMap<>();
		int len = buf.readInt();
		for (int i = 0; i < len; i++) {
			Map<ResourceLocation, JsonObject> valMap = new ConcurrentHashMap<>();
			JType type = JType.values()[buf.readInt()];
			int l = buf.readInt();
			for (int j = 0; j < l; j++) {
				int clen = buf.readInt();
				ResourceLocation res = new ResourceLocation(buf.readCharSequence(clen, Charset.defaultCharset()).toString());
				clen = buf.readInt();
				JsonObject obj = gson.fromJson(buf.readCharSequence(clen, Charset.defaultCharset()).toString(), JsonObject.class);
				valMap.put(res, obj);
			}
			src.put(type, valMap);
		}
	}
	
	public PacketSync( Map<JType, Map<ResourceLocation, JsonObject>> src) { 
		this.src = src;
	}
	
	public void toBytes(FriendlyByteBuf buf) {
		int len = src.size();
		buf.writeInt(len);
		for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> map : src.entrySet()) {
			buf.writeInt(map.getKey().ordinal());
			buf.writeInt(map.getValue().size());
			for (Map.Entry<ResourceLocation, JsonObject> val : map.getValue().entrySet()) {
				buf.writeInt(val.getKey().toString().length());
				buf.writeCharSequence(val.getKey().toString(), Charset.defaultCharset());
				buf.writeInt(val.getValue().toString().length());
				buf.writeCharSequence(val.getValue().toString(), Charset.defaultCharset());
			}
		}
	}
 	
	public boolean handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			System.out.println("Client Updated");
			ReqChecker.src.putAll(this.src);
			PMMONBT.registerLogic();
			for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> s : ReqChecker.src.entrySet()) {
				System.out.println("JType="+s.getKey().toString());
				for (Map.Entry<ResourceLocation, JsonObject> i : s.getValue().entrySet()) {
					System.out.println("   "+i.getKey().toString()+":"+i.getValue().toString().substring(0, i.getValue().toString().length() > 2000 ? 2000 : i.getValue().toString().length()));
							//+":"+i.getValue().toString().substring(0, i.getValue().toString().length() > 2000 ? 2000 : i.getValue().toString().length()));
				}
			}
		});
		return true;
	}
}
