package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.5, 3.0, 0.5));

    public Step() {
        super("Step", "Increases your step height to walk up full blocks seamlessly.", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        updateStep();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (attr != null) {
                attr.setBaseValue(0.6);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        updateStep();
    }

    private void updateStep() {
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(height.getValue());
        }
    }
}
