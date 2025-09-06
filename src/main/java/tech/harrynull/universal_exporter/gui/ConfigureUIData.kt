package tech.harrynull.universal_exporter.gui

import com.cleanroommc.modularui.factory.GuiData
import net.minecraft.entity.player.EntityPlayer

class ConfigureUIData(
    player: EntityPlayer,
    val x: Int,
    val y: Int,
    val z: Int,
    val aeEnabled: Boolean,
) : GuiData(player)
