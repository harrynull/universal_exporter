package tech.harrynull.universal_exporter.gui

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.factory.AbstractUIFactory
import com.cleanroommc.modularui.factory.GuiData
import com.cleanroommc.modularui.factory.GuiManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.PacketBuffer

class ListAllUIFactory(name: String) : AbstractUIFactory<GuiData>(name) {
    override fun getGuiHolder(data: GuiData): IGuiHolder<GuiData> {
        return ListAllUI()
    }

    fun open(player: EntityPlayerMP) {
        GuiManager.open(this, GuiData(player), player)
    }

    override fun readGuiData(player: EntityPlayer, buffer: PacketBuffer): GuiData {
        return GuiData(player)
    }

    override fun writeGuiData(guiData: GuiData, buffer: PacketBuffer) {
    }

    companion object {
        @JvmField
        val INSTANCE: ListAllUIFactory = ListAllUIFactory("ue:list_ui")
    }
}
