package me.collective.module.movement;

import me.collective.category.Category;
import me.collective.module.Module;

public class SafeWalk extends Module {
    public SafeWalk() {
        super("SafeWalk", "Prevents you from walking off the edge of blocks.", Category.MOVEMENT);
    }
}
