package me.collective.scripting;

import me.collective.category.Category;
import me.collective.command.ChatHelper;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.script.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptContext {
    public final MinecraftClient mc = MinecraftClient.getInstance();
    public final String scriptName;
    private final List<Module> registeredModules = new ArrayList<>();

    public ScriptContext(String scriptName) {
        this.scriptName = scriptName;
    }

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

    public void print(Object msg) {
        ChatHelper.sendMessage(Text.literal("§7[§d" + scriptName + "§7] §f" + String.valueOf(msg)));
    }

    public void log(Object msg) {
        System.out.println("[" + scriptName + "] " + String.valueOf(msg));
    }

    public void unregisterAll() {
        for (Module m : registeredModules) {
            if (m.isToggled()) m.setToggled(false);
            ModuleManager.INSTANCE.getModules().remove(m);
        }
        registeredModules.clear();
    }

    public List<Module> getRegisteredModules() {
        return registeredModules;
    }
}
