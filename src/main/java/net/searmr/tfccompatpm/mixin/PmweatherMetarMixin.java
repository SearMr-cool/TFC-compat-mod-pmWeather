package net.searmr.tfccompatpm.mixin;

import dev.protomanly.pmweather.block.MetarBlock;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.data.DataAttachments;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WindEngine;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

// useful method Climate.get(level).getInstantTemperature(level,new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
@Mixin(MetarBlock.class)
public abstract class PmweatherMetarMixin {


    @Shadow
    public Map<UUID, Long> lastInteractions;

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, remap = false)
    protected void useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide) {
            UUID uuid = player.getUUID();
            long lastInteration = (Long)this.lastInteractions.getOrDefault(uuid, -10000L);
            long curTime = level.getGameTime();
            if (curTime - lastInteration > 40L) {
                this.lastInteractions.put(uuid, curTime);
                WeatherHandler weatherHandler = (WeatherHandler) GameBusEvents.MANAGERS.get(level.dimension());
                Vec3 wind = WindEngine.getWind(pos, level);
                int windAngle = Math.floorMod((int)Math.toDegrees(Math.atan2(-wind.x, wind.z)), 360);
                double windspeed = wind.length();
                ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, (RadarBlockEntity)null, 0);
                float riskV = 0.0F;
                float peakRiskOffset = 0.0F;
                float risk2 = 0.0F;
                float risk3 = 0.0F;
                int dayLength = TFCConfig.COMMON.defaultCalendarDayLength.get() * 60 * 20;
                for(int i = 0; i < dayLength; i += 200) {
                    Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    float r = sounding.getRisk(i);
                    if (r > riskV) {
                        riskV = r;
                        peakRiskOffset = (float)i;
                    }
                }

                for(int i = dayLength; i < dayLength * 2; i += 400) {
                    Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    float r = sounding.getRisk(i);
                    if (r > risk2) {
                        risk2 = r;
                    }
                }

                for(int i = dayLength * 2; i < dayLength * 3; i += 800) {
                    Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
                    float r = sounding.getRisk(i);
                    if (r > risk3) {
                        risk3 = r;
                    }
                }

                float temperature = sfc.temperature();
                float dew = sfc.dewpoint();
                CompoundTag data = new CompoundTag();
                data.putString("packetCommand", "Metar");
                data.putString("command", "sendData");
                float moisture = 0.0F;
                ChunkAccess chunkAccess = level.getChunk(pos);
                if (chunkAccess.hasData(DataAttachments.MOISTURE)) {
                    moisture = (Float)chunkAccess.getData(DataAttachments.MOISTURE);
                }

                data.putFloat("temp", temperature);
                data.putFloat("dew", dew);
                data.putFloat("moisture", moisture);
                data.putFloat("day1", riskV);
                data.putFloat("day2", risk2);
                data.putFloat("day3", risk3);
                data.putFloat("peakOffset", peakRiskOffset);
                data.putFloat("windAngle", (float)windAngle);
                data.putDouble("windspeed", windspeed);
                ModNetworking.serverSendToClientPlayer(data, player);
            }
        }

        cir.setReturnValue(InteractionResult.SUCCESS_NO_ITEM_USED);
    }

}
