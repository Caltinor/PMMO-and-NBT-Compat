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
		String key = obj.getAsJsonObject().get("key").getAsString();
		//if the root NBT contains the desired key, process the key
		if (type.equalsIgnoreCase("id") && nbt.contains(key)) {		
			list.add(new Result(obj, nbt.getString(key)));
		}
		//if the type is a compound, go one step deeper with recursion
		else if (type.equalsIgnoreCase("compound")){
			CompoundNBT nbtOut = key.equalsIgnoreCase("") ? nbt : nbt.getCompound(key);					
			list.addAll(evaluateEntry(obj.getAsJsonObject().get("sub_reference").getAsJsonObject(), nbtOut));
		}
		//if the type is list process list logic
		else if (type.equalsIgnoreCase("list")) {
			list.addAll(evaluateList(obj, (ListNBT) nbt.get(obj.getAsJsonObject().get("key").getAsString())));
		}
		return list;
	}
	//list evaluator
	private static List<Result> evaluateList(JsonObject obj, ListNBT lnbt){
		List<Result> list = new ArrayList<>();
		if (lnbt == null) return list;
		
		JsonObject subRef = obj.getAsJsonObject().get("sub_reference").getAsJsonObject();
		int index = obj.getAsJsonObject().get("index").getAsInt();
		//safety for invalid json entries
		if (index < -1 || index >= lnbt.size()) return null;

		if (lnbt.size() > 0) {
			if (index == -1) {
				for (int i = 0; i < lnbt.size(); i++) {
					if (lnbt.get(0) instanceof CompoundNBT)
						list.addAll(evaluateEntry(subRef, lnbt.getCompound(i)));
					else if (lnbt.get(0) instanceof ListNBT)
						list.addAll(evaluateList(subRef, lnbt.getList(i)));
					else
						list.add(new Result(subRef, lnbt.getString(i)));	
				}
			}
			else {
				if (lnbt.get(0) instanceof CompoundNBT)
					list.addAll(evaluateEntry(subRef, lnbt.getCompound(index)));
				else if (lnbt.get(0) instanceof ListNBT)
					list.addAll(evaluateList(subRef, lnbt.getList(index)));
				else
					list.add(new Result(subRef, lnbt.getString(index)));
			}							
		}
		return list;
	}
}
