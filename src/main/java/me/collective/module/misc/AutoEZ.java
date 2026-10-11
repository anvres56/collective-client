package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Порт AutoEZ из ThunderHack.
 *
 * <p>Пишет сообщение в чат за твои киллы.
 */
public class AutoEZ extends Module {
    public final NumberSetting range = add(new NumberSetting("Радиус", 10.0, 2.0, 30.0, 1.0));
    public final BooleanSetting random = add(new BooleanSetting("Случайные", true));

    private static final List<String> MESSAGES = List.of(
        "EZ",
        "покойся с миром",
        " collective client",
        "ты даже не старался",
        "следующий",
        "слишком легко"
    );

    public AutoEZ() {
        super("AutoEZ", "Сообщения в чат за киллы.", Category.MISC);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.isAlive()) continue;
            if (mc.player.distanceTo(p) > range.getFloatValue()) continue;
            if (p.getPrimeAdversary() != mc.player) continue;
            String msg = random.getValue()
                ? MESSAGES.get(ThreadLocalRandom.current().nextInt(MESSAGES.size()))
                : MESSAGES.get(0);
            mc.getNetworkHandler().sendChatMessage(msg);
            break;
        }
    }
}
