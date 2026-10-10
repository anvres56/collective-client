package me.collective.scripting;

import me.collective.category.Category;
import me.collective.command.ChatHelper;
import me.collective.command.Command;
import me.collective.command.CommandManager;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import me.collective.module.hud.HudModule;
import me.collective.util.BaritoneBridge;
import me.collective.util.InventoryUtil;
import me.collective.util.PacketUtil;
import me.collective.util.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ScriptContext {
    public final MinecraftClient mc = MinecraftClient.getInstance();
    public final String scriptName;

    private final List<Module> registeredModules = new ArrayList<>();
    private final List<Command> registeredCommands = new ArrayList<>();
    private final List<HudModule> registeredHudElements = new ArrayList<>();

    public ScriptContext(String scriptName) {
        this.scriptName = scriptName;
    }

    // --- Module Registration ---

    public ScriptModule module(String name, Category category, String description) {
        ScriptModule sm = new ScriptModule(name, description, category);
        registeredModules.add(sm);
        ModuleManager.INSTANCE.register(sm);
        return sm;
    }

    public ScriptModule module(String name, Category category) {
        return module(name, category, "Scripted module from " + scriptName);
    }

    public ScriptModule module(String name, String categoryName) {
        Category cat = Category.MISC;
        try {
            cat = Category.valueOf(categoryName.toUpperCase());
        } catch (Exception ignored) {}
        return module(name, cat);
    }

    // --- Custom Chat Commands ---

    public ScriptCustomCommand command(String name, String description, Consumer<String[]> action, String... aliases) {
        ScriptCustomCommand cmd = new ScriptCustomCommand(name, description, action, aliases);
        registeredCommands.add(cmd);
        CommandManager.INSTANCE.register(cmd);
        return cmd;
    }

    public ScriptCustomCommand command(String name, Consumer<String[]> action, String... aliases) {
        return command(name, "Script command from " + scriptName, action, aliases);
    }

    // --- Custom HUD Elements ---

    public ScriptHudElement hud(String name, String description, float defaultX, float defaultY) {
        ScriptHudElement hud = new ScriptHudElement(name, description, defaultX, defaultY);
        registeredHudElements.add(hud);
        ModuleManager.INSTANCE.register(hud);
        return hud;
    }

    public ScriptHudElement hud(String name, float defaultX, float defaultY) {
        return hud(name, "Script HUD element from " + scriptName, defaultX, defaultY);
    }

    // --- Game Automation & Actions API ---

    public void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null && packet != null) {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public void rotateSilent(float yaw, float pitch) {
        RotationUtils.getInstance().setRotationSilent(yaw, pitch);
    }

    public void rotateSilent(float yaw, float pitch, double turnSpeed) {
        RotationUtils.getInstance().setRotationSilent(yaw, pitch, turnSpeed);
    }

    public void rotateInstant(float yaw, float pitch) {
        RotationUtils.getInstance().setRotationSilentInstant(null, 0, yaw, pitch);
    }

    public void rotateTo(Vec3d target) {
        if (mc.player != null) {
            float[] rots = RotationUtils.getLegitRotations(target);
            RotationUtils.getInstance().setRotationSilent(rots[0], rots[1]);
        }
    }

    public void rotateTo(double x, double y, double z) {
        rotateTo(new Vec3d(x, y, z));
    }

    public void pathTo(int x, int y, int z) {
        BaritoneBridge.setGoal(new BlockPos(x, y, z));
    }

    public void cancelPath() {
        BaritoneBridge.clearGoal();
    }

    public void sendChatMessage(String message) {
        if (mc.player != null && message != null) {
            if (message.startsWith("/")) {
                mc.player.networkHandler.sendChatCommand(message.substring(1));
            } else {
                mc.player.networkHandler.sendChatMessage(message);
            }
        }
    }

    public void executeClientCommand(String commandWithPrefix) {
        CommandManager.INSTANCE.execute(commandWithPrefix);
    }

    // --- Output & Logging ---

    public void print(Object msg) {
        ChatHelper.sendMessage(Text.literal("§7[§d" + scriptName + "§7] §f" + String.valueOf(msg)));
    }

    public void log(Object msg) {
        System.out.println("[" + scriptName + "] " + String.valueOf(msg));
    }

    // --- Cleanup & Lifecycle ---

    public void unregisterAll() {
        for (Module m : registeredModules) {
            if (m.isToggled()) m.setToggled(false);
            ModuleManager.INSTANCE.getModules().remove(m);
        }
        registeredModules.clear();

        for (Command c : registeredCommands) {
            CommandManager.INSTANCE.getCommands().remove(c);
        }
        registeredCommands.clear();

        for (HudModule h : registeredHudElements) {
            if (h.isToggled()) h.setToggled(false);
            ModuleManager.INSTANCE.getModules().remove(h);
        }
        registeredHudElements.clear();
    }

    public List<Module> getRegisteredModules() {
        return registeredModules;
    }

    public List<Command> getRegisteredCommands() {
        return registeredCommands;
    }

    public List<HudModule> getRegisteredHudElements() {
        return registeredHudElements;
    }
}
