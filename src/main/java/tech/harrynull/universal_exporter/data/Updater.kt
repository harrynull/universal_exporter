package tech.harrynull.universal_exporter.data

import appeng.me.helpers.IGridProxyable
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import net.minecraft.world.World
import tech.harrynull.universal_exporter.data.MetricType.GAUGE

private val registeredCounters = mutableMapOf<String, Counter>()
private val registeredGauges = mutableMapOf<String, Gauge>()

fun updateCounter(world: World, x: Int, y: Int, z: Int, type: String, amount: Double = 1.0) {
    val metric = TrackedMetrics.get(world).getMetrics().find {
        it.type == MetricType.COUNTER && it.x == x && it.y == y && it.z == z && it.value == type
    } ?: return
    val counter = registeredCounters.getOrPut(metric.name) {
        Counter.builder()
            .name(metric.name)
            .help("Counter metric $metric")
            .labelNames(*metric.labels.keys.toTypedArray())
            .register()
    }
    counter.labelValues(*metric.labels.values.toTypedArray()).inc(amount)
    TrackedMetrics.setMetricActive(metric)
}

fun updateMetrics(world: World) {
    val metrics = TrackedMetrics.get(world)
    for (metric in metrics.getMetrics()) {
        when (metric.type) {
            GAUGE -> {
                val obj = objectFromWorld(world, metric.x, metric.y, metric.z)
                val value = accessObject(obj, metric.value) ?: continue
                val sameNameMetrics = metrics.getMetrics()
                    .filter { it.name == metric.name }
                val gauge = registeredGauges.getOrPut(metric.name) {
                    Gauge.builder()
                        .name(metric.name)
                        .help(
                            "Gauge metric ${
                                sameNameMetrics.joinToString(", ")
                            }"
                        )
                        .labelNames(
                            *(sameNameMetrics
                                .flatMap { it.labels.keys }
                                .toSet()
                                .toTypedArray()
                                )
                        )
                        .register()
                }
                gauge.labelValues(*metric.labels.values.toTypedArray()).set(value.toDouble())
            }

            MetricType.AE -> {
                val obj = objectFromWorld(world, metric.x, metric.y, metric.z)
                if (obj !is IGridProxyable) {
                    continue
                }
                val itemGaugeKey = "${metric.name}_items"
                val itemGauge = registeredGauges.getOrPut(itemGaugeKey) {
                    Gauge.builder()
                        .name(itemGaugeKey)
                        .help("Number of items in AE system")
                        .labelNames("pos", "item", *metric.labels.keys.toTypedArray())
                        .register()
                }
                obj.proxy.storage.itemInventory.storageList.forEach { itemStack ->
                    val count = itemStack.stackSize
                    itemGauge
                        .labelValues(
                            "${metric.x},${metric.y},${metric.z}",
                            itemStack.itemStack.displayName,
                            *metric.labels.values.toTypedArray()
                        )
                        .set(count.toDouble())
                }
                val fluidGaugeKey = "${metric.name}_fluids"
                val fluidGauge = registeredGauges.getOrPut(fluidGaugeKey) {
                    Gauge.builder()
                        .name(fluidGaugeKey)
                        .help("Number of fluids in AE system")
                        .labelNames("pos", "fluid", *metric.labels.keys.toTypedArray())
                        .register()
                }
                obj.proxy.storage.fluidInventory.storageList.forEach { fluidStack ->
                    val count = fluidStack.stackSize
                    fluidGauge
                        .labelValues(
                            "${metric.x},${metric.y},${metric.z}",
                            fluidStack.fluidStack.localizedName,
                            *metric.labels.values.toTypedArray()
                        )
                        .set(count.toDouble())
                }
                val cpuGaugeKey = "${metric.name}_cpus"
                val cpuGauge = registeredGauges.getOrPut(cpuGaugeKey) {
                    Gauge.builder()
                        .name(cpuGaugeKey)
                        .help("AE CPU usage")
                        .labelNames("pos", "cpu", *metric.labels.keys.toTypedArray())
                        .register()
                }

                obj.proxy.crafting.cpus.forEachIndexed { index: Int, cpu ->
                    cpuGauge.labelValues(
                        "${metric.x},${metric.y},${metric.z}",
                        cpu.name?.takeIf { it.isNotBlank() } ?: "cpu$index",
                        *metric.labels.values.toTypedArray()
                    ).set(cpu.remainingItemCount.toDouble())
                }
            }

            MetricType.COUNTER -> {
                continue
            }
        }
        TrackedMetrics.setMetricActive(metric)
    }
}
