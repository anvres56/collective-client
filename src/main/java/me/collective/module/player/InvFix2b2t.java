package me.collective.module.player;

import me.collective.category.Category;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;

public class InvFix2b2t extends Module {
    public final BooleanSetting fixGhostItems = add(new BooleanSetting("FixGhostItems", true));
    public final BooleanSetting fixBundles = add(new BooleanSetting("FixBundles", true));

    public InvFix2b2t() {
        super("InvFix2b2t", "Fixes 2b2t ghost item bugs and desynced bundle item order.", Category.PLAYER);
    }
}
