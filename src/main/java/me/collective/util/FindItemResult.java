package me.collective.util;

import net.minecraft.util.Hand;

public record FindItemResult(int slot, int count) {
    public boolean found() {
        return slot != -1;
    }

    public boolean isOffhand() {
        return slot == 40;
    }

    public boolean isHotbar() {
        return slot >= 0 && slot < 9;
    }

    public boolean isMain() {
        return slot >= 9 && slot < 36;
    }

    public Hand getHand() {
        if (slot == 40) return Hand.OFF_HAND;
        return Hand.MAIN_HAND;
    }
}
