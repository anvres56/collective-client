package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.mixin.EntityVelocityUpdateS2CPacketAccessor;
import me.collective.mixin.ExplosionS2CPacketAccessor;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class Velocity extends Module {
    public final BooleanSetting knockback = add(new BooleanSetting("Knockback", true));
    public final NumberSetting knockbackHorizontal = add(new NumberSetting("Horizontal", 0.0, 0.0, 1.0, 0.05));
    public final NumberSetting knockbackVertical = add(new NumberSetting("Vertical", 0.0, 0.0, 1.0, 0.05));

    public final BooleanSetting explosions = add(new BooleanSetting("Explosions", true));
    public final NumberSetting explosionsHorizontal = add(new NumberSetting("ExpHorizontal", 0.0, 0.0, 1.0, 0.05));
    public final NumberSetting explosionsVertical = add(new NumberSetting("ExpVertical", 0.0, 0.0, 1.0, 0.05));

    public Velocity() {
        super("Velocity", "Modifies knockback received from damage and explosions.", Category.MOVEMENT);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (knockback.getValue() && event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                double h = knockbackHorizontal.getValue();
                double v = knockbackVertical.getValue();

                if (h == 0.0 && v == 0.0) {
                    event.cancel();
                } else {
                    double velX = (packet.getVelocity().getX() - mc.player.getVelocity().x) * h;
                    double velY = (packet.getVelocity().getY() - mc.player.getVelocity().y) * v;
                    double velZ = (packet.getVelocity().getZ() - mc.player.getVelocity().z) * h;
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setVelocity(
                        new Vec3d(velX + mc.player.getVelocity().x, velY + mc.player.getVelocity().y, velZ + mc.player.getVelocity().z)
                    );
                }
            }
        }

        if (explosions.getValue() && event.getPacket() instanceof ExplosionS2CPacket packet) {
            ExplosionS2CPacketAccessor accessor = (ExplosionS2CPacketAccessor) (Object) packet;
            Optional<Vec3d> kbOpt = accessor.getPlayerKnockback();
            if (kbOpt.isPresent()) {
                double h = explosionsHorizontal.getValue();
                double v = explosionsVertical.getValue();
                if (h == 0.0 && v == 0.0) {
                    accessor.setPlayerKnockback(Optional.empty());
                } else {
                    Vec3d kb = kbOpt.get();
                    accessor.setPlayerKnockback(Optional.of(new Vec3d(kb.x * h, kb.y * v, kb.z * h)));
                }
            }
        }
    }
}
