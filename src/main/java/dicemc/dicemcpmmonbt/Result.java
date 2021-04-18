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
	
	public Result(String key, String operator, String comparator, JsonObject values, String comparison) {
		res = key;
		this.comparison = comparison;
		for (int i = 0; i < Operator.values().length; i++) {
			if (Operator.values()[i].toString().equalsIgnoreCase(operator)) {
				this.operator = Operator.values()[i];
				break;
			}
		}
		values.entrySet().forEach((e) -> {
			this.values.put(e.getKey(), e.getValue().getAsDouble());
		});
		this.comparator = comparator;
	}

	public boolean compares() {
		switch (operator) {
		case EQUALS: {
			System.out.println(comparator+":"+comparison);
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