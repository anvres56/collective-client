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
        super("scripts", "Управление Kotlin и Java скриптами клиента.", "script", "скрипты");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Map<String, ?> list = ScriptManager.getInstance().getActiveContexts();
            if (list.isEmpty()) {
                ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] Активных скриптов нет. Поместите .kts файлы в §fcollective/scripts/"));
                return;
            }
            ChatHelper.sendMessage(Text.literal("§7[§dCollective§7] Активные скрипты (§f" + list.size() + "§7):"));
            for (String name : list.keySet()) {
                ChatHelper.sendMessage(Text.literal(" §8- §e" + name));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            ScriptManager.getInstance().reload();
            return;
        }

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

        ChatHelper.sendMessage(Text.literal("§7Использование: §b.scripts <list | reload | folder>"));
    }
}
