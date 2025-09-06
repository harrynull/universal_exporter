package tech.harrynull.universal_exporter

import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import tech.harrynull.universal_exporter.Config.synchronizeConfiguration

open class CommonProxy {
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    fun preInit(event: FMLPreInitializationEvent) {
        synchronizeConfiguration(event.suggestedConfigurationFile)
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    fun init(event: FMLInitializationEvent?) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    fun postInit(event: FMLPostInitializationEvent?) {}

    // register server commands in this event handler (Remove if not needed)
    fun serverStarting(event: FMLServerStartingEvent?) {}
}
