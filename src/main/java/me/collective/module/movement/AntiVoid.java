package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Порт AntiVoid из ThunderHack.
 *
 * <p>Не даёт упасть в пустоту: гасит падение над войдом. Для городов Края.
 */
public class AntiVoid extends Module {
    public final NumberSetting fallDistance = add(new NumberSetting("Дистанция падения", 3.0, 1.0, 10.0, 0.5));
    public final NumberSetting height = add(new NumberSetting("Высота срабатывания", 0.0, -64.0, 64.0, 1.0));
    public final BooleanSetting jump = add(new BooleanSetting("Авто-прыжок", true));

    public AntiVoid() {
        super("AntiVoid", "Спасает от падения в пустоту.", Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.player.isOnGround()) return;
        if (mc.player.getY() > height.getFloatValue()) return;
        if (mc.player.fallDistance < fallDistance.getFloatValue()) return;
        if (!isOverVoid()) return;

        // Гасим падение, тянем вверх
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x * 0.7, Math.max(vel.y, -0.15), vel.z * 0.7);
        if (jump.getValue()) mc.options.jumpKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        if (mc.options != null) mc.options.jumpKey.setPressed(false);
    }

    private boolean isOverVoid() {
        BlockPos pos = mc.player.getBlockPos();
        for (int y = pos.getY(); y > mc.world.getBottomY() - 10; y--) {
            if (!mc.world.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isAir()) return false;
        }
        return true;
    }
}
