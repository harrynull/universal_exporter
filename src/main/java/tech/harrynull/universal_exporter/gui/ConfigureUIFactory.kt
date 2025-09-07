package tech.harrynull.universal_exporter.gui

import appeng.me.helpers.IGridProxyable
import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.factory.AbstractUIFactory
import com.cleanroommc.modularui.factory.GuiManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.PacketBuffer

class ConfigureUIFactory(name: String) : AbstractUIFactory<ConfigureUIData>(name) {
    init {
        GuiManager.registerFactory(this)
    }

    override fun getGuiHolder(data: ConfigureUIData): IGuiHolder<ConfigureUIData> {
        return ConfigureUI()
    }

    fun open(player: EntityPlayerMP, x: Int, y: Int, z: Int) {
        val tile = player.entityWorld.getTileEntity(x, y, z)
        val aeEnabled = tile is IGridProxyable && tile.proxy != null
        GuiManager.open(this, ConfigureUIData(player, x, y, z, aeEnabled), player)
    }

    override fun readGuiData(player: EntityPlayer, buffer: PacketBuffer): ConfigureUIData {
        return ConfigureUIData(player, buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean())
    }

    override fun writeGuiData(guiData: ConfigureUIData, buffer: PacketBuffer) {
        buffer.writeInt(guiData.x)
        buffer.writeInt(guiData.y)
        buffer.writeInt(guiData.z)
        buffer.writeBoolean(guiData.aeEnabled)
    }

    companion object {
        @JvmField
        val INSTANCE: ConfigureUIFactory = ConfigureUIFactory("configure_ui")
    }
}
