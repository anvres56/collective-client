package me.collective.scripting;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.*;
import me.collective.module.Module;
import me.collective.setting.*;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;
import java.util.function.Consumer;

public class ScriptModule extends Module {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private Consumer<ScriptModule> onEnableAction;
    private Consumer<ScriptModule> onDisableAction;
    private Consumer<ScriptModule> onTickAction;
    private Consumer<PacketEvent.Send> onPacketSendAction;
    private Consumer<PacketEvent.Receive> onPacketReceiveAction;
    private Consumer<Render3DEvent> onRender3DAction;
    private Consumer<PlayerMoveEvent> onPlayerMoveAction;
    private Consumer<StartBreakingBlockEvent> onBreakBlockAction;
    private Consumer<SendMessageEvent> onSendMessageAction;
    private Consumer<MouseClickEvent> onMouseClickAction;
    private Consumer<KeyInputEvent> onKeyInputAction;
    private Consumer<InteractEntityEvent> onInteractEntityAction;

    public ScriptModule(String name, String description, Category category) {
        super(name, description, category);
    }

    // --- Fluent Settings API ---

    public BooleanSetting boolSetting(String name, boolean defaultValue) {
        return add(new BooleanSetting(name, defaultValue));
    }

    public NumberSetting numberSetting(String name, double defaultValue, double min, double max, double step) {
        return add(new NumberSetting(name, defaultValue, min, max, step));
    }

    public ColorSetting colorSetting(String name, Color defaultValue) {
        return add(new ColorSetting(name, defaultValue));
    }

    public StringSetting stringSetting(String name, String defaultValue) {
        return add(new StringSetting(name, defaultValue));
    }

    public <T extends Enum<T>> EnumSetting<T> enumSetting(String name, T defaultValue) {
        return add(new EnumSetting<>(name, defaultValue));
    }

    // --- Event Subscriptions ---

    public ScriptModule onEnable(Consumer<ScriptModule> action) {
        this.onEnableAction = action;
        return this;
    }

    public ScriptModule onDisable(Consumer<ScriptModule> action) {
        this.onDisableAction = action;
        return this;
    }

    public ScriptModule onTick(Consumer<ScriptModule> action) {
        this.onTickAction = action;
        return this;
    }

    public ScriptModule onPacketSend(Consumer<PacketEvent.Send> action) {
        this.onPacketSendAction = action;
        return this;
    }

    public ScriptModule onPacketReceive(Consumer<PacketEvent.Receive> action) {
        this.onPacketReceiveAction = action;
        return this;
    }

    public ScriptModule onRender3D(Consumer<Render3DEvent> action) {
        this.onRender3DAction = action;
        return this;
    }

    public ScriptModule onPlayerMove(Consumer<PlayerMoveEvent> action) {
        this.onPlayerMoveAction = action;
        return this;
    }

    public ScriptModule onBreakBlock(Consumer<StartBreakingBlockEvent> action) {
        this.onBreakBlockAction = action;
        return this;
    }

    public ScriptModule onSendMessage(Consumer<SendMessageEvent> action) {
        this.onSendMessageAction = action;
        return this;
    }

    public ScriptModule onMouseClick(Consumer<MouseClickEvent> action) {
        this.onMouseClickAction = action;
        return this;
    }

    public ScriptModule onKeyInput(Consumer<KeyInputEvent> action) {
        this.onKeyInputAction = action;
        return this;
    }

    public ScriptModule onInteractEntity(Consumer<InteractEntityEvent> action) {
        this.onInteractEntityAction = action;
        return this;
    }

    // --- Module Lifecycle ---

    @Override
    public void onEnable() {
        if (onEnableAction != null) onEnableAction.accept(this);
    }

    @Override
    public void onDisable() {
        if (onDisableAction != null) onDisableAction.accept(this);
    }

    // --- Internal Event Dispatchers ---

    @EventHandler
    private void handleTick(TickEvent.Pre event) {
        if (onTickAction != null && isToggled()) {
            onTickAction.accept(this);
        }
    }

    @EventHandler
    private void handleSend(PacketEvent.Send event) {
        if (onPacketSendAction != null && isToggled()) {
            onPacketSendAction.accept(event);
        }
    }

    @EventHandler
    private void handleReceive(PacketEvent.Receive event) {
        if (onPacketReceiveAction != null && isToggled()) {
            onPacketReceiveAction.accept(event);
        }
    }

    @EventHandler
    private void handleRender3D(Render3DEvent event) {
        if (onRender3DAction != null && isToggled()) {
            onRender3DAction.accept(event);
        }
    }

    @EventHandler
    private void handlePlayerMove(PlayerMoveEvent event) {
        if (onPlayerMoveAction != null && isToggled()) {
            onPlayerMoveAction.accept(event);
        }
    }

    @EventHandler
    private void handleBreakBlock(StartBreakingBlockEvent event) {
        if (onBreakBlockAction != null && isToggled()) {
            onBreakBlockAction.accept(event);
        }
    }

    @EventHandler
    private void handleSendMessage(SendMessageEvent event) {
        if (onSendMessageAction != null && isToggled()) {
            onSendMessageAction.accept(event);
        }
    }

    @EventHandler
    private void handleMouseClick(MouseClickEvent event) {
        if (onMouseClickAction != null && isToggled()) {
            onMouseClickAction.accept(event);
        }
    }

    @EventHandler
    private void handleKeyInput(KeyInputEvent event) {
        if (onKeyInputAction != null && isToggled()) {
            onKeyInputAction.accept(event);
        }
    }

    @EventHandler
    private void handleInteractEntity(InteractEntityEvent event) {
        if (onInteractEntityAction != null && isToggled()) {
            onInteractEntityAction.accept(event);
        }
    }
}
