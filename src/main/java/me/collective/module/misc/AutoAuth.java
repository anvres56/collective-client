package me.collective.module.misc;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.PacketEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.ModeSetting;
import me.collective.setting.StringSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Порт AutoAuth из ThunderHack.
 *
 * <p>Авто-регистрация и авто-логин (/reg, /login) на серверах с AuthMe.
 * ВНИМАНИЕ: пароль хранится в конфиге открытым текстом — перед передачей
 * конфига выключи модуль (он сотрёт пароль)!
 */
public class AutoAuth extends Module {
    public final ModeSetting authMode = add(new ModeSetting("Mode", "Custom", "Custom", "Random", "Qwerty"));
    public final StringSetting password = add(new StringSetting("Password", "changeme"));
    public final BooleanSetting showPassword = add(new BooleanSetting("ShowPassword", true));

    public AutoAuth() {
        super("AutoAuth", "Авто-регистрация и вход (/reg, /login).", Category.MISC);
    }

    @Override
    public void onEnable() {
        warning("Пароль хранится в конфиге! Перед передачей конфига ВЫКЛЮЧИ модуль!");
    }

    @Override
    public void onDisable() {
        info("Стираю пароль...");
        password.setValue("none");
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket pac)) return;
        if (mc.getNetworkHandler() == null) return;

        String pass;
        String accountNick = currentNick();
        String accountPass = accounts().get(accountNick.toLowerCase());
        if (accountPass != null) {
            pass = accountPass;
        } else switch (authMode.compute().trim()) {
            case "Qwerty" -> pass = "qwerty123";
            case "Random" -> pass = randomPassword();
            default -> {
                pass = password.getValue();
                if (pass == null || pass.isEmpty()) {
                    error("Ошибка регистрации: пароль пуст!");
                    return;
                }
            }
        }

        String m = pac.content().getString().toLowerCase();

        if (m.contains("/reg") || m.contains("/register") || m.contains("зарегистрируйтесь")) {
            mc.getNetworkHandler().sendChatCommand("reg " + pass + " " + pass);
            if (showPassword.getValue()) info("Твой пароль: " + pass);
            info("Выполнена регистрация!");
        } else if (m.contains("авторизуйтесь") || m.contains("/l")) {
            mc.getNetworkHandler().sendChatCommand("login " + pass);
            info("Выполнен вход!");
        }
    }

    private static String randomPassword() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append((char) ('a' + rnd.nextInt(26)));
        for (int i = 0; i < 5; i++) sb.append((char) ('!' + rnd.nextInt(90)));
        return sb.toString();
    }

    /** Ник текущего аккаунта. */
    private String currentNick() {
        try {
            return mc.getSession().getUsername();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Пароли по аккаунтам из collective/accounts.txt (строки ник:пароль).
     * Ник из файла имеет приоритет над настройкой Mode.
     */
    private static Map<String, String> accounts() {
        Map<String, String> map = new HashMap<>();
        try {
            Path file = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getGameDir().resolve("collective").resolve("accounts.txt");
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file,
                    "# Ник:пароль — по одному на строку\n# Пример:\n# Steve:qwerty123\n",
                    StandardCharsets.UTF_8);
                return map;
            }
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int sep = line.indexOf(':');
                if (sep <= 0) continue;
                String nick = line.substring(0, sep).strip().toLowerCase();
                String pass = line.substring(sep + 1).strip();
                if (!nick.isEmpty() && !pass.isEmpty()) map.put(nick, pass);
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}
