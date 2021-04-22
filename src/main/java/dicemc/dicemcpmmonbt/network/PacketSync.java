package dicemc.dicemcpmmonbt.network;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.ReqChecker;
import harmonised.pmmo.config.JType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketSync {
	private final  Map<JType, Map<ResourceLocation, JsonObject>> src;
	
	public PacketSync(PacketBuffer buf) {
		Gson gson = new Gson();
		src = new HashMap<>();
		int len = buf.readInt();
		for (int i = 0; i < len; i++) {
			Map<ResourceLocation, JsonObject> valMap = new HashMap<>();
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
	
	public void toBytes(PacketBuffer buf) {
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
			ReqChecker.src = this.src;
		});
		return true;
	}
}
