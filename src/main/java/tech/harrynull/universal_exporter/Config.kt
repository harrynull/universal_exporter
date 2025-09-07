package tech.harrynull.universal_exporter

import net.minecraftforge.common.config.Configuration
import java.io.File

object Config {
    var host: String = "0.0.0.0"
    var port: Int = 9400
    var updateIntervalTicks: Int = 100

    @JvmStatic
    fun synchronizeConfiguration(configFile: File?) {
        val configuration = Configuration(configFile)

        //greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?")
        host = configuration.getString(
            "host",
            Configuration.CATEGORY_GENERAL,
            "0.0.0.0",
            "Host to bind to for the metrics server"
        )
        port =
            configuration.getInt(
                "port",
                Configuration.CATEGORY_GENERAL,
                9400,
                0,
                65536,
                "Port to bind to for the metrics server"
            )
        updateIntervalTicks =
            configuration.getInt(
                "updateIntervalTicks",
                Configuration.CATEGORY_GENERAL,
                100,
                1,
                Integer.MAX_VALUE,
                "Gauge value update intervals"
            )

        if (configuration.hasChanged()) {
            configuration.save()
        }
    }
}
