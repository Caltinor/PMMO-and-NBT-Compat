package dicemc.dicemcpmmonbt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.readers.EvaluationHandler;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.skills.Skill;
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
			if (Skill.getLevel(vals.getKey(), player) < vals.getValue()) {
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
			if (Skill.getLevel(vals.getKey(), player) < vals.getValue()) {
				return false;
			}
		}
		return true;
	}
	
	public static Map<String, Double> getNBTReqs(JType jType, ItemStack stack) {
		Map<String, Double> map = new HashMap<>();
		CompoundNBT nbt = stack.getTag();
		if (nbt == null) return map;
		JsonObject ref = src.get(jType).getOrDefault(stack.getItem().getRegistryName(), new JsonObject());
		if (jType.equals(JType.REQ_BREAK) && ref.get("item") != null) 
			ref = ref.get("item").getAsJsonObject();		
		if (ref.get("values") == null) return map;
		JsonArray values = ref.get("values").getAsJsonArray();		
		if (ref.get("summative") == null) return map;
		boolean isSummative = ref.get("summative").getAsBoolean();
		List<Result> data = EvaluationHandler.evaluateEntries(values, nbt);
		for (Result r : data) {
			if (r == null) continue;
			if (!r.compares()) continue;
			Map<String, Double> value = r.values;					
			for (Map.Entry<String, Double> val : value.entrySet()) {
				map.merge(val.getKey(), val.getValue(), (in1, in2) -> {
					return isSummative ? (in1 + in2)
							: (in1 > in2 ? in1 : in2);});
			}
		}
		return map;
	}
	
	public static Map<String, Double> getNBTReqs(JType jType, TileEntity tile) {
		CompoundNBT nbt = tile.serializeNBT();
		JsonObject ref = src.get(jType).getOrDefault(tile.getBlockState().getBlock().getRegistryName(), new JsonObject());
		if (jType.equals(JType.REQ_BREAK) && ref.get("tile") != null)
			ref = ref.get("tile").getAsJsonObject();
		Map<String, Double> map = new HashMap<>();
		if (ref.get("values") == null)
			return map;
		JsonArray values = ref.get("values").getAsJsonArray();		
		if (ref.get("summative") == null)
			return map;
		boolean isSummative = ref.get("summative").getAsBoolean();
		List<Result> data = EvaluationHandler.evaluateEntries(values, nbt);
		for (Result r : data) {
			if (r == null) continue;
			if (!r.compares()) continue;
			Map<String, Double> value = r.values;					
			for (Map.Entry<String, Double> val : value.entrySet()) {
				map.merge(val.getKey(), val.getValue(), (in1, in2) -> {
					return isSummative ? (in1 + in2)
							: (in1 > in2 ? in1 : in2);});
			}
		}
		return map;
	}
	
	public static void printSrc(Logger LOG) {
		for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> s : src.entrySet()) {
			LOG.info("JType="+s.getKey().toString());
			for (Map.Entry<ResourceLocation, JsonObject> i : s.getValue().entrySet()) {
				LOG.info("   "+i.getKey().toString()+":"+i.getValue().toString().substring(0, i.getValue().toString().length() > 2000 ? 2000 : i.getValue().toString().length()));
			}
		}
	}
}
