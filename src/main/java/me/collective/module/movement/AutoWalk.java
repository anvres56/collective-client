package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.ModeSetting;

/**
 * Порт AutoWalk из ThunderHack.
 *
 * <p>Держит ходьбу за тебя: руки свободны на хайвеях.
 */
public class AutoWalk extends Module {
    public final ModeSetting direction = add(new ModeSetting("Направление", "Forward", "Forward", "Back", "Left", "Right"));
    public final BooleanSetting sprint = add(new BooleanSetting("Спринт", true));
    public final BooleanSetting jump = add(new BooleanSetting("Прыжки", false));

    public AutoWalk() {
        super("AutoWalk", "Автоматическая ходьба вперёд.", Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.options == null) return;
        releaseAll();
        switch (direction.compute().trim()) {
            case "Back" -> mc.options.backKey.setPressed(true);
            case "Left" -> mc.options.leftKey.setPressed(true);
            case "Right" -> mc.options.rightKey.setPressed(true);
            default -> mc.options.forwardKey.setPressed(true);
        }
        mc.player.setSprinting(sprint.getValue());
        if (jump.getValue() && mc.player.isOnGround() && mc.player.horizontalCollision) {
            mc.player.jump();
        }
    }

    @Override
    public void onDisable() {
        releaseAll();
    }

    private void releaseAll() {
        if (mc.options == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }
}
