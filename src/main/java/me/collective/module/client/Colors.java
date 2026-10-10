package me.collective.module.client;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.ColorSetting;
import me.collective.setting.EnumSetting;

import java.awt.Color;

public class Colors extends Module {
    public static Colors INSTANCE;

    public final ColorSetting friendColor = add(new ColorSetting("FriendColor", new Color(85, 255, 85, 255)));
    public final ColorSetting enemyColor = add(new ColorSetting("EnemyColor", new Color(255, 85, 85, 255)));
    public final ColorSetting themeColor = add(new ColorSetting("ThemeColor", new Color(189, 153, 255, 255)));
    public final EnumSetting<Scheme> scheme = add(new EnumSetting<>("Scheme", Scheme.CUSTOM));

    public enum Scheme {
        CUSTOM, COLLECTIVE, PASTEL, RAINBOW, MONOCHROME, RED, BLUE, GREEN,
        PURPLE, CYAN, GOLD, SUNSET, ROSE, EMERALD, NEON_LIME, MIDNIGHT, CHERRY, LAVENDER
    }

    public Colors() {
        super("Colors", "Manages the client's global color system and theme.", Category.CLIENT);
        INSTANCE = this;
        setToggled(true);

        themeColor.setChangeListener(s -> applyTheme());
        scheme.setChangeListener(s -> applyTheme());
    }

    @Override
    public void onEnable() {
        applyTheme();
    }

    @Override
    public void onUpdate() {
        if (!isToggled()) return;
        if (scheme.getValue() == Scheme.RAINBOW) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            Color rainbow = Color.getHSBColor(hue, 0.7f, 1.0f);
            if (UI.INSTANCE != null) {
                UI.INSTANCE.color.setValue(new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), 180));
                UI.INSTANCE.bgEnabled.setValue(new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), 140));
            }
        }
    }

    private boolean applyingTheme = false;

    public void applyTheme() {
        if (UI.INSTANCE == null || applyingTheme) return;
        applyingTheme = true;
        try {
            Color c = themeColor.getValue();
            switch (scheme.getValue()) {
                case CUSTOM -> {
                    UI.INSTANCE.color.setValue(c);
                    UI.INSTANCE.bgEnabled.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), 140));
                }
                case COLLECTIVE -> {
                    Color collectiveColor = new Color(189, 153, 255, 180);
                    themeColor.setValueQuietly(collectiveColor);
                    UI.INSTANCE.color.setValue(collectiveColor);
                    UI.INSTANCE.bgEnabled.setValue(new Color(189, 153, 255, 120));
                }
                case PASTEL -> {
                    Color pastel = new Color(255, 179, 186, 180);
                    themeColor.setValueQuietly(pastel);
                    UI.INSTANCE.color.setValue(pastel);
                    UI.INSTANCE.bgEnabled.setValue(new Color(255, 179, 186, 120));
                }
                case RED -> {
                    Color red = new Color(230, 40, 40, 180);
                    themeColor.setValueQuietly(red);
                    UI.INSTANCE.color.setValue(red);
                    UI.INSTANCE.bgEnabled.setValue(new Color(230, 40, 40, 120));
                }
                case BLUE -> {
                    Color blue = new Color(40, 140, 250, 180);
                    themeColor.setValueQuietly(blue);
                    UI.INSTANCE.color.setValue(blue);
                    UI.INSTANCE.bgEnabled.setValue(new Color(40, 140, 250, 120));
                }
                case GREEN -> {
                    Color green = new Color(50, 220, 90, 180);
                    themeColor.setValueQuietly(green);
                    UI.INSTANCE.color.setValue(green);
                    UI.INSTANCE.bgEnabled.setValue(new Color(50, 220, 90, 120));
                }
                case MONOCHROME -> {
                    Color mono = new Color(200, 200, 200, 180);
                    themeColor.setValueQuietly(mono);
                    UI.INSTANCE.color.setValue(mono);
                    UI.INSTANCE.bgEnabled.setValue(new Color(200, 200, 200, 100));
                }
                case PURPLE -> {
                    Color purple = new Color(168, 85, 247, 180);
                    themeColor.setValueQuietly(purple);
                    UI.INSTANCE.color.setValue(purple);
                    UI.INSTANCE.bgEnabled.setValue(new Color(168, 85, 247, 120));
                }
                case CYAN -> {
                    Color cyan = new Color(6, 182, 212, 180);
                    themeColor.setValueQuietly(cyan);
                    UI.INSTANCE.color.setValue(cyan);
                    UI.INSTANCE.bgEnabled.setValue(new Color(6, 182, 212, 120));
                }
                case GOLD -> {
                    Color gold = new Color(245, 158, 11, 180);
                    themeColor.setValueQuietly(gold);
                    UI.INSTANCE.color.setValue(gold);
                    UI.INSTANCE.bgEnabled.setValue(new Color(245, 158, 11, 120));
                }
                case SUNSET -> {
                    Color sunset = new Color(255, 107, 53, 180);
                    themeColor.setValueQuietly(sunset);
                    UI.INSTANCE.color.setValue(sunset);
                    UI.INSTANCE.bgEnabled.setValue(new Color(255, 107, 53, 120));
                }
                case ROSE -> {
                    Color rose = new Color(244, 63, 94, 180);
                    themeColor.setValueQuietly(rose);
                    UI.INSTANCE.color.setValue(rose);
                    UI.INSTANCE.bgEnabled.setValue(new Color(244, 63, 94, 120));
                }
                case EMERALD -> {
                    Color emerald = new Color(16, 185, 129, 180);
                    themeColor.setValueQuietly(emerald);
                    UI.INSTANCE.color.setValue(emerald);
                    UI.INSTANCE.bgEnabled.setValue(new Color(16, 185, 129, 120));
                }
                case NEON_LIME -> {
                    Color lime = new Color(132, 204, 22, 180);
                    themeColor.setValueQuietly(lime);
                    UI.INSTANCE.color.setValue(lime);
                    UI.INSTANCE.bgEnabled.setValue(new Color(132, 204, 22, 120));
                }
                case MIDNIGHT -> {
                    Color midnight = new Color(99, 102, 241, 180);
                    themeColor.setValueQuietly(midnight);
                    UI.INSTANCE.color.setValue(midnight);
                    UI.INSTANCE.bgEnabled.setValue(new Color(99, 102, 241, 120));
                }
                case CHERRY -> {
                    Color cherry = new Color(220, 38, 38, 180);
                    themeColor.setValueQuietly(cherry);
                    UI.INSTANCE.color.setValue(cherry);
                    UI.INSTANCE.bgEnabled.setValue(new Color(220, 38, 38, 120));
                }
                case LAVENDER -> {
                    Color lavender = new Color(192, 132, 252, 180);
                    themeColor.setValueQuietly(lavender);
                    UI.INSTANCE.color.setValue(lavender);
                    UI.INSTANCE.bgEnabled.setValue(new Color(192, 132, 252, 120));
                }
                default -> {}
            }
        } finally {
            applyingTheme = false;
        }
    }

    public Color getFriendColor() {
        return friendColor.getValue();
    }

    public Color getEnemyColor() {
        return enemyColor.getValue();
    }

    public Color getThemeColor() {
        return themeColor.getValue();
    }
}
