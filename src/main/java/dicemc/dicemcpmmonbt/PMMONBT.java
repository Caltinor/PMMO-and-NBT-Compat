package dicemc.dicemcpmmonbt;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.commands.PrintCommand;
import dicemc.dicemcpmmonbt.commands.ReloadCommand;
import dicemc.dicemcpmmonbt.network.Networking;
import dicemc.dicemcpmmonbt.network.PacketSync;
import dicemc.dicemcpmmonbt.readers.JsonParser;
import harmonised.pmmo.ProjectMMOMod;
import harmonised.pmmo.api.PredicateRegistry;
import harmonised.pmmo.api.TooltipSupplier;
import harmonised.pmmo.config.JType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PMMONBT.MOD_ID)
public class PMMONBT
{
	public static final String MOD_ID = "dicemcpmmonbt"; 
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public PMMONBT() {
    	initData();
    	JsonParser.readRawData();
    	ReqChecker.printSrc(LOGGER);
    	
    	FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
    }
    
    public void setup(final FMLCommonSetupEvent event) {
    	Networking.registerMessages();
    	registerLogic();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
        PrintCommand.register(event.getDispatcher());
    }   
    
    @SuppressWarnings("resource")
	@SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
    	if (!event.getPlayer().getCommandSenderWorld().isClientSide)
    		Networking.sendToClient(new PacketSync(ReqChecker.src), (ServerPlayerEntity) event.getPlayer());
    }
    
    public static void registerLogic() {
    	for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> base : ReqChecker.src.entrySet()) {
    		JType jType = base.getKey();
    		for (Map.Entry<ResourceLocation, JsonObject> map : base.getValue().entrySet()) {
    			Predicate<PlayerEntity> pred = player -> (ReqChecker.checkNBTReq(player, map.getKey(), jType));
    			PredicateRegistry.registerPredicate(map.getKey(), jType, pred);
    			Function<ItemStack, Map<String, Double>> func = stack -> (ReqChecker.getNBTReqs(jType, stack));
    			TooltipSupplier.registerTooltipData(map.getKey(), jType, func);
    			if (jType.equals(JType.REQ_BREAK)) {
    				BiPredicate<PlayerEntity, TileEntity> pred2 = (player, tile) -> (ReqChecker.checkNBTReq(player, tile, jType));
    				PredicateRegistry.registerBreakPredicate(map.getKey(), jType, pred2);
    				Function<TileEntity, Map<String, Double>> func2 = tile -> (ReqChecker.getNBTReqs(jType, tile));
        			TooltipSupplier.registerBreakTooltipData(map.getKey(), jType, func2);
    			}
    		}
    	}
    }
    
    private void initData() {
    	String fileName;
    	for (int i = JType.REQ_WEAR.getValue(); i <= JType.REQ_CRAFT.getValue(); i++) {
    		if (i == JType.REQ_USE_ENCHANTMENT.getValue()) continue;
    		fileName = JType.values()[i].name().toLowerCase() + "_nbt.json";
    		File dataFile = FMLPaths.CONFIGDIR.get().resolve( "pmmo/" + fileName ).toFile();
    		if (!dataFile.exists())
    			createData(dataFile, fileName);
    	}
    }
    
    private void createData( File dataFile, String fileName )
    {
        try     //create template data file
        {
            dataFile.getParentFile().mkdir();
            dataFile.createNewFile();
        }
        catch( IOException e )
        {
            LOGGER.error( "Could not create template json config!", dataFile.getPath(), e );
        }

        try( InputStream inputStream = ProjectMMOMod.class.getResourceAsStream( "/assets/"+MOD_ID+"/util/" + fileName );
             FileOutputStream outputStream = new FileOutputStream( dataFile ); )
        {
            LOGGER.debug( "Copying over " + fileName + " json config to " + dataFile.getPath(), dataFile.getPath() );
            IOUtils.copy( inputStream, outputStream );
        }
        catch( IOException e )
        {
            LOGGER.error( "Error copying over " + fileName + " json config to " + dataFile.getPath(), dataFile.getPath(), e );
        }
    }
}
