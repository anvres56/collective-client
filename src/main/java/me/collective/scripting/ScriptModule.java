package me.collective.scripting;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

public class ScriptModule extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private Consumer<ScriptModule> onEnableAction;
    private Consumer<ScriptModule> onDisableAction;
    private Consumer<ScriptModule> onTickAction;
    private Consumer<PacketEvent.Send> onPacketSendAction;
    private Consumer<PacketEvent.Receive> onPacketReceiveAction;

    public ScriptModule(String name, String description, Category category) {
        super(name, description, category);
    }

    public void onEnable(Consumer<ScriptModule> action) {
        this.onEnableAction = action;
    }

    public void onDisable(Consumer<ScriptModule> action) {
        this.onDisableAction = action;
    }

    public void onTick(Consumer<ScriptModule> action) {
        this.onTickAction = action;
    }

    public void onPacketSend(Consumer<PacketEvent.Send> action) {
        this.onPacketSendAction = action;
    }

    public void onPacketReceive(Consumer<PacketEvent.Receive> action) {
        this.onPacketReceiveAction = action;
    }

    @Override
    public void onEnable() {
        if (onEnableAction != null) onEnableAction.accept(this);
    }

    @Override
    public void onDisable() {
        if (onDisableAction != null) onDisableAction.accept(this);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onTickAction != null && isToggled()) {
            onTickAction.accept(this);
        }
    }

    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (onPacketSendAction != null && isToggled()) {
            onPacketSendAction.accept(event);
        }
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (onPacketReceiveAction != null && isToggled()) {
            onPacketReceiveAction.accept(event);
        }
    }
}
