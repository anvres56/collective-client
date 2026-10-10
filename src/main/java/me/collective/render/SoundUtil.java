package me.collective.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class SoundUtil {
    public static void playClick(float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Identifier id = Identifier.of("collective", "click");
            SoundEvent event = SoundEvent.of(id);
            mc.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
        } catch (Exception e) {
            mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
        }
    }

    public static void playHover(float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Identifier id = Identifier.of("collective", "hover");
            SoundEvent event = SoundEvent.of(id);
            mc.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
        } catch (Exception ignored) {
        }
    }
}
