package me.collective.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Смена сессии для AltManager. */
@Mixin(MinecraftClient.class)
public interface MixinMinecraftClient {
    @Mutable
    @Accessor("session")
    void setSession(Session session);
}
