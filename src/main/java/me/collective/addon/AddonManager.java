package me.collective.addon;

import me.collective.command.ChatHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.text.Text;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonManager {
    public static final AddonManager INSTANCE = new AddonManager();
    private final List<CollectiveAddon> addons = new ArrayList<>();
    private final Map<CollectiveAddon, URLClassLoader> dynamicLoaders = new HashMap<>();
    private final Path addonsFolder;

    private AddonManager() {
        this.addonsFolder = FabricLoader.getInstance().getGameDir().resolve("collective").resolve("addons");
        try {
            if (!Files.exists(addonsFolder)) {
                Files.createDirectories(addonsFolder);
            }
        } catch (Exception ignored) {}
    }

    public static AddonManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        loadFabricEntrypoints();
        loadDynamicAddons();
    }

    private void loadFabricEntrypoints() {
        for (EntrypointContainer<CollectiveAddon> container : FabricLoader.getInstance().getEntrypointContainers("collective", CollectiveAddon.class)) {
            try {
                CollectiveAddon addon = container.getEntrypoint();
                ModMetadata meta = container.getProvider().getMetadata();

                String author = "Unknown";
                if (!meta.getAuthors().isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (Person p : meta.getAuthors()) names.add(p.getName());
                    author = String.join(", ", names);
                }

                String website = meta.getContact().get("sources").or(() -> meta.getContact().get("homepage")).orElse("");
                addon.initMetadata(meta.getName(), meta.getVersion().getFriendlyString(), author, meta.getDescription(), website);

                addon.onInitialize();
                addons.add(addon);
                System.out.println("[Collective] Loaded Fabric entrypoint addon: " + addon.getName() + " v" + addon.getVersion());
            } catch (Throwable t) {
                System.err.println("[Collective] Failed to load Fabric addon: " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    public void loadDynamicAddons() {
        File dir = addonsFolder.toFile();
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File jar : files) {
            loadJarAddon(jar);
        }
    }

    public boolean loadJarAddon(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());
            Class<?> targetClass = null;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                    String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                    try {
                        Class<?> cls = Class.forName(className, false, loader);
                        if (CollectiveAddon.class.isAssignableFrom(cls) && !cls.isInterface() && !java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
                            targetClass = cls;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            if (targetClass != null) {
                CollectiveAddon addon = (CollectiveAddon) targetClass.getDeclaredConstructor().newInstance();
                AddonInfo info = targetClass.getAnnotation(AddonInfo.class);
                if (info != null) {
                    addon.initMetadata(info.name(), info.version(), info.author(), info.description(), info.website());
                } else {
                    addon.initMetadata(targetClass.getSimpleName(), "1.0.0", "Dynamic Addon", "", "");
                }

                addon.onInitialize();
                addons.add(addon);
                dynamicLoaders.put(addon, loader);
                System.out.println("[Collective] Loaded dynamic addon from JAR: " + addon.getName() + " v" + addon.getVersion());
                return true;
            } else {
                loader.close();
            }
        } catch (Throwable t) {
            System.err.println("[Collective] Failed to load jar addon " + jarFile.getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
        return false;
    }

    public void reload() {
        for (CollectiveAddon addon : new ArrayList<>(addons)) {
            try {
                addon.onShutdown();
            } catch (Throwable ignored) {}
            URLClassLoader loader = dynamicLoaders.remove(addon);
            if (loader != null) {
                try {
                    loader.close();
                } catch (Exception ignored) {}
            }
        }
        addons.clear();
        init();
        ChatHelper.sendMessage(Text.literal("§a[Collective] Аддоны успешно перезагружены! Загружено: §f" + addons.size()));
    }

    public List<CollectiveAddon> getAddons() {
        return Collections.unmodifiableList(addons);
    }

    public Path getAddonsFolder() {
        return addonsFolder;
    }
}
