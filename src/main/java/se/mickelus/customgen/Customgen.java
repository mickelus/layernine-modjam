package se.mickelus.customgen;


import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import se.mickelus.customgen.blocks.EmptyBlock;
import se.mickelus.customgen.blocks.InterfaceBlock;
import se.mickelus.customgen.items.GenBookItem;
import se.mickelus.customgen.items.PlaceholderItem;
import se.mickelus.customgen.models.Gen;
import se.mickelus.customgen.models.GenManager;
import se.mickelus.customgen.network.GenAddRequestPacket;
import se.mickelus.customgen.network.GenGenerationRequestPacket;
import se.mickelus.customgen.network.GenListReponsePacket;
import se.mickelus.customgen.network.GenListRequestPacket;
import se.mickelus.customgen.network.GenRequestPacket;
import se.mickelus.customgen.network.GenResponsePacket;
import se.mickelus.customgen.network.PacketPipeline;
import se.mickelus.customgen.network.SegmentAddRequestPacket;
import se.mickelus.customgen.network.SegmentGenerationRequestPacket;
import se.mickelus.customgen.network.SegmentRequestPacket;
import se.mickelus.customgen.network.SegmentResponsePacket;
import se.mickelus.customgen.network.TemplateGenerationRequestPacket;
import se.mickelus.customgen.proxy.Proxy;

@Mod (modid = Constants.MOD_ID, name = Constants.MOD_NAME, version = Constants.VERSION)
public class Customgen {
	
	@Instance(Constants.MOD_ID)
    public static Customgen instance;
	
	@SidedProxy(clientSide = "se.mickelus.customgen.proxy.ClientProxy", serverSide = "se.mickelus.customgen.proxy.ServerProxy")
	public static Proxy proxy;
	
	public static final PacketPipeline packetPipeline = new PacketPipeline();
	
	@EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigHandler.init(event.getSuggestedConfigurationFile());
        
        new CustomgenCreativeTabs();
        
        new EmptyBlock();
		new InterfaceBlock();
		
		new PlaceholderItem();
		new GenBookItem();
    }
	
	@EventHandler
    public void init(FMLInitializationEvent event) {  
		packetPipeline.initialize();
		
		packetPipeline.registerPacket(GenAddRequestPacket.class);
		packetPipeline.registerPacket(GenGenerationRequestPacket.class);
		packetPipeline.registerPacket(GenListReponsePacket.class);
		packetPipeline.registerPacket(GenListRequestPacket.class);
		packetPipeline.registerPacket(GenRequestPacket.class);
		packetPipeline.registerPacket(GenResponsePacket.class);
		packetPipeline.registerPacket(SegmentAddRequestPacket.class);
		packetPipeline.registerPacket(SegmentGenerationRequestPacket.class);
		packetPipeline.registerPacket(SegmentRequestPacket.class);
		packetPipeline.registerPacket(SegmentResponsePacket.class);
		packetPipeline.registerPacket(TemplateGenerationRequestPacket.class);
        
        proxy.init();
    }
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		packetPipeline.postInitialize();
	}
	
	
	@EventHandler
	public void serverStart(FMLServerStartingEvent event){
		
		if(ForgeGenerator.getInstance() == null) {
			new ForgeGenerator();
		}

		/* when playing singleplayer this lets us update Gens by restarting the world
		 * instead of restarting the game */
		GenManager genManager;
		//if(GenManager.getInstance() == null) {
			genManager = new GenManager();
		/*} else {
			genManager = GenManager.getInstance();
		}*/
        Gen[] gens = FileHandler.parseAllGens();
        if(gens != null) {
        	for (int i = 0; i < gens.length; i++) {
    			genManager.addGen(gens[i]);
    		}
        }
        
	}
}
