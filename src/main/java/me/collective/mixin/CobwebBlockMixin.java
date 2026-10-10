package me.collective.mixin;

import me.collective.module.movement.NoWeb;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobwebBlock.class)
public class CobwebBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void collective$onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                              EntityCollisionHandler handler, boolean bl, CallbackInfo ci) {
        if (NoWeb.isIgnoreActive() && entity == MinecraftClient.getInstance().player) {
            ci.cancel();
            NoWeb.onEntityCollideCobweb(pos);
        }
    }
}
