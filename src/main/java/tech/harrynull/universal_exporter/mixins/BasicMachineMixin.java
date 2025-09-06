package tech.harrynull.universal_exporter.mixins;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import tech.harrynull.universal_exporter.data.UpdaterKt;

@Mixin(MTEBasicMachine.class)
public class BasicMachineMixin {

    @Final
    @Shadow
    public ItemStack[] mOutputItems;

    @Shadow
    public FluidStack mOutputFluid;

    @Inject(
        method = "onPostTick(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;J)V",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;mOutputItems:[Lnet/minecraft/item/ItemStack;",
            ordinal = 0),
        remap = false)
    private void basicMachineMixin$injectOutputAfterRecipe(IGregTechTileEntity aBaseMetaTileEntity, long aTick,
        CallbackInfo ci) {
        UpdaterKt.updateCounter(
            aBaseMetaTileEntity.getWorld(),
            aBaseMetaTileEntity.getXCoord(),
            aBaseMetaTileEntity.getYCoord(),
            aBaseMetaTileEntity.getZCoord(),
            "machineProcessed",
            1);
        if (mOutputItems != null) {
            UpdaterKt.updateCounter(
                aBaseMetaTileEntity.getWorld(),
                aBaseMetaTileEntity.getXCoord(),
                aBaseMetaTileEntity.getYCoord(),
                aBaseMetaTileEntity.getZCoord(),
                "machineOutputItems",
                Arrays.stream(mOutputItems)
                    .map((stack) -> stack.stackSize)
                    .reduce(0, Integer::sum));
        }
        if (mOutputFluid != null) {
            UpdaterKt.updateCounter(
                aBaseMetaTileEntity.getWorld(),
                aBaseMetaTileEntity.getXCoord(),
                aBaseMetaTileEntity.getYCoord(),
                aBaseMetaTileEntity.getZCoord(),
                "machineOutputFluids",
                mOutputFluid.amount);
        }
    }
}
