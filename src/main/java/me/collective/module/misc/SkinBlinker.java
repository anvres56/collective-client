package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.NumberSetting;
import net.minecraft.entity.player.PlayerModelPart;

/**
 * Порт SkinBlinker из ThunderHack.
 *
 * <p>Мигает слоями скина. Фан.
 */
public class SkinBlinker extends Module {
    public final NumberSetting delay = add(new NumberSetting("Задержка", 5.0, 1.0, 40.0, 1.0));

    private int timer = 0;
    private int partIndex = 0;
    private static final PlayerModelPart[] PARTS = PlayerModelPart.values();

    public SkinBlinker() {
        super("SkinBlinker", "Мигание слоями скина.", Category.MISC);
    }

    @Override
    public void onUpdate() {
        if (mc.options == null) return;
        if (timer++ < delay.getIntValue()) return;
        timer = 0;
        PlayerModelPart part = PARTS[partIndex % PARTS.length];
        partIndex++;
        mc.options.setPlayerModelPart(part, !mc.options.isPlayerModelPartEnabled(part));
    }
}
