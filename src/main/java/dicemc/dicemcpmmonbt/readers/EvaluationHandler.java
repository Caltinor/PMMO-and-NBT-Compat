package dicemc.dicemcpmmonbt.readers;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.Result;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

public class EvaluationHandler {
	
	//entry point and compoundNBT evaluator
	public static List<Result> evaluateEntries(JsonArray jarr, CompoundNBT nbt) {		
		List<Result> list = new ArrayList<>();
		
		//cancels evaluation if NBT has no data
		if (nbt.isEmpty() || nbt == null) return list;
		
		for (int i = 0; i < jarr.size(); i++) {
			JsonObject obj = jarr.get(i).getAsJsonObject();
			list.addAll(evaluateEntry(obj, nbt));
		}
		
		return list;
	}
	
	private static List<Result> evaluateEntry(JsonObject obj, CompoundNBT nbt) {
		List<Result> list = new ArrayList<>();
		
		String type = obj.getAsJsonObject().get("type").getAsString();
		JsonArray keyArr = obj.getAsJsonObject().get("keys").getAsJsonArray();
		//if the root NBT contains the desired key, process the key
		for (int k = 0; k < keyArr.size(); k++) {
			String key = keyArr.get(k).getAsString();
			if (type.equalsIgnoreCase("id") && nbt.contains(key)) {
				String nbtValue = nbt.get(key).getAsString();
				JsonArray predicates = obj.get("predicates").getAsJsonArray();
				for (int i = 0; i < predicates.size(); i++) {
					JsonObject pred = predicates.get(i).getAsJsonObject();
					String operator = pred.get("operator").getAsString();
					JsonObject values = pred.get("value").getAsJsonObject();
					String comparator = "";
					if (!operator.equalsIgnoreCase("EXISTS"))
						comparator = pred.get("comparator").getAsString();
					list.add(new Result(key, operator, comparator, values, operator.equalsIgnoreCase("EXISTS") ? key : nbtValue));
				}
			}
			//if the type is a compound, go one step deeper with recursion
			else if (type.equalsIgnoreCase("compound")){
				CompoundNBT nbtOut = key.equalsIgnoreCase("") ? nbt : nbt.getCompound(key);
				JsonArray subArray = obj.getAsJsonObject().get("sub_references").getAsJsonArray();
				for (int i = 0; i < subArray.size(); i ++) {
					list.addAll(evaluateEntry(subArray.get(i).getAsJsonObject(), nbtOut));
				}
			}
			//if the type is list process list logic
			else if (type.equalsIgnoreCase("list")) {
				list.addAll(evaluateList(obj, (ListNBT) nbt.get(key)));
			}
		}
		return list;
	}
	//list evaluator
	private static List<Result> evaluateList(JsonObject obj, ListNBT lnbt){
		List<Result> list = new ArrayList<>();
		if (lnbt == null) return list;
		
		JsonArray subArray = obj.getAsJsonObject().get("sub_references").getAsJsonArray();
		int index = obj.getAsJsonObject().get("index").getAsInt();
		//safety for invalid json entries
		if (index < -1 || index >= lnbt.size()) return list;

		if (lnbt.size() > 0) {
			for (int s = 0; s < subArray.size(); s++) {
				JsonObject subRef = subArray.get(s).getAsJsonObject();
				if (index == -1) {
					for (int i = 0; i < lnbt.size(); i++) {
						if (lnbt.get(0) instanceof CompoundNBT)
							list.addAll(evaluateEntry(subRef, lnbt.getCompound(i)));
						else if (lnbt.get(0) instanceof ListNBT)
							list.addAll(evaluateList(subRef, lnbt.getList(i)));
						else {
							JsonArray keys = subRef.get("keys").getAsJsonArray();
							for (int sk = 0; sk < keys.size(); sk++) {
								String keyEntry = keys.get(sk).getAsString();
								String nbtValue = lnbt.get(i).getAsString();
								JsonArray predicates = subRef.get("predicates").getAsJsonArray();
								for (int j = 0; j < predicates.size(); j++) {
									JsonObject pred = predicates.get(j).getAsJsonObject();
									String operator = pred.get("operator").getAsString();
									JsonObject values = pred.get("value").getAsJsonObject();
									String comparator = "";
									if (!operator.equalsIgnoreCase("EXISTS"))
										comparator = pred.get("comparator").getAsString();
									list.add(new Result(keyEntry, operator, comparator, values, operator.equalsIgnoreCase("EXISTS") ? keyEntry : nbtValue));
								}
							}
						}
					}
				}
				else {
					if (lnbt.get(0) instanceof CompoundNBT)
						list.addAll(evaluateEntry(subRef, lnbt.getCompound(index)));
					else if (lnbt.get(0) instanceof ListNBT)
						list.addAll(evaluateList(subRef, lnbt.getList(index)));
					else {
						JsonArray keys = subRef.get("keys").getAsJsonArray();
						for (int sk = 0; sk < keys.size(); sk++) {
							String keyEntry = keys.get(sk).getAsString();
							String nbtValue = lnbt.get(index).getAsString();
							JsonArray predicates = subRef.get("predicates").getAsJsonArray();
							for (int j = 0; j < predicates.size(); j++) {
								JsonObject pred = predicates.get(j).getAsJsonObject();
								String operator = pred.get("operator").getAsString();
								JsonObject values = pred.get("value").getAsJsonObject();
								String comparator = "";
								if (!operator.equalsIgnoreCase("EXISTS"))
									comparator = pred.get("comparator").getAsString();
								list.add(new Result(keyEntry, operator, comparator, values, operator.equalsIgnoreCase("EXISTS") ? keyEntry : nbtValue));
							}
						}
					}
				}
			}
		}
		return list;
	}
}
