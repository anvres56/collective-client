package me.collective.scripting;

import me.collective.command.ChatHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import javax.script.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScriptManager {
    public static final ScriptManager INSTANCE = new ScriptManager();
    private final Path scriptsFolder;
    private final Map<String, ScriptContext> activeContexts = new HashMap<>();
    private ScriptEngineManager engineManager;

    private ScriptManager() {
        this.scriptsFolder = FabricLoader.getInstance().getGameDir().resolve("collective").resolve("scripts");
        try {
            if (!Files.exists(scriptsFolder)) {
                Files.createDirectories(scriptsFolder);
                createExampleScripts();
            }
        } catch (Exception ignored) {}
    }

    public static ScriptManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        this.engineManager = new ScriptEngineManager(getClass().getClassLoader());
        loadAllScripts();
    }

    private void createExampleScripts() {
        try {
            Path exampleKt = scriptsFolder.resolve("AutoJump.kts");
            if (!Files.exists(exampleKt)) {
                String content = """
                    // Kotlin Collective Script Example
                    val myModule = module("AutoJump", me.collective.category.Category.MOVEMENT, "Automatically jumps when moving")
                    
                    myModule.onTick { mod ->
                        if (mc.player != null && mc.player!!.isOnGround && mc.options.forwardKey.isPressed) {
                            mc.player!!.jump()
                        }
                    }
                    
                    print("AutoJump.kts successfully loaded!")
                    """;
                Files.writeString(exampleKt, content);
            }
        } catch (Exception ignored) {}
    }

    public void loadAllScripts() {
        File folder = scriptsFolder.toFile();
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".kts") || name.endsWith(".kt") || name.endsWith(".js") || name.endsWith(".groovy"));
        if (files == null) return;

        for (File file : files) {
            loadScript(file);
        }
    }

    public boolean loadScript(File file) {
        String name = file.getName();
        unloadScript(name);

        String ext = name.substring(name.lastIndexOf('.') + 1);
        ScriptEngine engine = getEngineForExtension(ext);
        if (engine == null) {
            System.err.println("[Collective-Scripting] No script engine found for extension: ." + ext);
            return false;
        }

        ScriptContext ctx = new ScriptContext(name);
        Bindings bindings = engine.createBindings();
        bindings.put("ctx", ctx);
        bindings.put("mc", ctx.mc);
        bindings.put("client", ctx.mc);
        bindings.put("print", (java.util.function.Consumer<Object>) ctx::print);
        bindings.put("log", (java.util.function.Consumer<Object>) ctx::log);
        bindings.put("modules", me.collective.module.ModuleManager.INSTANCE);
        bindings.put("commands", me.collective.command.CommandManager.INSTANCE);
        bindings.put("events", me.collective.event.EventBus.INSTANCE);
        bindings.put("rotations", me.collective.util.RotationUtils.getInstance());
        bindings.put("baritone", me.collective.util.BaritoneBridge.class);
        bindings.put("inv", me.collective.util.InventoryUtil.class);
        bindings.put("fonts", me.collective.font.FontManager.INSTANCE);
        bindings.put("blur", me.collective.render.BlurEngine.INSTANCE);

        try {
            String code = Files.readString(file.toPath());
            engine.eval(code, bindings);
            activeContexts.put(name, ctx);
            System.out.println("[Collective-Scripting] Loaded script: " + name);
            return true;
        } catch (ScriptException e) {
            System.err.println("[Collective-Scripting] Error executing script " + name + ": " + e.getMessage());
            ChatHelper.sendMessage(Text.literal("§c[Script Error] " + name + ": " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("[Collective-Scripting] Failed to read script " + name + ": " + e.getMessage());
        }
        return false;
    }

    private ScriptEngine getEngineForExtension(String ext) {
        if (engineManager == null) engineManager = new ScriptEngineManager(getClass().getClassLoader());
        
        ScriptEngine engine = engineManager.getEngineByExtension(ext);
        if (engine != null) return engine;

        // Try standard Kotlin engine names
        if (ext.equalsIgnoreCase("kts") || ext.equalsIgnoreCase("kt")) {
            engine = engineManager.getEngineByName("kotlin");
            if (engine != null) return engine;
            engine = engineManager.getEngineByName("kts");
            if (engine != null) return engine;
        }

        // Try javascript or groovy as alternative fallbacks
        if (ext.equalsIgnoreCase("js")) return engineManager.getEngineByName("nashorn");
        if (ext.equalsIgnoreCase("groovy")) return engineManager.getEngineByName("groovy");

        return null;
    }

    public boolean unloadScript(String name) {
        String cleanName = name;
        if (!activeContexts.containsKey(cleanName)) {
            for (String key : activeContexts.keySet()) {
                if (key.equalsIgnoreCase(cleanName) || key.startsWith(cleanName + ".")) {
                    cleanName = key;
                    break;
                }
            }
        }

        ScriptContext ctx = activeContexts.remove(cleanName);
        if (ctx != null) {
            ctx.unregisterAll();
            System.out.println("[Collective-Scripting] Unloaded script: " + cleanName);
            return true;
        }
        return false;
    }

    public boolean loadScriptByName(String name) {
        File folder = scriptsFolder.toFile();
        if (!folder.exists() || !folder.isDirectory()) return false;

        File target = new File(folder, name);
        if (target.exists() && target.isFile()) {
            return loadScript(target);
        }

        // Try standard extensions if omitted
        String[] extensions = {".kts", ".kt", ".js", ".groovy"};
        for (String ext : extensions) {
            File f = new File(folder, name + ext);
            if (f.exists() && f.isFile()) {
                return loadScript(f);
            }
        }

        return false;
    }

    public void reload() {
        for (String name : new ArrayList<>(activeContexts.keySet())) {
            unloadScript(name);
        }
        loadAllScripts();
        ChatHelper.sendMessage(Text.literal("§a[Collective-Scripting] Скрипты перезагружены! Активно: §f" + activeContexts.size()));
    }

    public Path getScriptsFolder() {
        return scriptsFolder;
    }

    public Map<String, ScriptContext> getActiveContexts() {
        return Collections.unmodifiableMap(activeContexts);
    }
}
