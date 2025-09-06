package tech.harrynull.universal_exporter.gui

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.factory.GuiData
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import com.cleanroommc.modularui.value.sync.SyncHandler
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.ListWidget
import com.cleanroommc.modularui.widgets.TextWidget
import com.cleanroommc.modularui.widgets.layout.Flow
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumChatFormatting
import tech.harrynull.universal_exporter.data.TrackedMetric
import tech.harrynull.universal_exporter.data.TrackedMetrics
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class ListAllUI : IGuiHolder<GuiData> {
    private enum class SyncIDs(val id: Int) {
        LIST_ALL(0),
        ANSWER(1),
        DELETE(3);
    }

    private lateinit var list: ListAllUiListWidget

    class ListAllUiListWidget : ListWidget<IWidget, ListAllUiListWidget>()

    val syncHandler = ListAllUISyncHandler()

    override fun buildUI(data: GuiData, syncManager: PanelSyncManager, settings: UISettings): ModularPanel {
        val panel = ModularPanel.defaultPanel("custom", 300, 200)
        panel.child(
            ListAllUiListWidget().also { list = it }
                .sizeRel(1f)
        ).padding(10)
        syncManager.syncValue("sync", syncHandler)
        syncManager.addOpenListener { player ->
            if (!player.entityWorld.isRemote) {
                syncHandler.syncToServer(SyncIDs.LIST_ALL.id)
            }
        }
        return panel
    }

    @OptIn(ExperimentalUuidApi::class)
    inner class ListAllUISyncHandler : SyncHandler() {
        override fun readOnClient(id: Int, buf: PacketBuffer?) {
            when (id) {
                SyncIDs.ANSWER.id -> {
                    val count = buf!!.readInt()
                    for (i in 0 until count) {
                        lateinit var firstLine: TextWidget
                        val metric = TrackedMetric(buf.readNBTTagCompoundFromBuffer()!!)
                        list.child(
                            Flow.column()
                                .child(
                                    TextWidget("[${metric.type}] ${metric.name} at ${metric.x} ${metric.y} ${metric.z}")
                                        .also { firstLine = it }
                                )
                                .child(
                                    TextWidget(metric.value)
                                )
                                .child(
                                    TextWidget("${metric.labels}")
                                )
                                .child(
                                    ButtonWidget().overlay(IKey.str("Delete")).size(60, 15)
                                        .align(Alignment.CenterRight)
                                        .onMouseReleased {
                                            syncToServer(SyncIDs.DELETE.id) { buf ->
                                                buf.writeLong(metric.uuid.toLongs { most, _ -> most })
                                                buf.writeLong(metric.uuid.toLongs { _, least -> least })
                                            }
                                            firstLine.style(EnumChatFormatting.STRIKETHROUGH)
                                            true
                                        }
                                )
                                .padding(5)
                                .height(40)
                                .crossAxisAlignment(Alignment.CrossAxis.START)
                        )
                    }
                }
            }
        }

        override fun readOnServer(id: Int, buf: PacketBuffer?) {
            when (id) {
                SyncIDs.LIST_ALL.id -> {
                    val metrics = TrackedMetrics.get(syncManager.player.worldObj).getMetrics()
                    syncToClient(SyncIDs.ANSWER.id) { buf ->
                        buf!!.writeInt(metrics.size)
                        for (metric in metrics) {
                            buf.writeNBTTagCompoundToBuffer(NBTTagCompound().apply { metric.writeToNBT(this) })
                        }
                    }
                }

                SyncIDs.DELETE.id -> {
                    val most = buf!!.readLong()
                    val least = buf.readLong()
                    val uuid = Uuid.fromLongs(most, least)
                    TrackedMetrics.get(syncManager.player.worldObj).deleteMetric(uuid)
                }
            }
        }
    }
}
