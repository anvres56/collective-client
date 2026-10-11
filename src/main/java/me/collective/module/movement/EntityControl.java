package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Порт EntityControl из ThunderHack.
 *
 * <p>Управление ездовыми: разворот за взглядом + ускорение.
 */
public class EntityControl extends Module {
    public final NumberSetting speed = add(new NumberSetting("Ускорение", 0.5, 0.0, 2.0, 0.1));
    public final BooleanSetting yawSync = add(new BooleanSetting("Разворот за взглядом", true));

    public EntityControl() {
        super("EntityControl", "Управление лошадьми, свиньями и лодками.", Category.MOVEMENT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;

        if (yawSync.getValue()) {
            vehicle.setYaw(mc.player.getYaw());
        }
        if (mc.options.forwardKey.isPressed() && speed.getFloatValue() > 0) {
            Vec3d look = mc.player.getRotationVec(1.0f);
            Vec3d v = vehicle.getVelocity();
            double s = speed.getFloatValue() * 0.15;
            vehicle.setVelocity(v.x + look.x * s, v.y, v.z + look.z * s);
        }
    }
}
