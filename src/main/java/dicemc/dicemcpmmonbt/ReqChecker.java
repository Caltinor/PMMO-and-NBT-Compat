package dicemc.dicemcpmmonbt;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.readers.EvaluationHandler;
import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.config.JType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class ReqChecker {
	public static Map<JType, Map<ResourceLocation, JsonObject>> src = new HashMap<>();
	
	public static void setValues(JType jtype, Map<ResourceLocation, JsonObject> values) {
		src.put(jtype, values);
	}
	public static void setValues(JType jtype, ResourceLocation key, JsonObject value) {
		src.get(jtype).put(key, value);
	}
		
	public static boolean checkNBTReq(PlayerEntity player, ResourceLocation res, JType jType) {
		//failsafe code
		if (!src.containsKey(jType)) return true;
		if (!src.get(jType).containsKey(res)) return true;
		
		//core logic
		ItemStack stack = player.getMainHandItem();
		CompoundNBT tag = stack.getTag();
		if (tag == null) return true;
		//XP check returns false if any criteria do not meet, otherwise proceeds to true return
		for (Map.Entry<String, Double> vals : getNBTReqs(jType, stack).entrySet()) {
			if (APIUtils.getLevel(vals.getKey(), player) < vals.getValue()) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean checkNBTReq(PlayerEntity player, TileEntity tile, JType jType) {
		if (!src.containsKey(jType)) return true;
		if (!src.get(jType).containsKey(tile.getBlockState().getBlock().getRegistryName())) return true;

		//core logic
		CompoundNBT tag = tile.serializeNBT();
		if (tag == null) return true;
		//XP check returns false if any criteria do not meet, otherwise proceeds to true return
		for (Map.Entry<String, Double> vals : getNBTReqs(jType, tile).entrySet()) {
			if (APIUtils.getLevel(vals.getKey(), player) < vals.getValue()) {
				return false;
			}
		}
		return true;
	}
	
	public static Map<String, Double> getNBTReqs(JType jType, ItemStack stack) {
		CompoundNBT nbt = stack.getTag();
		if (nbt == null) return new HashMap<>();
		JsonObject ref = src.get(jType).getOrDefault(stack.getItem().getRegistryName(), new JsonObject());
		if (jType.equals(JType.REQ_BREAK) && ref.get("item") != null) {ref = ref.get("item").getAsJsonObject();}		
		if (ref.get("logic") == null) {return new HashMap<>();}
		JsonArray values = ref.get("logic").getAsJsonArray();
		JsonObject globals = src.get(jType).getOrDefault(new ResourceLocation("global"), new JsonObject());
		return EvaluationHandler.evaluateEntries(values, nbt, globals);
	}
	
	public static Map<String, Double> getNBTReqs(JType jType, TileEntity tile) {
		CompoundNBT nbt = tile.serializeNBT();
		JsonObject ref = src.get(jType).getOrDefault(tile.getBlockState().getBlock().getRegistryName(), new JsonObject());
		if (jType.equals(JType.REQ_BREAK) && ref.get("tile") != null) {ref = ref.get("tile").getAsJsonObject();}
		if (ref.get("logic") == null) {return new HashMap<>();}
		JsonArray values = ref.get("logic").getAsJsonArray();		
		JsonObject globals = src.get(jType).getOrDefault(new ResourceLocation("global"), new JsonObject());
		return EvaluationHandler.evaluateEntries(values, nbt, globals);
	}
	
	public static Map<String, Double> getNBTReqs(JType jType, Entity entity) {
		CompoundNBT nbt = entity.serializeNBT();
		JsonObject ref = src.get(jType).getOrDefault(entity.getType().getRegistryName(), new JsonObject());
		if (ref.get("logic") == null) {return new HashMap<>();}
		JsonArray values = ref.get("values").getAsJsonArray();			
		JsonObject globals = src.get(jType).getOrDefault(new ResourceLocation("global"), new JsonObject());
		return EvaluationHandler.evaluateEntries(values, nbt, globals);
	}
	
	public static void printSrc(Logger LOG) {
		for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> s : src.entrySet()) {
			LOG.info("JType="+s.getKey().toString());
			for (Map.Entry<ResourceLocation, JsonObject> i : s.getValue().entrySet()) {
				LOG.info("   "+i.getKey().toString()+":"+i.getValue().toString().length());
						//+":"+i.getValue().toString().substring(0, i.getValue().toString().length() > 2000 ? 2000 : i.getValue().toString().length()));
			}
		}
	}
}
