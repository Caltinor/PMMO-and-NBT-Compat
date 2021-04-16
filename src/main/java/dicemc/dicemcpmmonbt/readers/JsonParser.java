package dicemc.dicemcpmmonbt.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import dicemc.dicemcpmmonbt.PMMONBT;
import dicemc.dicemcpmmonbt.ReqChecker;
import harmonised.pmmo.config.JType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

public class JsonParser {
	private static final String dataPath = "pmmo/";
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
                Reader reader = new BufferedReader(new InputStreamReader(input))) 
            {
            	Map<String, JsonObject> rootMap = gson.fromJson(reader, baseType);
            	for (Map.Entry<String, JsonObject> entry : rootMap.entrySet()) {
            		ResourceLocation key = new ResourceLocation(entry.getKey());
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
}
