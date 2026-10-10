package me.collective.module.render;

import me.collective.category.Category;
import me.collective.module.Module;

public class NoHurtCam extends Module {
    public static NoHurtCam INSTANCE;

    public NoHurtCam() {
        super("NoHurtCam", "Disables camera shake when taking damage.", Category.RENDER);
        INSTANCE = this;
    }
}
