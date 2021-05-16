package dicemc.dicemcpmmonbt.readers;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.StringReader;

import net.minecraft.nbt.CompoundNBT;

public class PathReader {
	/**This method takes a raw NBT path (as syntactically defined by
	 * this class's reader, and traces the provided NBT Compound for
	 * the value at that destination.  All values are returned as
	 * Strings.  if a path includes an include-all list, multiple
	 * values will be returned.
	 * 
	 * @param path the NBT path to locate values
	 * @param nbt the source to be searched
	 * @return all possible values at path destinations
	 */
	/*
	public static List<String> getNBTValues(String path, CompoundNBT nbt) {
		List<String> nodes = parsePath(path);
		return tracePath(nodes, nbt);
	}
	
	private static List<String> parsePath(String path) {
		List<String> nodes = new ArrayList<>();
		StringReader reader = new StringReader(path);
		String element = "";
		while (reader.canRead()) {
			if (reader.peek() == '.') {
				nodes.add(element);
				element = "";
				reader.read();
				continue;
			}
			element += reader.read();
		}
		return nodes;
	}
	
	private static List<String> tracePath(List<String> nodes, CompoundNBT nbt) {
		List<String> values = new ArrayList<>();
		if (nodes.isEmpty()) return values;
		for (int i = 0; i < values.size(); i++) {
			if (isList(values.get(i))) {
				
			}
		}
		return values;
	}
	
	
	
	//HELPER METHODS
	//this method might be better used in the expression reader
	private static boolean peekKeyChar(StringReader reader) {
		char c = reader.peek();
		return c == '.' || c == '[';
	}
	private static boolean isList(String node) {
		return node.contains("[");
	}
	private static int getListIndex(String node) {
		if (isList(node)) {
			
		}
		return -1;
	}*/
}
