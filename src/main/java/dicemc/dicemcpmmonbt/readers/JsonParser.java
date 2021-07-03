package dicemc.dicemcpmmonbt.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import dicemc.dicemcpmmonbt.PMMONBT;
import dicemc.dicemcpmmonbt.ReqChecker;
import harmonised.pmmo.config.JType;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

public class JsonParser {
	private static final String dataPath = "pmmo/";
	public static final String TAGFLAG = "_tagflag_";
	public static Gson gson = new Gson();
	public static final Type baseType = new TypeToken<Map<String, JsonObject>>(){}.getType();

	public static void readRawData()
    {
        File file;
        String fileName;

        for( JType jType : JType.values())
        {
        	Map<ResourceLocation, JsonObject> map = new HashMap<>();
            fileName = jType.name().toLowerCase() + "_nbt.json";
            file = FMLPaths.CONFIGDIR.get().resolve( dataPath + fileName ).toFile();
            if (!file.exists()) continue;
            
            try(InputStream input = new FileInputStream(file.getPath());
                Reader reader = new BufferedReader(new InputStreamReader(input), 1000000)) 
            {
            	Map<String, JsonObject> rootMap = gson.fromJson(reader, baseType);
            	for (Map.Entry<String, JsonObject> entry : rootMap.entrySet()) {
            		//TODO check if the key is a tag and create entries for each member
            		//this may require running at a later stage in the loading process
            		ResourceLocation key = new ResourceLocation(entry.getKey().replace("#", TAGFLAG));
            		JsonObject element = entry.getValue().getAsJsonObject();
            		map.put(key, element);
            	}
            }
            catch( Exception e )
            {
                PMMONBT.LOGGER.error( "ERROR READING PROJECT MMO CONFIG: Invalid JSON Structure of " + dataPath + fileName, e );
            }
            ReqChecker.setValues(jType, map);
        }
    }
	
	public static void parseTags(MinecraftServer server) {
		Map<JType, List<ResourceLocation>> removals = new HashMap<>();
		Map<JType, Map<ResourceLocation, JsonObject>> additions = new HashMap<>();
		//iterate through map and locate tags
		for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> src : ReqChecker.src.entrySet()) {
    		for (Map.Entry<ResourceLocation, JsonObject> map : src.getValue().entrySet()) {
    			if (map.getKey().getNamespace().contains(JsonParser.TAGFLAG)) {
    				JsonObject jsonHolder = map.getValue();
    				ResourceLocation res = new ResourceLocation(map.getKey().getNamespace().replace(JsonParser.TAGFLAG, ""), map.getKey().getPath());
    				List<Item> members = server.getTags().getItems().getTagOrEmpty(res).getValues();
    				for (int i = 0; i < members.size(); i++) {
    					additions.computeIfAbsent(src.getKey(), (a) -> new HashMap<>()).put(members.get(i).getRegistryName(), jsonHolder);
    				}
    				removals.computeIfAbsent(src.getKey(), (a) -> new ArrayList<>()).add(map.getKey());
    			}
    		}
    	}
		//remove all tag entries
		for (Map.Entry<JType, List<ResourceLocation>> rems : removals.entrySet()) {
			for (int i = 0; i < rems.getValue().size(); i++) {
				ReqChecker.src.get(rems.getKey()).remove(rems.getValue().get(i));
			}
		}
		//add tag members as entries
		for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> addmap : additions.entrySet()) {
			ReqChecker.src.get(addmap.getKey()).putAll(addmap.getValue());
		}
	}
}
