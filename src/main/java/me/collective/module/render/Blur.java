package me.collective.module.render;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;

public class Blur extends Module {
    public static Blur INSTANCE;

    public final NumberSetting radius = add(new NumberSetting("Radius", 8, 1, 20, 1));
    public final BooleanSetting clickGui = add(new BooleanSetting("ClickGUI", true));
    public final BooleanSetting categories = add(new BooleanSetting("Categories", true));
    public final BooleanSetting settings = add(new BooleanSetting("Settings", true));
    public final BooleanSetting tooltips = add(new BooleanSetting("Tooltips", true));
    public final BooleanSetting hud = add(new BooleanSetting("HUD", false));

    public Blur() {
        super("Blur", "Blurs the background in GUI's, categories and settings.", Category.RENDER);
        INSTANCE = this;
        setToggled(true);
    }
}
