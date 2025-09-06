package tech.harrynull.universal_exporter

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.Side
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tech.harrynull.universal_exporter.data.updateMetrics
import tech.harrynull.universal_exporter.items.DataWand

@Mod(
    modid = UniversalExporter.MODID,
    version = Tags.VERSION,
    name = "UniversalExporter",
    acceptedMinecraftVersions = "[1.7.10]"
)
class UniversalExporter {
    private var httpServer: HTTPServer? = null

    @Mod.EventHandler // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    fun preInit(event: FMLPreInitializationEvent) {
        proxy!!.preInit(event)
        GameRegistry.registerItem(DataWand(), "data_wand")
        FMLCommonHandler.instance().bus().register(ServerTickHandler())
    }

    @Mod.EventHandler // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    fun init(event: FMLInitializationEvent?) {
        proxy!!.init(event)

        if (httpServer == null) {
            println("Starting HTTP server on port 9400")
            httpServer = HTTPServer.builder()
                .port(9400)
                .buildAndStart()
        }
    }

    @Mod.EventHandler // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    fun postInit(event: FMLPostInitializationEvent?) {
        proxy!!.postInit(event)
    }

    @Mod.EventHandler // register server commands in this event handler (Remove if not needed)
    fun serverStarting(event: FMLServerStartingEvent?) {
        proxy!!.serverStarting(event)
    }

    companion object {
        const val MODID: String = "universal_exporter"

        @JvmField
        val LOG: Logger = LogManager.getLogger(MODID)

        @SidedProxy(
            clientSide = "tech.harrynull.universal_exporter.ClientProxy",
            serverSide = "tech.harrynull.universal_exporter.CommonProxy"
        )
        var proxy: CommonProxy? = null
    }

    class ServerTickHandler {
        private val updateInterval = 20 * 5 // 5 seconds at 20 ticks per second

        @SubscribeEvent
        fun onWorldTick(event: WorldTickEvent) {
            if (event.phase == TickEvent.Phase.END && event.side == Side.SERVER) {
                if (event.world.worldInfo.worldTotalTime % updateInterval != 0L) {
                    return
                }
                updateMetrics(world = event.world)
            }
        }
    }
}
