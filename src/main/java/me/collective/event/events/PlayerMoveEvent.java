package me.collective.event.events;

import me.collective.event.CancellableEvent;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class PlayerMoveEvent extends CancellableEvent {
    public final MovementType type;
    public Vec3d movement;

    public PlayerMoveEvent(MovementType type, Vec3d movement) {
        this.type = type;
        this.movement = movement;
    }
}
