package net.searmr.tfccompatpm.mixin;

import net.dries007.tfc.world.chunkdata.ChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkData.class)
public class ChunkDataMixin {
//    @Inject(method = "getAverageRainfall(II)F", at = @At("HEAD"), cancellable = true)
//    private void AverageRainO(int x, int z, CallbackInfoReturnable<Float> cir) {
//
//    }
}
