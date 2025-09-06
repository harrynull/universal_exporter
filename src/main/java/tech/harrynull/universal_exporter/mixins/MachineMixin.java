package tech.harrynull.universal_exporter.mixins;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import tech.harrynull.universal_exporter.data.UpdaterKt;

@Mixin(MTEMultiBlockBase.class)
public class MachineMixin {

    @Shadow
    public ItemStack[] mOutputItems;

    @Shadow
    public FluidStack[] mOutputFluids;

    @Inject(
        method = "runMachine(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;J)V",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;mOutputItems:[Lnet/minecraft/item/ItemStack;"),
        remap = false)
    private void machineMixin$injectOutputAfterRecipe(IGregTechTileEntity aBaseMetaTileEntity, long aTick,
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
                    .filter(Objects::nonNull)
                    .map((stack) -> stack.stackSize)
                    .reduce(0, Integer::sum));
        }
        if (mOutputFluids != null) {
            UpdaterKt.updateCounter(
                aBaseMetaTileEntity.getWorld(),
                aBaseMetaTileEntity.getXCoord(),
                aBaseMetaTileEntity.getYCoord(),
                aBaseMetaTileEntity.getZCoord(),
                "machineOutputFluids",
                Arrays.stream(mOutputFluids)
                    .filter(Objects::nonNull)
                    .map((fluid) -> fluid.amount)
                    .reduce(0, Integer::sum));
        }
    }
}
