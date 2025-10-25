package tech.harrynull.universal_exporter.gui

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.api.ITheme
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.theme.WidgetTheme
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.utils.Color
import com.cleanroommc.modularui.value.sync.BooleanSyncValue
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import com.cleanroommc.modularui.value.sync.StringSyncValue
import com.cleanroommc.modularui.value.sync.SyncHandler
import com.cleanroommc.modularui.widgets.*
import com.cleanroommc.modularui.widgets.layout.Flow
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget
import gregtech.api.metatileentity.BaseMetaTileEntity
import net.minecraft.network.PacketBuffer
import tech.harrynull.universal_exporter.data.*
import java.lang.reflect.Modifier
import java.util.concurrent.locks.ReentrantLock
import kotlin.uuid.ExperimentalUuidApi

class MyList : ListWidget<IWidget, MyList>()

class ClickableWidget<W : ClickableWidget<W>> : ButtonWidget<W>() {
    override fun getWidgetThemeInternal(theme: ITheme): WidgetTheme? {
        return theme.fallback
    }
}

class ConfigureUI : IGuiHolder<ConfigureUIData> {
    private enum class SyncIDs(val id: Int) {
        SEARCH_TEXT(0),
        SEARCH_RESULTS(1),
        SAVE(2),
        INIT(3),
        INIT_RESULTS(4),
    }

    private lateinit var syncHandler: VariableHintSyncManager
    private var searchText = ""
    private var trigger = ""
    private var possibleTriggers = listOf<String>()
    private var name = ""
    private var selectedType = MetricType.GAUGE
    private var labelString = ""
    private var pagedWidgetController = PagedWidget.Controller()
    private lateinit var myList: MyList
    private val searchResultUpdateLock = ReentrantLock()

    fun buildTypeSpecificSettings(): PagedWidget<*> = PagedWidget()
        .addPage(
            Flow.column()
                .child(
                    Flow.row()
                        .child(
                            TextWidget("Accessor").width(50)
                        )
                        .child(
                            TextFieldWidget().value(
                                StringSyncValue(
                                    // client getter
                                    { searchText },
                                    { newSearchText ->
                                        searchText = newSearchText
                                        if (syncHandler.syncManager.isClient) {
                                            syncHandler.syncToServer(SyncIDs.SEARCH_TEXT.id) {
                                                it.writeStringToBuffer(newSearchText)
                                            }
                                        }

                                    })
                            ).width(200).height(10)
                        ).height(20)
                ).child(
                    MyList().also { myList = it }.widthRel(1.0f).height(100)
                )
        )
        .addPage(
            Flow.column().child(
                Flow.row()
                    .child(
                        TextWidget("Trigger").width(50)
                    )
                    .child(
                        TextFieldWidget().value(
                            StringSyncValue(
                                { trigger },
                                { newTrigger -> trigger = newTrigger }
                            )
                        ).width(200).height(10)
                    ).height(30)
            ).child(
                TextWidget(IKey.dynamic { possibleTriggers.joinToString(",") }.withStyle())
                    .widthRel(1.0f)
            )
        )
        .addPage(TextWidget("AE items, fluids and CPU will be exported"))
        .controller(pagedWidgetController)

    override fun buildUI(data: ConfigureUIData, syncManager: PanelSyncManager, settings: UISettings): ModularPanel {
        syncHandler = VariableHintSyncManager(data.x, data.y, data.z)
        val panel = ModularPanel.defaultPanel("custom", 300, 200)
        panel.child(
            Flow.column()
                .child(
                    Flow.row()
                        .child(TextWidget("Name").width(50))
                        .child(
                            TextFieldWidget().value(
                                StringSyncValue(
                                    { name },
                                    { newName -> name = newName })
                            ).width(200).height(10)
                        )
                        .height(20)
                )
                .child(
                    Flow.row()
                        .child(TextWidget("Labels").width(50))
                        .child(
                            TextFieldWidget().value(
                                StringSyncValue(
                                    { labelString },
                                    { newLabelString -> labelString = newLabelString })
                            ).width(200).height(10)
                        )
                        .height(20)
                )
                .child(
                    Flow.row()
                        .child(TextWidget("Type").width(50))
                        .apply {
                            for (type in MetricType.entries) {
                                if (type == MetricType.AE && !data.aeEnabled) {
                                    continue
                                }
                                child(
                                    ToggleButton().value(
                                        BooleanSyncValue(
                                            { selectedType == type },
                                            { value: Boolean ->
                                                if (value) {
                                                    selectedType = type
                                                    if (pagedWidgetController.isInitialised) {
                                                        pagedWidgetController.setPage(type.ordinal)
                                                    }
                                                }
                                            })
                                    ).size(10)
                                )
                                child(TextWidget(type.name).width(50).paddingLeft(5))
                            }
                        }
                        .height(15)
                )
                .child(buildTypeSpecificSettings().widthRel(1.0f).height(120))
                .child(
                    ButtonWidget().overlay(IKey.str("Create")).width(50).height(10)
                        .onMouseReleased {
                            syncHandler.syncToServer(SyncIDs.SAVE.id)
                            panel.closeIfOpen()
                            false
                        }
                )
        ).padding(10)
        syncManager.syncValue("variable_hints", syncHandler)
        syncManager.addOpenListener { player ->
            if (player.entityWorld.isRemote) {
                syncHandler.syncToServer(SyncIDs.INIT.id)
            }
        }
        return panel
    }

