package tech.harrynull.universal_exporter.items

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import tech.harrynull.universal_exporter.gui.ConfigureUIFactory
import tech.harrynull.universal_exporter.gui.ListAllUIFactory

class DataWand : Item() {
    init {
        this.setUnlocalizedName("data_wand")
        this.setTextureName("universal_exporter:data_wand")
        this.setMaxStackSize(1)
    }

    override fun onItemUse(
        stack: ItemStack,
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        hitX: Float,
        hitY: Float,
        hitZ: Float
    ): Boolean {
        if (!world.isRemote) {
            player.swingItem()
            ConfigureUIFactory.INSTANCE.open(player as EntityPlayerMP, x, y, z)
        }
        return true
    }

    override fun onItemRightClick(itemStackIn: ItemStack?, world: World?, player: EntityPlayer?): ItemStack? {
        if (world?.isRemote == false) {
            ListAllUIFactory.INSTANCE.open(player as EntityPlayerMP)
        }
        return super.onItemRightClick(itemStackIn, world, player)
    }
}

