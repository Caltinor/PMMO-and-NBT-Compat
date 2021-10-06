package dicemc.dicemcpmmonbt.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dicemc.dicemcpmmonbt.Result;
import net.minecraft.nbt.CompoundTag;

public class EvaluationHandler {
	
	//entry point
	public static Map<String, Double> evaluateEntries(JsonArray logic, CompoundTag nbt, JsonObject globals) {
		Map<String, Double> map = new HashMap<>();		
		try {
		//cancels evaluation if NBT has no data
		if (nbt.isEmpty() || nbt == null) return map;
		//this section cycles through the logic and generates usable result objects
		List<LogicTier> logicSequence = new ArrayList<>();
		for (int i = 0; i < logic.size(); i++) {
			JsonObject logicValue = logic.get(i).getAsJsonObject();
			String relationToHigher = logicValue.get("behavior_to_previous").getAsString();
			boolean summative = logicValue.get("should_cases_add").getAsBoolean();
			JsonArray pred = logicValue.get("cases").getAsJsonArray();
			logicSequence.add(new LogicTier(relationToHigher, summative, processCases(pred, nbt, globals)));
		}
		//This section iterates through the logical tiers and processes the summative attribute
		List<Map<String, Double>> interMap = new ArrayList<>();
		for (int i = 0; i < logicSequence.size(); i++) {
			Map<String, Double> combinedMap = new HashMap<>();
			List<Result> data = logicSequence.get(i).results;
			boolean isSummative = logicSequence.get(i).isSummative;
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
		for (int i = 0; i < logicSequence.size(); i++) {
			switch (logicSequence.get(i).relationToHigher) {
			case "SUB_FROM": {
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					if (map.getOrDefault(value.getKey(), 0d) - value.getValue() <= 0) map.remove(value.getKey());
					else 
						map.merge(value.getKey(), value.getValue(), (oldValue, newValue) -> oldValue - newValue);
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
			case "ADD_TO": default:{
				for (Map.Entry<String, Double> value : interMap.get(i).entrySet()) {
					map.merge(value.getKey(), value.getValue(), (oldValue, newValue) -> oldValue + newValue);
				}
				break;
			}
			}
		}
		}
		catch (NullPointerException e) {e.printStackTrace();}
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
		return map;
		
	}
	
	private static List<Result> processCases(JsonArray cases, CompoundTag nbt, JsonObject globals) {
		List<Result> results = new ArrayList<>();
		for (int i = 0; i < cases.size(); i++) {
			JsonObject caseIterant = cases.get(i).getAsJsonObject();
			JsonArray paths = caseIterant.get("paths").getAsJsonArray();
			JsonArray criteria = caseIterant.get("criteria").getAsJsonArray();
			for (int p = 0; p < paths.size(); p++) {
				for (int c = 0; c < criteria.size(); c++) {
					JsonObject critObj = criteria.get(c).getAsJsonObject();
					JsonObject values = critObj.get("value").getAsJsonObject();
					String operator = critObj.get("operator").getAsString();					
					List<String> comparison = PathReader.getNBTValues(getPathOrGlobal(globals, paths.get(p).getAsString()), nbt);
					for (int j = 0; j < comparison.size(); j++) {
						JsonArray comparators = new JsonArray();
						if (!operator.equalsIgnoreCase("EXISTS")) {
							comparators = critObj.get("comparators").getAsJsonArray();
							for (int l = 0; l < comparators.size(); l++) {
								results.add(new Result(operator, comparators.get(l).getAsString(), values, comparison.get(j)));
							}
						}
						else results.add(new Result(operator, "", values, comparison.get(j)));
					}
					
				}
			}
		}
		return results;
	}
	
	public static String getPathOrGlobal(JsonObject globals, String key) {
		//Note, local paths and constants will not be in this iteration
		return key.contains("#") ? globals.get("paths").getAsJsonObject().get(key.replace("#", "")).getAsString() : key;
	}
	
	protected static class LogicTier {
		public String relationToHigher;
		public boolean isSummative;
		public List<Result> results;
		
		public LogicTier(String relationToHigher, boolean isSummative, List<Result> results) {
			this.relationToHigher = relationToHigher;
			this.isSummative = isSummative;
			this.results = results;
		}
	}

}
