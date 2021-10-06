package dicemc.dicemcpmmonbt;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import dicemc.dicemcpmmonbt.commands.PathCommand;
import dicemc.dicemcpmmonbt.commands.PrintCommand;
import dicemc.dicemcpmmonbt.commands.ReloadCommand;
import dicemc.dicemcpmmonbt.network.Networking;
import dicemc.dicemcpmmonbt.network.PacketSync;
import dicemc.dicemcpmmonbt.readers.JsonParser;
import harmonised.pmmo.api.PredicateRegistry;
import harmonised.pmmo.api.TooltipSupplier;
import harmonised.pmmo.config.JType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

@Mod(PMMONBT.MOD_ID)
public class PMMONBT
{
	public static final String MOD_ID = "dicemcpmmonbt"; 
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
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
        PrintCommand.register(event.getDispatcher());
        //this might replace the print command since it has multiple utilities
        PathCommand.register(event.getDispatcher());
    }   
    
    @SuppressWarnings("resource")
	@SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
    	if (!event.getPlayer().getCommandSenderWorld().isClientSide && event.getPlayer() instanceof ServerPlayerEntity)
    		loginQueue.put((ServerPlayerEntity) event.getPlayer(), System.currentTimeMillis()+15000);
    }
    
    private Map<ServerPlayerEntity, Long> loginQueue = new ConcurrentHashMap<>();
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
    	if (!event.world.isClientSide) {
	    	List<ServerPlayerEntity> outQueue = new ArrayList<>();
	    	for (Map.Entry<ServerPlayerEntity, Long> map : loginQueue.entrySet()) {
	    		if (map.getValue() < System.currentTimeMillis()) 
	    			outQueue.add(map.getKey());
	    	}
	    	for (ServerPlayerEntity player : outQueue) {
	    		System.out.println("sending update packet to client");
	    		Networking.sendToClient(new PacketSync(ReqChecker.src), player);
	    		loginQueue.remove(player);
	    	}
    	}
    }
    
    @SubscribeEvent
    public void onServerStart(FMLServerStartedEvent event) {
    	JsonParser.parseTags(event.getServer());
    	ReqChecker.printSrc(LOGGER);
    	registerLogic();
    }
    
    public static void registerLogic() {
    	for (Map.Entry<JType, Map<ResourceLocation, JsonObject>> base : ReqChecker.src.entrySet()) {
    		JType jType = base.getKey();
    		for (Map.Entry<ResourceLocation, JsonObject> map : base.getValue().entrySet()) {
    			if (jType.equals(JType.REQ_WEAR) || jType.equals(JType.REQ_TOOL) || jType.equals(JType.REQ_WEAPON) ||
    					jType.equals(JType.REQ_USE) || jType.equals(JType.REQ_PLACE) ||	jType.equals(JType.REQ_KILL) || 
    					jType.equals(JType.REQ_CRAFT) || jType.equals(JType.XP_BONUS_WORN) || jType.equals(JType.XP_BONUS_HELD)) {
	    			Predicate<PlayerEntity> pred = player -> (ReqChecker.checkNBTReq(player, map.getKey(), jType));
	    			PredicateRegistry.registerPredicate(map.getKey(), jType, pred);
	    			Function<ItemStack, Map<String, Double>> func = stack -> (ReqChecker.getNBTReqs(jType, stack));
	    			TooltipSupplier.registerTooltipData(map.getKey(), jType, func);
    			}
    			//BREAK TE OBJECT
    			if (jType.equals(JType.REQ_BREAK)) {
    				BiPredicate<PlayerEntity, TileEntity> pred2 = (player, tile) -> (ReqChecker.checkNBTReq(player, tile, jType));
    				PredicateRegistry.registerBreakPredicate(map.getKey(), jType, pred2);
    				Function<TileEntity, Map<String, Double>> func2 = tile -> (ReqChecker.getNBTReqs(jType, tile));
        			TooltipSupplier.registerBreakTooltipData(map.getKey(), jType, func2);
    			}
    			//ITEMSTACK OBJECTS
    			if (jType.equals(JType.XP_VALUE_GENERAL) || jType.equals(JType.XP_VALUE_CRAFT) || 
    					jType.equals(JType.XP_VALUE_CRAFT) || jType.equals(JType.XP_VALUE_SMELT) ||
    					jType.equals(JType.XP_VALUE_COOK) || jType.equals(JType.XP_VALUE_BREW)) {
    				Function<ItemStack, Map<String, Double>> func = stack -> (ReqChecker.getNBTReqs(jType, stack));
	    			TooltipSupplier.registerTooltipData(map.getKey(), jType, func);
    			}   
    			//BLOCK/TE OJECTS
    			if (jType.equals(JType.XP_VALUE_BREAK)) {
    				Function<TileEntity, Map<String, Double>> func2 = tile -> (ReqChecker.getNBTReqs(jType, tile));
        			TooltipSupplier.registerBreakTooltipData(map.getKey(), jType, func2);
    			}
    			//ENTITY OBJECTS
    			if (jType.equals(JType.XP_VALUE_BREED) || jType.equals(JType.XP_VALUE_TAME) ||
    					jType.equals(JType.XP_VALUE_KILL)) {
    				Function<Entity, Map<String, Double>> func3 = entity -> ReqChecker.getNBTReqs(jType, entity);
    				TooltipSupplier.registerEntityTooltipData(map.getKey(), jType, func3);
    			}
    		}
    	}
    }
    
    private void initData() {
    	String fileName;
    	File dataFile;
    	for (int i = JType.REQ_WEAR.getValue(); i < JType.XP_VALUE_GROW.getValue(); i++) {
    		if (i == JType.REQ_USE_ENCHANTMENT.getValue() 
    				|| i == JType.XP_VALUE_TRIGGER.getValue() 
    				|| i == JType.REQ_BIOME.getValue()
    				|| i == JType.REQ_DIMENSION_TRAVEL.getValue()) 
    			continue;
    		fileName = JType.values()[i].name().toLowerCase() + "_nbt.json";
    		dataFile = FMLPaths.CONFIGDIR.get().resolve( "pmmo/" + fileName ).toFile();
    		if (!dataFile.exists())
    			createData(dataFile, fileName);
    	}
    	//worn and held jtypes
    	fileName = JType.XP_BONUS_WORN.name().toLowerCase() + "_nbt.json";
		dataFile = FMLPaths.CONFIGDIR.get().resolve( "pmmo/" + fileName ).toFile();
		if (!dataFile.exists())
			createData(dataFile, fileName);
		fileName = JType.XP_BONUS_HELD.name().toLowerCase() + "_nbt.json";
		dataFile = FMLPaths.CONFIGDIR.get().resolve( "pmmo/" + fileName ).toFile();
		if (!dataFile.exists())
			createData(dataFile, fileName);
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

        try( InputStream inputStream = PMMONBT.class.getResourceAsStream( "/assets/"+MOD_ID+"/util/" + fileName );
             FileOutputStream outputStream = new FileOutputStream( dataFile ); )
        {
            LOGGER.info( "Copying over " + fileName + " json config to " + dataFile.getPath(), dataFile.getPath() );
            IOUtils.copy( inputStream, outputStream );
        }
        catch( IOException e )
        {
            LOGGER.error( "Error copying over " + fileName + " json config to " + dataFile.getPath(), dataFile.getPath(), e );
        }
    }
}
