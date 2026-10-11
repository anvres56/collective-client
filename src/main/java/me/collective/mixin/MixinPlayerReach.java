package me.collective.mixin;

import me.collective.module.ModuleManager;
import me.collective.module.combat.Reach;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Часть порта Reach из ThunderHack.
 *
 * <p>Подменяет дальность взаимодействия с блоками и сущностями для себя.
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerReach {

    private boolean reach$applies() {
        Reach mod = ModuleManager.INSTANCE.getModule(Reach.class);
        if (mod == null || !mod.isToggled()) return false;
        return (Object) this == MinecraftClient.getInstance().player;
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void reach$blocks(CallbackInfoReturnable<Double> cir) {
        Reach mod = ModuleManager.INSTANCE.getModule(Reach.class);
        if (mod == null || !mod.isToggled()) return;
        if ((Object) this != MinecraftClient.getInstance().player) return;
        cir.setReturnValue(mod.creative.getValue()
            ? (double) mod.creativeBlocksRange.compute()
            : (double) mod.blocksRange.compute());
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void reach$entities(CallbackInfoReturnable<Double> cir) {
        Reach mod = ModuleManager.INSTANCE.getModule(Reach.class);
        if (mod == null || !mod.isToggled()) return;
        if ((Object) this != MinecraftClient.getInstance().player) return;
        cir.setReturnValue(mod.creative.getValue()
            ? (double) mod.creativeEntityRange.compute()
            : (double) mod.entityRange.compute());
    }
}
