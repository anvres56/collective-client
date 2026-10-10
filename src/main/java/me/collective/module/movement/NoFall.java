package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.util.PacketUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Prevents taking fall damage by resetting fall distance via packet spoofing.", Category.MOVEMENT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (mc.player.fallDistance > 2.5f && !mc.player.isGliding()) {
            PacketUtil.sendSilently(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
        }
    }
}
