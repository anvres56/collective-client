package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.emotes.EmoteEngine;
import me.collective.emotes.EmoteWheelScreen;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.KeybindSetting;

/**
 * Порт PlayerEmotes (Колесо эмоций) из декомпиля.
 *
 * <p>Держишь бинд "Emote menu" — открывается колесо {@link EmoteWheelScreen},
 * отпускаешь — колесо закрывается и проигрывает выбранный эмоут само.
 */
public class EmotesModule extends Module {
    public static EmotesModule INSTANCE;

    public final KeybindSetting emoteMenu = add(new KeybindSetting("Emote menu", -1));
    public final BooleanSetting keepWhileMoving = add(new BooleanSetting("Keep While Moving", false));
    public final BooleanSetting debugLog = add(new BooleanSetting("Debug", true));

    public EmotesModule() {
        super("Emotes", "Колесо эмоций: зажми бинд, выбери эмоут и отпусти.", Category.MISC);
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        me.collective.emotes.EmoteEngine.tick();
        if (emoteMenu.isPressed()) {
            if (mc.currentScreen == null
                && me.collective.emotes.EmoteEngine.millisSincePlay() > 500) {
                mc.setScreen(new EmoteWheelScreen());
            }
        } else {
            // Отпустил бинд — играем подсвеченный эмоут (как в оригинале)
            if (mc.currentScreen instanceof EmoteWheelScreen wheel) {
                me.collective.emotes.Emote selected = wheel.getSelectedEmote();
                mc.setScreen(null);
                if (selected != null) me.collective.emotes.EmoteEngine.play(selected);
            }
        }
    }

    @Override
    public void onDisable() {
        EmoteEngine.stop();
        if (mc.currentScreen instanceof EmoteWheelScreen) {
            mc.setScreen(null);
        }
    }

    public boolean keepWhileMoving() {
        return keepWhileMoving.getValue();
    }
}
