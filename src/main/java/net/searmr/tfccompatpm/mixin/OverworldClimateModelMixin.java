package net.searmr.tfccompatpm.mixin;

import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.searmr.tfccompatpm.TfcCompatPm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.samplePoint;

@Mixin(OverworldClimateModel.class)
public class OverworldClimateModelMixin {



    @Inject(method = "getInstantRainfall", at = @At("HEAD"),cancellable = true,remap = false)
    private void getInstantRainFallO(LevelReader level, BlockPos pos, long calendarTicks, int daysInMonth, CallbackInfoReturnable<Float> cir)
    {
        Level levelS = (Level) level;
        WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(levelS.dimension());
        int topMostBlock = levelS.getHeight(Heightmap.Types.WORLD_SURFACE,(int)pos.getX(),(int)pos.getZ());
        Vec3 center = new Vec3(pos.getX(),topMostBlock,pos.getZ());
        float rainLevel = weatherHandler.getPrecipitation(center);
        cir.setReturnValue(rainLevel  * 500f);
    }
}