    data class FieldPreviewItem(val fullyQualifiedName: String, val typeName: String, val value: Any?) {
        val stringValue: String
            get() {
                if (value == null) return "<null>"
                if (value is Number || value is Boolean) return value.toString()
                return "..."
            }
        val shouldHide: Boolean get() = value is String
        val relativeOrder: Int
            get() =
                when (value) {
                    null -> 10
                    is Number -> 1
                    is Boolean -> 2
                    else -> 5
                }

    }

    @OptIn(ExperimentalUuidApi::class)
    inner class VariableHintSyncManager(private val x: Int, private val y: Int, private val z: Int) : SyncHandler() {
        override fun readOnClient(id: Int, buf: PacketBuffer) {
            when (id) {
                SyncIDs.SEARCH_RESULTS.id -> {
                    val count = buf.readInt()
                    val results = ((0 until count).map { buf.readStringFromBuffer(128) })
                    while (myList.children.isNotEmpty()) {
                        myList.remove(0)
                    }
                    for (result in results) {
                        val parts = result.split(",")
                        val name = parts[0]
                        val type = parts[1]
                        val value = parts[2]
                        myList.child(
                            ClickableWidget()
                                .child(
                                    Flow.row()
                                        .child(TextWidget(name))
                                        .child(
                                            Flow.row()
                                                .child(TextWidget(type).color(Color.GREY.main).paddingRight(5))
                                                .child(TextWidget(value).color(Color.LIGHT_BLUE.main))
                                                .anchor(Alignment.CenterRight)
                                                .mainAxisAlignment(Alignment.MainAxis.END)
                                        )
                                        .widthRel(1.0f)
                                        .height(10)
                                )
                                .onMouseReleased { mouseButton ->
                                    if (mouseButton != 0) return@onMouseReleased false
                                    syncHandler.syncToServer(SyncIDs.SEARCH_TEXT.id) { it.writeStringToBuffer(name) }
                                    false
                                }
                                .widthRel(1.0f)
                                .height(10)
                        )
                    }
                }

                SyncIDs.INIT_RESULTS.id -> {
                    val count = buf.readInt()
                    possibleTriggers = (0 until count).map { buf.readStringFromBuffer(128) }
                }
            }
        }

        fun listCandidates(obj: Any?, searchText: String, prefix: String): List<FieldPreviewItem> {
            if (obj == null) return emptyList()

            val allFields = getAllFieldsIncludingInherited(obj.javaClass)

            if (searchText.contains(".")) {
                val firstPart = searchText.substringBefore(".")
                val firstPartNumeric = firstPart.toIntOrNull()
                val field = if (firstPartNumeric == null) {
                    allFields.singleOrNull { it.name == firstPart }?.also { it.isAccessible = true }?.get(obj)
                } else {
                    accessArray(obj, firstPartNumeric)
                }
                return listCandidates(
                    field ?: return emptyList(),
                    searchText.substringAfter("."),
                    "$prefix${firstPart}.",
                )
            }

            val matchingFields = allFields.filter {
                !Modifier.isStatic(it.modifiers) &&
                    (searchText.isBlank() || it.name.contains(
                        searchText,
                        ignoreCase = true
                    ))
            }.map { it.isAccessible = true; it }

            return matchingFields
                .map { field ->
                    FieldPreviewItem("${prefix}${field.name}", field.type.simpleName, field.get(obj))
                }
                .filterNot { it.shouldHide }
                .sortedBy { it.relativeOrder }
        }

        override fun readOnServer(id: Int, buf: PacketBuffer) {
            when (id) {
                SyncIDs.SEARCH_TEXT.id -> {
                    val searchText = buf.readStringFromBuffer(64)
                    this@ConfigureUI.searchText = searchText
                    var obj = syncManager.player.entityWorld.getTileEntity(x, y, z) as Any?
                    if (obj != null && obj is BaseMetaTileEntity) {
                        obj = obj.metaTileEntity as Any
                    }
                    val history = TrackedMetrics.get(syncManager.player.entityWorld).getMetrics()
                        .filter { it.type == MetricType.GAUGE }
                        .map { it.value }
                        .toSet()
                        .map { it to accessObject(obj, it) }
                        .filter { it.second != null }
                        .map { FieldPreviewItem(it.first, "[used]", it.second) }
                    val matchingFields =
                        (history + listCandidates(obj, searchText, "")).distinctBy { it.fullyQualifiedName }
                    syncToClient(SyncIDs.SEARCH_RESULTS.id) { buffer ->
                        buffer.writeInt(matchingFields.size)
                        matchingFields
                            .map { "${it.fullyQualifiedName},${it.typeName},${it.stringValue}" }
                            .forEach { buffer.writeStringToBuffer(it.take(128)) }
                    }
                }

                SyncIDs.SAVE.id -> {
                    TrackedMetrics.get(syncManager.player.entityWorld).addMetric(
                        TrackedMetric(
                            x,
                            y,
                            z,
                            selectedType,
                            name,
                            searchText.takeIf { selectedType == MetricType.GAUGE }
                                ?: (trigger.takeIf { selectedType == MetricType.COUNTER })
                                ?: "",
                            labels = tagsStringToMap(labelString)
                        )
                    )
                }

                SyncIDs.INIT.id -> {
                    val possibleTriggers = syncManager.player.entityWorld.getTileEntity(x, y, z)?.let {
                        possibleTriggers(it)
                    } ?: emptyList()
                    syncToClient(SyncIDs.INIT_RESULTS.id) { buf ->
                        buf.writeInt(possibleTriggers.size)
                        possibleTriggers.forEach { buf.writeStringToBuffer(it) }
                    }
                }
            }
        }
    }
}
