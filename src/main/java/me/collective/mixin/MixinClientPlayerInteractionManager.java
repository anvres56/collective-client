package me.collective.mixin;

import me.collective.event.EventBus;
import me.collective.event.events.StartBreakingBlockEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        StartBreakingBlockEvent event = new StartBreakingBlockEvent(blockPos, direction);
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) {
            info.setReturnValue(false);
            info.cancel();
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(net.minecraft.entity.player.PlayerEntity player, net.minecraft.entity.Entity entity, net.minecraft.util.Hand hand, CallbackInfoReturnable<net.minecraft.util.ActionResult> info) {
        me.collective.event.events.InteractEntityEvent event = new me.collective.event.events.InteractEntityEvent(player, entity, hand);
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) {
            info.setReturnValue(net.minecraft.util.ActionResult.FAIL);
            info.cancel();
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(net.minecraft.client.network.ClientPlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hitResult, CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        if (hitResult != null) {
            me.collective.util.tracker.ContainerIndexRecorder.onBlockInteract(hitResult.getBlockPos());
        }
    }
}
