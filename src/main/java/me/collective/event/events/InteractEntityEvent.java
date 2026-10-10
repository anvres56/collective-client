package me.collective.event.events;

import me.collective.event.CancellableEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class InteractEntityEvent extends CancellableEvent {
    public final PlayerEntity player;
    public final Entity entity;
    public final Hand hand;

    public InteractEntityEvent(PlayerEntity player, Entity entity, Hand hand) {
        this.player = player;
        this.entity = entity;
        this.hand = hand;
    }
}
