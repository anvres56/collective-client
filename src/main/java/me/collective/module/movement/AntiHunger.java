package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * Порт AntiHunger из Meteor Client.
 *
 * <p>Режет расход голода: подменяет спринт-пакеты, сервер думает что идёшь.
 * Для миллионных переходов по хайвеям.
 */
public class AntiHunger extends Module {
    public final BooleanSetting spoofSprint = add(new BooleanSetting("Подмена спринта", true));
    public final BooleanSetting spoofJump = add(new BooleanSetting("Подмена прыжка", false));

    public AntiHunger() {
        super("AntiHunger", "Меньше расход голода при беге и прыжках.", Category.MOVEMENT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ClientCommandC2SPacket cmd)) return;
        if (spoofSprint.getValue() && (cmd.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING
            || cmd.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING)) {
            event.cancel();
        }
        if (spoofJump.getValue() && (cmd.getMode() == ClientCommandC2SPacket.Mode.START_RIDING_JUMP
            || cmd.getMode() == ClientCommandC2SPacket.Mode.STOP_RIDING_JUMP)) {
            event.cancel();
        }
    }
}
