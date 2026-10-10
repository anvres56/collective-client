package me.collective.util;

import net.minecraft.client.MinecraftClient;

public class ChatUtils {
    public static void sendPlayerMsg(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        if (msg.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(msg.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(msg);
        }
    }
}
