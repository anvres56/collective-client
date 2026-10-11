package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import me.collective.scripting.ScriptManager;
import net.minecraft.text.Text;

import java.awt.Desktop;
import java.io.File;
import java.util.Map;

public class ScriptCommand extends Command {
    public ScriptCommand() {
        super("scripts", "Управление Kotlin и Java скриптами клиента.", ".scripts <list|load|unload|reload|folder>", "script", "скрипты");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Map<String, ?> list = ScriptManager.getInstance().getActiveContexts();
            File folder = ScriptManager.getInstance().getScriptsFolder().toFile();
            File[] allFiles = folder.listFiles((dir, name) -> name.endsWith(".kts") || name.endsWith(".kt") || name.endsWith(".js") || name.endsWith(".groovy"));

            if ((allFiles == null || allFiles.length == 0) && list.isEmpty()) {
                ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] В папке §fcollective/scripts/ §7нет скриптов."));
                return;
            }

            ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] Скрипты (Активно: §a" + list.size() + "§7):"));
            if (allFiles != null) {
                for (File file : allFiles) {
                    boolean active = list.containsKey(file.getName());
                    String status = active ? "§a[АКТИВЕН]§r" : "§c[ВЫКЛЮЧЕН]§r";
                    ChatHelper.sendMessage(Text.literal(" §8- " + status + " §e" + file.getName()));
                }
            }
            return;
        }

        // Загрузка отдельного скрипта
        if (args[0].equalsIgnoreCase("load")) {
            if (args.length < 2) {
                ChatHelper.sendMessage(Text.literal("§cИспользование: .scripts load <имя_скрипта>"));
                return;
            }
            String scriptName = args[1];
            boolean success = ScriptManager.getInstance().loadScriptByName(scriptName);
            if (success) {
                ChatHelper.sendMessage(Text.literal("§a[Collective] Скрипт §e" + scriptName + " §aуспешно загружен и активирован!"));
            } else {
                ChatHelper.sendMessage(Text.literal("§c[Collective] Не удалось найти или загрузить скрипт: §e" + scriptName));
            }
            return;
        }

        // Выгрузка отдельного скрипта
        if (args[0].equalsIgnoreCase("unload")) {
            if (args.length < 2) {
                ChatHelper.sendMessage(Text.literal("§cИспользование: .scripts unload <имя_скрипта>"));
                return;
            }
            String scriptName = args[1];
            boolean success = ScriptManager.getInstance().unloadScript(scriptName);
            if (success) {
                ChatHelper.sendMessage(Text.literal("§a[Collective] Скрипт §e" + scriptName + " §aуспешно выгружен!"));
            } else {
                ChatHelper.sendMessage(Text.literal("§c[Collective] Активный скрипт не найден: §e" + scriptName));
            }
            return;
        }

        // Перезагрузка всех скриптов
        if (args[0].equalsIgnoreCase("reload")) {
            ScriptManager.getInstance().reload();
            return;
        }

        // Открытие папки
        if (args[0].equalsIgnoreCase("folder") || args[0].equalsIgnoreCase("open")) {
            File folder = ScriptManager.getInstance().getScriptsFolder().toFile();
            try {
                if (!folder.exists()) folder.mkdirs();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(folder);
                    ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] Открыта папка скриптов: §f" + folder.getAbsolutePath()));
                } else {
                    ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] Папка скриптов: §f" + folder.getAbsolutePath()));
                }
            } catch (Exception e) {
                ChatHelper.sendMessage(Text.literal("§cОшибка открытия папки: " + e.getMessage()));
            }
            return;
        }

        ChatHelper.sendMessage(Text.literal("§7Использование: §b.scripts <list | load <name> | unload <name> | reload | folder>"));
    }
}
