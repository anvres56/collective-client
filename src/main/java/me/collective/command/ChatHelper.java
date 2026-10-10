package me.collective.command;

import me.collective.module.client.Colors;
import me.collective.module.client.UI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.awt.Color;

public class ChatHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Text getPrefix() {
        Color primary = getPrimaryColor();
        TextColor primaryTc = TextColor.fromRgb(primary.getRGB() & 0xFFFFFF);
        TextColor bracketTc = TextColor.fromFormatting(Formatting.DARK_GRAY);

        MutableText prefix = Text.empty();
        prefix.append(Text.literal("[").styled(s -> s.withColor(bracketTc)));
        prefix.append(Text.literal("Collective").styled(s -> s.withColor(primaryTc).withBold(false)));
        prefix.append(Text.literal("]").styled(s -> s.withColor(bracketTc)));
        return prefix;
    }

    public static Color getPrimaryColor() {
        if (Colors.INSTANCE != null && Colors.INSTANCE.themeColor != null) {
            return Colors.INSTANCE.themeColor.getValue();
        }
        if (UI.INSTANCE != null && UI.INSTANCE.color != null) {
            return UI.INSTANCE.color.getValue();
        }
        return new Color(189, 153, 255);
    }

    public static TextColor getPrimaryTextColor() {
        return TextColor.fromRgb(getPrimaryColor().getRGB() & 0xFFFFFF);
    }

    public static void sendMessage(Text text) {
        MutableText full = Text.empty().append(getPrefix()).append(" ").append(text);
        if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            mc.inGameHud.getChatHud().addMessage(full);
        } else if (mc.player != null) {
            mc.player.sendMessage(full, false);
        }
    }

    public static void sendMessage(String text) {
        sendMessage(Text.literal(text).styled(s -> s.withColor(Formatting.GRAY)));
    }
}
