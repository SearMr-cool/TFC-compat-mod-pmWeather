package net.searmr.tfccompatpm.mixin;


import dev.protomanly.pmweather.block.PMWFireBlock;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PMWFireBlock.class)
public class PmweatherFireBlockMixin {
    @Inject(method = "isGroundSuitable", at = @At("HEAD"),cancellable = true,remap = false)
    private static void isGroundSuitable(Level level, BlockState state, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is(net.minecraft.world.level.block.Blocks.FARMLAND) || state.is(net.minecraft.world.level.block.Blocks.PODZOL) || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH) || state.is(TFCTags.Blocks.GRASS));
    }
}
