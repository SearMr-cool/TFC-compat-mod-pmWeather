package net.searmr.tfccompatpm.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.searmr.tfccompatpm.ExampleModClient;
import net.searmr.tfccompatpm.TfcCompatPm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BaseFireBlock.class, priority = 1500)
public abstract class PmwBaseFireOverrideMixin {
    @TargetHandler(
            mixin = "dev.protomanly.pmweather.mixin.BaseFireBlockMixin",
            name = "editOnPlace"
    )

  @Redirect(
          method = "@MixinSquared:Handler",
          at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z")
  )
    private boolean test(BlockState instance, TagKey tagKey) {
        TfcCompatPm.LOGGER.info(Boolean.toString(instance.is(Tags.Blocks.NETHERRACKS) || instance.is(TFCBlocks.PIT_KILN.get())));
        return instance.is(Tags.Blocks.NETHERRACKS) || instance.is(TFCBlocks.PIT_KILN.get());
    }




}
