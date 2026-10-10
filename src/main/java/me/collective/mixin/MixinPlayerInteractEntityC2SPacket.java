package me.collective.mixin;

import me.collective.util.IPlayerInteractEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerInteractEntityC2SPacket.class)
public abstract class MixinPlayerInteractEntityC2SPacket implements IPlayerInteractEntity {
    @Shadow @Final private PlayerInteractEntityC2SPacket.InteractTypeHandler type;
    @Shadow @Final private int entityId;

    @Override
    public PlayerInteractEntityC2SPacket.InteractType collective$getType() {
        return type.getType();
    }

    @Override
    public Entity collective$getEntity() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world != null ? mc.world.getEntityById(entityId) : null;
    }
}
