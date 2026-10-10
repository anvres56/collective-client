package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;

public class Jesus extends Module {
    public final BooleanSetting water = add(new BooleanSetting("Water", true));
    public final BooleanSetting lava = add(new BooleanSetting("Lava", true));

    public Jesus() {
        super("Jesus", "Allows you to walk and bounce on the surface of water and lava.", Category.MOVEMENT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.options.sneakKey.isPressed()) return;

        boolean inWater = water.getValue() && mc.player.isTouchingWater();
        boolean inLava = lava.getValue() && mc.player.isInLava();

        if (inWater || inLava) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.11, mc.player.getVelocity().z);
        }
    }
}
