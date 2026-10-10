package me.collective.module.render;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    public static Freecam INSTANCE;

    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));

    private Vec3d pos = Vec3d.ZERO;
    private Vec3d prevPos = Vec3d.ZERO;
    private float yaw = 0f;
    private float pitch = 0f;

    public Freecam() {
        super("Freecam", "Detaches the camera from your player allowing free noclip exploration.", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            Vec3d eyePos = mc.player.getEyePos();
            pos = eyePos;
            prevPos = eyePos;
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        pos = Vec3d.ZERO;
        prevPos = Vec3d.ZERO;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        prevPos = pos;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        double s = speed.getValue();
        Vec3d forward = Vec3d.fromPolar(0, yaw).multiply(s);
        Vec3d strafe = Vec3d.fromPolar(0, yaw + 90).multiply(s);

        Vec3d move = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) move = move.add(forward);
        if (mc.options.backKey.isPressed()) move = move.subtract(forward);
        if (mc.options.leftKey.isPressed()) move = move.subtract(strafe);
        if (mc.options.rightKey.isPressed()) move = move.add(strafe);
        if (mc.options.jumpKey.isPressed()) move = move.add(0, s, 0);
        if (mc.options.sneakKey.isPressed()) move = move.subtract(0, s, 0);

        pos = pos.add(move);
    }

    public Vec3d getInterpolatedPos(float tickProgress) {
        if (pos == null || prevPos == null) {
            return mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO;
        }
        double x = MathHelper.lerp(tickProgress, prevPos.x, pos.x);
        double y = MathHelper.lerp(tickProgress, prevPos.y, pos.y);
        double z = MathHelper.lerp(tickProgress, prevPos.z, pos.z);
        return new Vec3d(x, y, z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
