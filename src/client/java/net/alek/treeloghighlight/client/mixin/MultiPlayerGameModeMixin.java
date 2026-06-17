package net.alek.treeloghighlight.client.mixin;

import net.alek.treeloghighlight.client.TreeLogHighlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level != null) {
            // ONLY start a highlight if we are actually clicking a log
            if (minecraft.level.getBlockState(pos).is(BlockTags.LOGS)) {
                TreeLogHighlightManager.highlightTree(minecraft.level, pos);
            }
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void onStopDestroyBlock(CallbackInfo ci) {
        // Removed TreeLogHighlightManager.clear() to keep highlighting visible while holding/breaking
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onBlockDestroyed(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level != null) {
            // Check if the block being destroyed was a log OR was already highlighted
            boolean wasLog = minecraft.level.getBlockState(pos).is(BlockTags.LOGS);
            boolean wasHighlighted = TreeLogHighlightManager.isHighlighted(pos);

            // ONLY recalculate if we broke a log that was part of our tree
            if (wasLog || wasHighlighted) {
                // Trigger a search from neighbors to update the remaining logs in the tree
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (minecraft.level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                        TreeLogHighlightManager.highlightTree(minecraft.level, neighbor);
                    }
                }
            }
        }
    }
}