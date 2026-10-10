package me.collective.module.render;

import me.collective.category.Category;
import me.collective.command.ChatHelper;
import me.collective.module.Module;
import me.collective.render.HandsShaderEngine;
import me.collective.setting.BooleanSetting;
import me.collective.setting.ColorSetting;
import me.collective.setting.EnumSetting;
import me.collective.setting.NumberSetting;

import java.awt.Color;

public class HandsShader extends Module {
    public static HandsShader INSTANCE;

    public enum Mode {
        COSMIC, LIGHTNING, CYBER, HOLOGRAM, GLOW
    }

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Режим", Mode.COSMIC));
    public final NumberSetting opacity = add(new NumberSetting("Непрозрачность", 0.75, 0.0, 1.0, 0.05));
    public final BooleanSetting customColor = add(new BooleanSetting("Кастомный цвет", false));
    public final ColorSetting color = add(new ColorSetting("Цвет 1", new Color(189, 153, 255, 255)));
    public final ColorSetting secondColor = add(new ColorSetting("Цвет 2", new Color(255, 130, 200, 255)));
    public final NumberSetting speed = add(new NumberSetting("Скорость", 1.0, 0.1, 4.0, 0.1));
    public final NumberSetting glow = add(new NumberSetting("Свечение", 1.2, 0.2, 3.0, 0.1));
    public final NumberSetting maxDepth = add(new NumberSetting("Глубина руки", 0.005, 0.0001, 0.02, 0.0002));

    public HandsShader() {
        super("HandsShader", "Накладывает анимированный шейдер на руки и предметы от первого лица", Category.RENDER);
        INSTANCE = this;
    }

    public void render() {
        if (!isToggled() || mc.player == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        Color primary = customColor.getValue() ? color.getValue() : ChatHelper.getPrimaryColor();
        Color secondary = secondColor.getValue();

        int modeIndex = mode.getValue().ordinal();

        HandsShaderEngine.INSTANCE.render(
                primary,
                secondary,
                opacity.getFloatValue(),
                speed.getFloatValue(),
                0.0f,
                maxDepth.getFloatValue(),
                modeIndex,
                glow.getFloatValue()
        );
    }
}
