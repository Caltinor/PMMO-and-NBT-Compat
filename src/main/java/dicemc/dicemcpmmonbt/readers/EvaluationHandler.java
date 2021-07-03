package dicemc.dicemcpmmonbt.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import dicemc.dicemcpmmonbt.Result;
import net.minecraft.nbt.CompoundNBT;

public class EvaluationHandler {
	
	//entry point
	public static Map<String, Double> evaluateEntries(JsonArray logic, CompoundNBT nbt, JsonObject globals) {
		Map<String, Double> map = new HashMap<>();		
		//cancels evaluation if NBT has no data
		if (nbt.isEmpty() || nbt == null) return map;
		//this section cycles through the logic and generates usable result objects
		LinkedHashMap<Pair<String, Boolean>, List<Result>> logicMap = new LinkedHashMap<>();
		for (int i = 0; i < logic.size(); i++) {
			JsonObject logicValue = logic.get(i).getAsJsonObject();
			String relationToHigher = logicValue.get("behavior_to_previous").getAsString();
			boolean summative = logicValue.get("should_cases_add").getAsBoolean();
			JsonArray pred = logicValue.get("cases").getAsJsonArray();
			logicMap.put(Pair.of(relationToHigher, summative), processCases(pred, nbt, globals));
		}
		//This section iterates through the logical tiers and processes the summative attribute
		List<Map<String, Double>> interMap = new ArrayList<>();
		List<Pair<String, Boolean>> keys = new ArrayList<>(logicMap.keySet());
		for (int i = 0; i < keys.size(); i++) {
			Map<String, Double> combinedMap = new HashMap<>();
			List<Result> data = logicMap.get(keys.get(i));
			boolean isSummative = keys.get(i).getSecond();
			for (Result r : data) {
				if (r == null) continue;
				if (!r.compares()) continue;
				Map<String, Double> value = r.values;					
				for (Map.Entry<String, Double> val : value.entrySet()) {
					combinedMap.merge(val.getKey(), val.getValue(), (in1, in2) -> {
						return isSummative ? (in1 + in2)
								: (in1 > in2 ? in1 : in2);});
				}
			}
			interMap.add(combinedMap);
		}
		//this section iterates through the logical tiers and processes the relational attribute
		for (int i = 0; i < keys.size(); i++) {
			switch (keys.get(i).getFirst()) {
			default: case "ADD_TO": {
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					map.merge(value.getKey(), value.getValue(), (oldValue, newValue) -> oldValue + newValue);
				}
				break;
			}
			case "SUB_FROM": {
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					map.merge(value.getKey(), value.getValue(), (oldValue, newValue) -> Math.max(0, oldValue - newValue));
				}
				break;
			}
			case "HIGHEST": {
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					map.merge(value.getKey(), value.getValue(), (oldValue, newValue) -> oldValue > newValue ? oldValue : newValue);
				}
				break;
			}
			case "REPLACE": {
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					map.put(value.getKey(), value.getValue());
				}
				break;
			}
			}
		}
		
		return map;
	}
	
	private static List<Result> processCases(JsonArray cases, CompoundNBT nbt, JsonObject globals) {
		List<Result> results = new ArrayList<>();
		for (int i = 0; i < cases.size(); i++) {
			JsonObject pred = cases.get(i).getAsJsonObject();
			String operator = pred.get("operator").getAsString();
			JsonObject values = pred.get("value").getAsJsonObject();
			String comparator = "";
			if (!operator.equalsIgnoreCase("EXISTS"))
				comparator = pred.get("comparator").getAsString();
			//TODO find a way to use multiple "paths":["path1","path2"]
			List<String> comparison = PathReader.getNBTValues(getPathOrGlobal(globals, pred.get("path").getAsString()), nbt); 
			for (int j = 0; j < comparison.size(); j++) {
				results.add(new Result(operator, comparator, values, comparison.get(j)));
			}
		}
		return results;
	}
	
	private static String getPathOrGlobal(JsonObject globals, String key) {
		//Note, local paths and constants will not be in this iteration
		return key.contains("#") ? globals.get("paths").getAsJsonObject().get(key.replace("#", "")).getAsString() : key;
	}

}
