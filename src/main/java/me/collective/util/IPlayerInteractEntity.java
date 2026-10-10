package me.collective.util;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public interface IPlayerInteractEntity {
    PlayerInteractEntityC2SPacket.InteractType collective$getType();
    Entity collective$getEntity();
}
