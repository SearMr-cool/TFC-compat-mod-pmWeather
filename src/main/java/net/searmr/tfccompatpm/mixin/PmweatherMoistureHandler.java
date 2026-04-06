package net.searmr.tfccompatpm.mixin;

import dev.protomanly.pmweather.seasons.MoistureHandler;

import dev.protomanly.pmweather.seasons.SeasonHandler;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MoistureHandler.class)
public class PmweatherMoistureHandler {
    @Inject(method = "getStartMoisture", at = @At("HEAD"),cancellable = true,remap = false)
    private static void getStartMoisture(Level level, ChunkAccess chunk, CallbackInfoReturnable<Float> cir) {
        float humidity = Math.max(Climate.get(level).getAverageRainfall(level,new BlockPos(chunk.getPos().x,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,chunk.getPos().x,chunk.getPos().z),chunk.getPos().z)) / 500f, 0.0F);
        float moisture = 90.0F * Mth.sqrt(humidity);
        cir.setReturnValue(moisture);
    }
}
