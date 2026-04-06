package net.searmr.tfccompatpm.mixin;

import dev.protomanly.pmweather.data.DataAttachments;
import dev.protomanly.pmweather.seasons.MoistureHandler;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.dries007.tfc.common.blocks.soil.FarmlandBlock.getHydrationMultiplier;
import static net.dries007.tfc.common.blocks.soil.FarmlandBlock.isSourceBlockPresent;


@Mixin(FarmlandBlock.class)
public class TFCFarmLandMixin {
    @Inject(method = "getInstantHydrationFromRainHydration", at = @At("HEAD"),cancellable = true,remap = false)
   private static void GetInstantHydrationFromRainHydration(Level level, BlockPos pos, int rainHydration, CallbackInfoReturnable<Integer> cir) {
        if (Helpers.isFluid(level.getFluidState(pos.above()), TFCTags.Fluids.HYDRATING))
        {
            cir.setReturnValue(100); // special case for waterlogged crops
        }
        float moisture = 0f;
        var chunk = level.getChunk(pos.getX(),pos.getZ());
        if (chunk.hasData(DataAttachments.MOISTURE)) {
            moisture = (Float)chunk.getData(DataAttachments.MOISTURE);
        }
        moisture /= 100;
        moisture *= 60;
        final int waterBoost = isSourceBlockPresent(level, pos) ? 40 : 0;
        final float soilMultiplier = getHydrationMultiplier(level, pos);

        cir.setReturnValue(Math.clamp((int) ((waterBoost + moisture) * soilMultiplier), 0, 100));
    }
    }

