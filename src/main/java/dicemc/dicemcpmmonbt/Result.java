package dicemc.dicemcpmmonbt;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

public class Result {
	public static enum Operator {
		EQUALS, 
		GREATER_THAN, 
		LESS_THAN, 
		GREATER_THAN_OR_EQUAL, 
		LESS_THAN_OR_EQUAL,
		EXISTS
	}
	
	public Operator operator;
	public String res;
	public String comparator, comparison;
	public Map<String, Double> values = new HashMap<>();
	
	public Result(JsonObject obj, String comparison) {
		res = obj.get("key").getAsString();
		String op = obj.get("predicate").getAsJsonObject().get("operator").getAsString();
		for (int i = 0; i < Operator.values().length; i++) {
			if (Operator.values()[i].toString().equalsIgnoreCase(op)) {
				operator = Operator.values()[i];
				break;
			}
		}
		JsonObject pred = obj.get("predicate").getAsJsonObject();
		if (!pred.get("operator").getAsString().equalsIgnoreCase("EXISTS"))
			comparator = pred.get("comparator").getAsString();
		JsonObject valueInputs = obj.get("value").getAsJsonObject();
		valueInputs.entrySet().forEach((e) -> {
			values.put(e.getKey(), e.getValue().getAsDouble());
		});
		this.comparison = comparison;
	}
	
	public boolean compares() {
		switch (operator) {
		case EQUALS: {
			return comparator.equals(comparison);
		}
		case GREATER_THAN: {
			return Double.valueOf(comparator) < Double.valueOf(comparison);
		}
		case LESS_THAN: {
			return Double.valueOf(comparator) > Double.valueOf(comparison);
		}
		case GREATER_THAN_OR_EQUAL: {
			return Double.valueOf(comparator) <= Double.valueOf(comparison);
		}
		case LESS_THAN_OR_EQUAL: {
			return Double.valueOf(comparator) >= Double.valueOf(comparison);
		}
		case EXISTS: {
			return comparison.equalsIgnoreCase(res);
		}
		default: return false;}
	}
}