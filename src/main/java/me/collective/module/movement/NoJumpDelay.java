package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.module.Module;

public class NoJumpDelay extends Module {
    public static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("NoJumpDelay", "Removes the delay between consecutive jumps.", Category.MOVEMENT);
        INSTANCE = this;
    }
}
