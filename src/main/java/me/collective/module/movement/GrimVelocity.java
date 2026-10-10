package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.event.events.TickEvent;
import me.collective.mixin.MixinClientConnection;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GrimVelocity extends Module {
    public final NumberSetting packetInterval = add(new NumberSetting("Interval", 2.0, 1.0, 20.0, 1.0));

    private int ccCooldown;
    private boolean flag;

    public GrimVelocity() {
        super("GrimVelocity", "GrimAC velocity/movement bypass: replaces move packets with PosRot and STOP_DESTROY_BLOCK", Category.MOVEMENT);
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket)) return;

        event.cancel();
        this.flag = true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (this.ccCooldown > 0) {
            this.ccCooldown--;
        }

        if (this.flag) {
            this.handleNewGrimTick();
        }
    }

    private void handleNewGrimTick() {
        if (this.ccCooldown <= 0) {
            me.collective.util.PacketUtil.sendSilently(new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                false
            ));

            me.collective.util.PacketUtil.sendSilently(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                Direction.DOWN
            ));

            this.ccCooldown = this.packetInterval.getValue().intValue();
        }

        this.flag = false;
    }

    @Override
    public void onDisable() {
        this.flag = false;
        this.ccCooldown = 0;
    }
}
