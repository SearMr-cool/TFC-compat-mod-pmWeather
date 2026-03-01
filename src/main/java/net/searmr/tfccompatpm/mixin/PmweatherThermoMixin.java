package net.searmr.tfccompatpm.mixin;

import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.seasons.SeasonHandler;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WindEngine;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.searmr.tfccompatpm.TfcCompatPm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.*;

@Mixin(ThermodynamicEngine.class)
public class PmweatherThermoMixin {
    @Inject(method = "samplePoint(Ldev/protomanly/pmweather/weather/WeatherHandler;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;Ldev/protomanly/pmweather/block/entity/RadarBlockEntity;ILjava/lang/Integer;ZZ)Ldev/protomanly/pmweather/weather/ThermodynamicEngine$AtmosphericDataPoint;", at = @At("HEAD"),cancellable = true)
    private static void samplePoint(WeatherHandler weatherHandler, Vec3 pos, Level level, RadarBlockEntity radarBlockEntity, int advance, Integer groundHeight, boolean doFireAffect, boolean stormsAffect, CallbackInfoReturnable<ThermodynamicEngine.AtmosphericDataPoint> cir) {
        BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
        ThermodynamicEngine.noise = WindEngine.simplexNoise;
        if (ThermodynamicEngine.noise == null) {
            cir.setReturnValue( new AtmosphericDataPoint(30.0F, 30.0F, 1013.0F, 30.0F));
        } else {
            float time = (float) (level.getDayTime() + (long) advance);
            float biomeTemp = 0.0F;
            float humidity = 0.0F;
            int c = 0;
            boolean cached = false;
            if (cachedPos != null && cachedPos.equals(pos.multiply((double) 1.0F, (double) 0.0F, (double) 1.0F)) && Math.abs(time - cachedTime) < 20.0F) {
                biomeTemp = cachedBiomeTemp;
                humidity = cachedHumidity;
                cached = true;
            } else {
                for (int x = -1; x <= 1; ++x) {
                    for (int z = -1; z <= 1; ++z) {
                        if (Mth.abs(x) != 1 || Mth.abs(z) != 1) {
                            ++c;
                            BlockPos p = blockPos.offset(new Vec3i(x * 64, 0, z * 64));
                            Holder<Biome> biome;
                            if (radarBlockEntity != null && radarBlockEntity.init) {
                                biome = radarBlockEntity.getNearestBiome(p);
                            } else {
                                biome = level.getBiome(p);
                            }

                            biomeTemp += ((Biome) biome.value()).getBaseTemperature();
                            humidity += Math.max(((Biome) biome.value()).getModifiedClimateSettings().downfall(), 0.0F);
                        }
                    }
                }

                humidity /= (float) c;
                biomeTemp /= (float) c;
                cachedPos = pos.multiply((double) 1.0F, (double) 0.0F, (double) 1.0F);
                cachedBiomeTemp = biomeTemp;
                cachedHumidity = humidity;
                cachedTime = time;
            }

            biomeTemp -= 0.15F;
            if (groundHeight == null) {
                groundHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
            }

            int elevation = Math.max(level.getSeaLevel(), groundHeight);
            ChunkAccess chunkAccess = level.getChunk(blockPos);
            Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
            float gBiomeTemp = ((Biome) biome.value()).getBaseTemperature();
            float gHumidity = Math.max(((Biome) biome.value()).getModifiedClimateSettings().downfall(), 0.0F);
            humidity = Mth.lerp(Math.clamp((float) pos.y() / 16000.0F, 0.0F, 0.15F), humidity, gHumidity);
            biomeTemp = Mth.lerp(Math.clamp((float) pos.y() / 16000.0F, 0.0F, 0.15F), biomeTemp, gBiomeTemp - 0.15F);
            if (humidity > 0.4F) {
                humidity -= 0.4F;
                humidity /= 2.0F;
                humidity += 0.4F;
            }

            humidity = (float) Math.pow((double) humidity, (double) 0.3F);
            int elevationSeaLevel = elevation - level.getSeaLevel();
            float aboveSeaLevel = (float) pos.y() - (float) level.getSeaLevel();
            float altitude = Math.max((float) pos.y() - (float) elevation, 0.0F);
            float daytime = (float) (level.getDayTime() + (long) advance) / 24000.0F;
            double x = ((double) daytime - 0.18) * Math.PI * (double) 2.0F;
            double timeFactor = Math.sin(x + Math.sin(x) / (double) -2.0F);
            float pblHeight;
            if (cached) {
                pblHeight = cachedPBLHeight;
            } else {
                pblHeight = FBM(pos.multiply((double) (1.0F / xzScale), (double) 0.0F, (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 2, 2.0F, 0.5F, 1.0F);
            }

            cachedPBLHeight = pblHeight;
            pblHeight = (Math.clamp(pblHeight + 1.0F, 0.0F, 2.0F) + 1.0F) * 500.0F;
            double timeFactorHeightAffected = Mth.lerp((double) Math.clamp(altitude / pblHeight, 0.0F, 1.0F), timeFactor, (double) 1.0F);
            float sfcPressure = 1013.25F;
            float sfcTNoise;
            if (cached) {
                sfcTNoise = cachedSfcTNoise;
            } else {
                sfcTNoise = FBM(pos.multiply((double) (1.0F / xzScale), (double) 0.0F, (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 3, 2.0F, 0.5F, 1.0F);
            }

            cachedSfcTNoise = sfcTNoise;
            sfcTNoise *= 5.0F;
            float sfcTemp= Climate.get(level).getInstantTemperature(level,new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));;

            float sfcTempTimeMod = (float) timeFactorHeightAffected * 5.0F * Math.max(1.0F - humidity, 0.05F);
            sfcTempTimeMod += 5.0F;
            float tNoise = sfcTNoise / 5.0F;
            if (ServerConfig.doSeasons) {
                float seasonEffect = SeasonHandler.getSeasonEffectSine(level, 0.0F);
                if (seasonEffect > 0.0F) {
                    seasonEffect /= 4.0F;
                }

                if (humidity > 0.75F) {
                    seasonEffect *= 1.0F - (humidity - 0.75F) * 4.0F;
                }
            }

            float fireIntensity = 0.0F;
            if (doFireAffect) {
                Tuple<Float, Float> rtrn = getFireTemperature(level, chunkAccess, pos, groundHeight);
                sfcTemp += (Float) rtrn.getA();
                fireIntensity = (Float) rtrn.getB();
            }

            float pNoise;
            if (cached) {
                pNoise = cachedPNoise;
            } else {
                pNoise = FBM(pos.multiply((double) (1.0F / -xzScale), (double) 0.0F, (double) (1.0F / -xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 3, 2.0F, 0.5F, 1.0F);
            }

            cachedPNoise = pNoise;
            sfcPressure += pNoise * 7.0F;
            float stormCooling = 0.0F;
            if (stormsAffect) {
                for (Storm storm : weatherHandler.getStorms()) {
                    stormCooling += storm.getTemperatureOffset(pos);
                }
            }

            stormCooling *= 1.0F - Math.clamp((float) advance / 12000.0F, 0.0F, 1.0F);
            sfcTemp -= stormCooling * Math.clamp(1.0F - altitude / 3000.0F, 0.0F, 1.0F);
            float var88;
            if (humidity > 0.5F) {
                var88 = (float) Math.pow((double) (2.0F * (humidity - 0.5F)), (double) 0.25F) + 0.5F;
            } else {
                var88 = (float) Math.pow((double) (2.0F * humidity), (double) 4.0F) * 0.5F;
            }

            float dewP = Mth.clamp((float) Mth.lerp((double) 0.7F, (ThermodynamicEngine.noise.getValue(pos.z / (double) 2200.0F, (double) (time / 9000.0F) + pos.y / (double) 100.0F, pos.x / (double) 300.0F) + (double) 1.0F) / (double) 2.0F, (double) var88), 0.2F, 1.0F);
            float sfcDew = Math.min(sfcTemp, 32.0F) - Math.clamp((1.0F - dewP) * (sfcTemp), 0.0F, 15.0F);
            if (sfcDew > 0.0F) {
                sfcDew *= humidity * 0.9F + 0.1F;
            }

            sfcDew -= Mth.lerp(1.0F - var88, 0.0F, 5.0F);
            sfcDew -= Mth.square(fireIntensity) * 3.0F;
            if (sfcDew < -10.0F) {
                sfcDew = -10.0F;
            }

            if (ServerConfig.doSeasons) {
                float seasonEffect = SeasonHandler.getSeasonEffectSine(level, 0.0F);
                if (seasonEffect > 0.0F) {
                    seasonEffect /= 7.0F;
                }

                if (seasonEffect < 0.0F && humidity > 0.75F) {
                    seasonEffect *= 1.0F - (humidity - 0.75F) * 4.0F;
                }

                sfcDew += seasonEffect * 8.0F;
                float humidEffect = SeasonHandler.getSeasonEffectSine(weatherHandler.getWorld(), 3.5F) + 1.0F;
                if (humidity > 0.75F) {
                    humidEffect *= 1.0F - (humidity - 0.75F) * 4.0F;
                }

                sfcDew -= humidEffect * 5.0F;
            }

            sfcDew = Math.min(sfcDew, sfcTemp);
            sfcPressure = getPressureAtHeight((float) elevationSeaLevel, sfcTemp, sfcPressure);
            float lapseRate = 5.5F;
            float lrNoise = tNoise;
            if (tNoise > 0.0F) {
                lrNoise = (float) Math.pow((double) tNoise, (double) 1.25F);
                lrNoise *= 2.0F;
            }

            lapseRate += lrNoise;
            lapseRate *= 0.4F + (1.0F - humidity);
            float dewRatio = Mth.lerp((tNoise + 1.0F) / 2.0F, Mth.lerp(humidity, 0.4F, 0.1F), Mth.lerp(humidity, 0.65F, 0.3F));
            float var90 = sfcTemp;
            float var93 = sfcDew;
            float noise;
            if (cached) {
                noise = cachedNoise;
            } else {
                noise = FBM(pos.multiply((double) (1.0F / xzScale), (double) 0.0F, (double) (1.0F / -xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 2, 2.0F, 0.5F, 1.0F);
            }

            cachedNoise = noise;
            float bumpH = (float) elevation + Math.clamp(noise + 0.5F, 0.5F, 1.5F) * 1250.0F;
            noise = FBM(pos.multiply((double) (1.0F / -xzScale), (double) 0.0F, (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 2, 2.0F, 0.5F, 1.0F);
            float bumpStrength = Math.clamp(noise + 0.5F, 0.0F, 1.5F) * 5.5F * Math.clamp(1.0F - humidity, 0.0F, 1.0F);
            bumpStrength -= 4.0F * humidity;
            if (altitude > bumpH) {
                float i = Math.clamp((altitude - bumpH) / 150.0F, 0.0F, 1.0F);
                var93 -= Mth.lerp(i, 0.0F, bumpStrength);
            }

            float a = Math.clamp(altitude, 0.0F, 1000.0F);

            var93 -= lapseRate * dewRatio * 0.25F;
            noise = FBM(pos.multiply((double) (1.0F / xzScale), (double) 0.0F, (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / timeScale), (double) 0.0F), 2, 2.0F, 0.5F, 1.0F);
            float inversionHeight = (float) elevationSeaLevel + Mth.lerp(Math.clamp(noise, 0.0F, 1.0F), 12000.0F, 16000.0F);
            if (altitude > inversionHeight) {
                float dif = altitude - inversionHeight;
                float i = Math.clamp(dif / 1500.0F, 0.0F, 1.0F);

                var93 += Mth.lerp(i, 0.0F, lapseRate * (dif / 1000.0F) * dewRatio);
            }

            float offset = FBM(pos.multiply((double) (1.0F / xzScale), (double) (1.0F / yScale), (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / -timeScale), (double) 0.0F), 4, 2.0F, 0.5F, 1.0F);
            offset *= 1.5F;
            var93 -= offset * 1.5F;
            float p = getPressureAtHeight(aboveSeaLevel, var90, (float) elevationSeaLevel, sfcPressure);
            float dewMin = FBM(pos.multiply((double) (1.0F / xzScale), (double) (1.0F / yScale), (double) (1.0F / xzScale)).add((double) 0.0F, (double) (time / -timeScale), (double) 0.0F), 4, 2.0F, 0.5F, 1.0F);
            dewMin = Math.clamp(dewMin + 1.0F, 0.0F, 2.0F) * 2.0F;
            dewMin += (float) Math.pow(pos.y / (double) 16000.0F, (double) 2.0F) * 40.0F * (1.0F - humidity);
            float td = var90 - dewMin;
            if (var93 > td) {
                float dif = var93 - td;
                var93 -= dif * Math.clamp(dif / 4.0F, 0.0F, 1.0F);
            }

            var93 = Math.min(var90, var93);
            cir.setReturnValue( new AtmosphericDataPoint(var90, var93, p, calcVTemp(var90, var93, sfcPressure)));
        }
    }
}
