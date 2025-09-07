package tech.harrynull.universal_exporter.data

import gregtech.api.metatileentity.BaseMetaTileEntity
import gregtech.api.metatileentity.implementations.MTEBasicMachine
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import java.lang.reflect.Field

fun accessArray(obj: Any, index: Int): Any? {
    return when (obj) {
        is Array<*> -> obj.getOrNull(index)
        is List<*> -> obj.getOrNull(index)
        else -> null
    }
}

fun getAllFieldsIncludingInherited(type: Class<*>?): MutableList<Field> {
    val fields: MutableList<Field> = ArrayList()
    var currentClass = type

    while (currentClass != null) {
        fields.addAll(currentClass.declaredFields)
        currentClass = currentClass.getSuperclass()
    }
    return fields
}

fun accessObject(obj: Any?, accessor: String): Int? {
    if (obj == null) return null
    val accessedObject = accessor.split(".").fold(obj) { acc: Any?, part ->
        if (acc == null) return@fold null
        val partNumeric = part.toIntOrNull()
        if (partNumeric == null) {
            (getAllFieldsIncludingInherited(acc.javaClass).singleOrNull { it.name == part })
                ?.also { it.isAccessible = true }
                ?.get(acc)
        } else {
            accessArray(acc, partNumeric)
        }
    }
    return when (accessedObject) {
        is Number -> accessedObject.toInt()
        is Boolean -> if (accessedObject) 1 else 0
        else -> null
    }
}

fun objectFromWorld(world: World, x: Int, y: Int, z: Int): Any? {
    val obj = world.getTileEntity(x, y, z) as Any?
    if (obj != null && obj is BaseMetaTileEntity) {
        return obj.metaTileEntity as Any
    }
    return obj
}

fun possibleTriggers(te: TileEntity): List<String> {
    val triggers = mutableListOf<String>()
    if (te is BaseMetaTileEntity) {
        val meta = te.metaTileEntity
        if (meta is MTEBasicMachine || meta is MTEMultiBlockBase) {
            triggers.add("machineProcessed")
            triggers.add("machineOutputItems")
            triggers.add("machineOutputFluids")
        }
    }
    return triggers
}
