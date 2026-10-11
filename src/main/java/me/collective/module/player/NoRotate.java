package me.collective.module.player;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.module.Module;
import me.collective.util.RotationUtils;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

/**
 * Порт NoRotate из ThunderHack.
 *
 * <p>Игнорирует принудительные развороты от сервера, возвращает свой угол.
 */
public class NoRotate extends Module {
    public NoRotate() {
        super("NoRotate", "Блокирует развороты от сервера.", Category.PLAYER);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerPositionLookS2CPacket)) return;
        RotationUtils rot = RotationUtils.getInstance();
        RotationUtils.setServerRotation(rot.getSentYaw(), rot.getSentPitch());
    }
}
