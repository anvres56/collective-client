package me.collective.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.collective.command.ChatHelper;
import me.collective.mixin.MixinMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Менеджер альтов (оффлайн-ники для анархий).
 */
public class AltManager {
    public static final AltManager INSTANCE = new AltManager();

    private final Path altsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Alt> alts = new ArrayList<>();

    private AltManager() {
        this.altsFile = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getGameDir().resolve("collective").resolve("alts.json");
        load();
    }

    public static AltManager getInstance() {
        return INSTANCE;
    }

    public List<Alt> getAlts() {
        return alts;
    }

    public synchronized void load() {
        alts.clear();
        try {
            if (!Files.exists(altsFile)) return;
            JsonObject root = gson.fromJson(Files.readString(altsFile, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("alts")) return;
            for (JsonElement e : root.getAsJsonArray("alts")) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("name") && o.has("uuid")) {
                    alts.add(new Alt(o.get("name").getAsString(), o.get("uuid").getAsString()));
                }
            }
        } catch (Exception e) {
            System.err.println("[Collective-Alts] Load failed: " + e.getMessage());
        }
    }

    public synchronized boolean save() {
        try {
            Files.createDirectories(altsFile.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Alt alt : alts) {
                JsonObject o = new JsonObject();
                o.addProperty("name", alt.name());
                o.addProperty("uuid", alt.uuid());
                arr.add(o);
            }
            root.add("alts", arr);
            Files.writeString(altsFile, gson.toJson(root), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            System.err.println("[Collective-Alts] Save failed: " + e.getMessage());
            return false;
        }
    }

    /** Оффлайн-UUID по нику (конвенция ваниллы). */
    public static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public boolean add(String name) {
        if (name == null || name.isBlank() || name.length() > 16) return false;
        for (Alt alt : alts) {
            if (alt.name().equalsIgnoreCase(name)) return false;
        }
        alts.add(new Alt(name, offlineUuid(name)));
        return save();
    }

    public boolean remove(Alt alt) {
        boolean removed = alts.remove(alt);
        if (removed) save();
        return removed;
    }

    public String currentName() {
        try {
            return MinecraftClient.getInstance().getSession().getUsername();
        } catch (Exception e) {
            return "?";
        }
    }

    /** Переключает сессию на альт (нужен релог на сервер). */
    public boolean login(Alt alt) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Session session = new Session(alt.name(), UUID.fromString(alt.uuid()),
                "", Optional.empty(), Optional.empty());
            ((MixinMinecraftClient) mc).setSession(session);
            ChatHelper.sendMessage(Text.literal("§a[Alts] Теперь ты §e" + alt.name()
                + " §7(перезайди на сервер)"));
            return true;
        } catch (Exception e) {
            System.err.println("[Collective-Alts] Login failed: " + e.getMessage());
            ChatHelper.sendMessage(Text.literal("§c[Alts] Не удалось переключить аккаунт"));
            return false;
        }
    }
}
