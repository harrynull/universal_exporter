package tech.harrynull.universal_exporter.data

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldSavedData
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MetricType {
    GAUGE,
    COUNTER,
    AE,
}

fun tagsStringToMap(tags: String): Map<String, String> {
    return tags.split(",").mapNotNull {
        val parts = it.split("=")
        if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            null
        }
    }.toMap()
}

fun mapToTagsString(map: Map<String, String>): String {
    return map.entries.joinToString(",") { "${it.key}=${it.value}" }
}

@OptIn(ExperimentalUuidApi::class)
data class TrackedMetric @OptIn(ExperimentalUuidApi::class) constructor(
    val x: Int,
    val y: Int,
    val z: Int,
    val type: MetricType,
    val name: String,
    val value: String,
    val uuid: Uuid = Uuid.random(),
    val labels: Map<String, String> = emptyMap(),
) {
    constructor(nbt: NBTTagCompound) : this(
        nbt.getInteger("x"),
        nbt.getInteger("y"),
        nbt.getInteger("z"),
        MetricType.valueOf(nbt.getString("type")),
        nbt.getString("name"),
        nbt.getString("value"),
        Uuid.fromLongs(nbt.getLong("uuidMost"), nbt.getLong("uuidLeast")),
        tagsStringToMap(nbt.getString("labels"))
    )

    fun writeToNBT(nbt: NBTTagCompound) {
        nbt.setInteger("x", x)
        nbt.setInteger("y", y)
        nbt.setInteger("z", z)
        nbt.setString("type", type.name)
        nbt.setString("name", name)
        nbt.setString("value", value)
        nbt.setLong("uuidMost", uuid.toLongs { mostSignificantBits, _ -> mostSignificantBits })
        nbt.setLong("uuidLeast", uuid.toLongs { _, leastSignificantBits -> leastSignificantBits })
        nbt.setString("labels", mapToTagsString(labels))
    }
}

@OptIn(ExperimentalUuidApi::class)
class TrackedMetrics : WorldSavedData {
    private val metrics: MutableList<TrackedMetric> = mutableListOf()

    constructor(name: String) : super(name)

    override fun readFromNBT(nbt: NBTTagCompound?) {
        nbt?.let {
            metrics.clear()
            val list = it.getTagList("metrics", 10) // 10 is the ID for compound tags
            for (i in 0 until list.tagCount()) {
                val metricNBT = list.getCompoundTagAt(i)
                val metric = TrackedMetric(metricNBT)
                metrics.add(metric)
            }
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        nbt?.let {
            val list = net.minecraft.nbt.NBTTagList()
            for (metric in metrics) {
                val metricNBT = NBTTagCompound()
                metric.writeToNBT(metricNBT)
                list.appendTag(metricNBT)
            }
            it.setTag("metrics", list)
        }
    }

    fun addMetric(metric: TrackedMetric) {
        metrics.add(metric)
        markDirty()
    }

    fun deleteMetric(uuid: Uuid) {
        metrics.removeIf { it.uuid == uuid }
        markDirty()
    }

    fun getMetrics(): List<TrackedMetric> = metrics.toList()

    companion object {
        fun get(world: net.minecraft.world.World): TrackedMetrics {
            val data = world.perWorldStorage.loadData(TrackedMetrics::class.java, "tracked_metrics") as? TrackedMetrics
            return data ?: TrackedMetrics("tracked_metrics").also {
                world.perWorldStorage.setData("tracked_metrics", it)
            }
        }

        private val metricLastUpdated = mutableMapOf<Uuid, Instant>()
        fun setMetricActive(metric: TrackedMetric) {
            metricLastUpdated[metric.uuid] = Instant.now()
        }

        fun isMetricActive(metric: TrackedMetric): Boolean {
            val lastUpdated = metricLastUpdated[metric.uuid] ?: return false
            return Instant.now().epochSecond - lastUpdated.epochSecond < 60
        }

        fun lastUpdated(metric: TrackedMetric) = metricLastUpdated[metric.uuid]
    }

}
