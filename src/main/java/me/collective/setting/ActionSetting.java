package me.collective.setting;

import java.util.function.Supplier;

public class ActionSetting extends Setting<String> {
    public ActionSetting(String name, int dummy) {
        super(name, "");
    }

    public ActionSetting process(String text) {
        return this;
    }

    public ActionSetting handle(Runnable action) {
        return this;
    }

    public ActionSetting handle(Supplier<Boolean> hidden) {
        return this;
    }
}
