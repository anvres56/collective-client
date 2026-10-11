package me.collective.mixin;

import me.collective.module.ModuleManager;
import me.collective.module.player.MultiTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Часть порта MultiTask из Meteor Client.
 *
 * <p>{@code MinecraftClient.handleBlockBreaking} не даёт копать во время
 * использования предмета (еды). Подменяем проверку — можно есть и копать
 * одновременно.
 */
@Mixin(MinecraftClient.class)
public abstract class MixinMultiTask {

    @Redirect(
        method = "handleBlockBreaking",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z")
    )
    private boolean allowBreakingWhileUsing(ClientPlayerEntity player) {
        MultiTask mod = ModuleManager.INSTANCE.getModule(MultiTask.class);
        if (mod != null && mod.isToggled()) return false;
        return player.isUsingItem();
    }
}
