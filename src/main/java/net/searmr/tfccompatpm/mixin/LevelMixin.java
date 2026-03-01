/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.searmr.tfccompatpm.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.client.ClimateRenderCache;
import net.dries007.tfc.client.overworld.ClientSolarCalculatorBridge;
import net.dries007.tfc.util.tracker.WeatherHelpers;

@Mixin(Level.class)
public abstract class LevelMixin
{
    @Shadow
    @Final
    protected WritableLevelData levelData;

    /**
     * Replace the default rainfall check, which by default only checks the biomes capability for rain, with one
     * that queries the climate model, if it supports that.
     */
    @Inject(
            method = {"isRainingAt"},
            at = {@At("RETURN")},
            cancellable = true
    )
    public void editRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Level level = (Level) this.levelData;
        float rain;
        WeatherHandler weatherHandlerC;
        if (level.isClientSide()) {
            GameBusClientEvents.getClientWeather();
            WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            if (weatherHandler == null) {
                callbackInfoReturnable.setReturnValue(false);
                return;
            }

            weatherHandlerC = weatherHandler;
            rain = weatherHandler.getPrecipitation();
        } else {
            WeatherHandler weatherHandler = (WeatherHandler) GameBusEvents.MANAGERS.get(level.dimension());
            if (weatherHandler == null) {
                callbackInfoReturnable.setReturnValue(false);
                return;
            }

            weatherHandlerC = weatherHandler;
            rain = weatherHandler.getPrecipitation(pos.getCenter());
        }

        if (rain <= 0.15F) {
            callbackInfoReturnable.setReturnValue(false);
        } else if (!level.canSeeSky(pos)) {
            callbackInfoReturnable.setReturnValue(false);
        } else if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            callbackInfoReturnable.setReturnValue(false);
        } else if (ThermodynamicEngine.samplePoint(weatherHandlerC, pos.getCenter(), level, (RadarBlockEntity)null, 0).temperature() <= 0.0F) {
            callbackInfoReturnable.setReturnValue(false);
        } else {
            callbackInfoReturnable.setReturnValue(true);
        }

    }


    /**
     * Replace the client side rain level query only, with one that is aware of the current player position,
     * and is able to linearly interpolate much better.
     */
    @Inject(
            method = {"getRainLevel"},
            at = {@At("RETURN")},
            cancellable = true
    )
    public void editRain(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        Level level = (Level)this.levelData;
        if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
            GameBusClientEvents.getClientWeather();
            callbackInfoReturnable.setReturnValue(((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation());
        } else {
            callbackInfoReturnable.setReturnValue(0.0F);
        }

    }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void getSolarAdjustedDayTimeOnClient(CallbackInfoReturnable<Long> cir)
    {
        final Level level = (Level) (Object) this;
        if (level.isClientSide())
        {
            cir.setReturnValue(ClientSolarCalculatorBridge.getDayTime(level));
        }
    }
}