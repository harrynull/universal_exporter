package tech.harrynull.universal_exporter.gui

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.UISettings
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
import kotlin.uuid.ExperimentalUuidApi


class MyList : ListWidget<IWidget, MyList>()

class ConfigureUI : IGuiHolder<ConfigureUIData> {
    private enum class SyncIDs(val id: Int) {
        SEARCH_TEXT(0),
        SEARCH_RESULTS(1),
        SAVE(2),
        INIT(3),
        INIT_RESULTS(4),
    }

    lateinit var syncHandler: VariableHintSyncManager
    private var searchText = ""
    private var trigger = ""
    private var possibleTriggers = listOf<String>()
    private var name = ""
    private var selectedType = MetricType.GAUGE
    private var labelString = ""
    private var pagedWidgetController = PagedWidget.Controller()
    lateinit var myList: MyList

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
                                    { searchText },
                                    { newSearchText ->
                                        searchText = newSearchText
                                        syncHandler.syncToServer(SyncIDs.SEARCH_TEXT.id) {
                                            it.writeStringToBuffer(newSearchText)
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
        .addPage(TextWidget("AE items and fluids will be exported"))
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
            if (!player.entityWorld.isRemote) {
                syncHandler.syncToServer(SyncIDs.INIT.id)
            }
        }
        return panel
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
                    }
                }

                SyncIDs.INIT_RESULTS.id -> {
                    val count = buf.readInt()
                    possibleTriggers = (0 until count).map { buf.readStringFromBuffer(128) }
                }
            }
        }


        fun listCandidates(obj: Any?, searchText: String, prefix: String): List<String> {
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
                    "$prefix${firstPart}."
                )
            }

            val matchingFields = allFields.filter {
                !Modifier.isStatic(it.modifiers) &&
                    (searchText.isBlank() || it.name.contains(
                        searchText,
                        ignoreCase = true
                    ))
            }.map { it.isAccessible = true; it }

            return matchingFields.map { field ->
                val value = (field.get(obj) ?: "<null>").takeIf {
                    it is Number || it is Boolean
                }?.toString() ?: "..."
                "${prefix}${field.name},${field.type.simpleName}" to value
            }.sortedBy { (_, value) ->
                if (value == "<null>") 10
                else if (value.firstOrNull()?.isDigit() == true || value.firstOrNull() == '-') 1
                else if (value == "true" || value == "false") 2
                else 5
            }.map { (key, value) ->
                "$key,$value".take(128)
            }
        }

        override fun readOnServer(id: Int, buf: PacketBuffer) {
            when (id) {
                SyncIDs.SEARCH_TEXT.id -> {
                    val searchText = buf.readStringFromBuffer(64)
                    var obj = syncManager.player.entityWorld.getTileEntity(x, y, z) as Any?
                    if (obj != null && obj is BaseMetaTileEntity) {
                        obj = obj.metaTileEntity as Any
                    }
                    val matchingFields = listCandidates(obj, searchText, "")
                    syncToClient(SyncIDs.SEARCH_RESULTS.id) { buffer ->
                        buffer.writeInt(matchingFields.size)
                        matchingFields.forEach { buffer.writeStringToBuffer(it) }
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
