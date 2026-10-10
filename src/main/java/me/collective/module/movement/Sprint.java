package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;

public class Sprint extends Module {
    public final BooleanSetting omni = add(new BooleanSetting("Omni", false));

    public Sprint() {
        super("Sprint", "Automatically sprints whenever you are moving.", Category.MOVEMENT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (omni.getValue()) {
            if (mc.player.input.hasForwardMovement() || Math.abs(mc.player.input.getMovementInput().x) > 0) {
                mc.player.setSprinting(true);
            }
        } else {
            if (mc.player.input.hasForwardMovement() && !mc.player.horizontalCollision) {
                mc.player.setSprinting(true);
            }
        }
    }
}
