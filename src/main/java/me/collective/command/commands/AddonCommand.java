package me.collective.command.commands;

import me.collective.addon.AddonManager;
import me.collective.addon.CollectiveAddon;
import me.collective.command.ChatHelper;
import me.collective.command.Command;
import net.minecraft.text.Text;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

public class AddonCommand extends Command {
    public AddonCommand() {
        super("addon", "Управление аддонами клиента.", "addons", "аддоны");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<CollectiveAddon> list = AddonManager.getInstance().getAddons();
            if (list.isEmpty()) {
                ChatHelper.sendMessage(Text.literal("§7[§bCollective§7] Установленных аддонов нет. Поместите .jar в папку §fcollective/addons/"));
                return;
            }
            ChatHelper.sendMessage(Text.literal("§7[§bCollective§7] Список установленных аддонов (§f" + list.size() + "§7):"));
            for (CollectiveAddon a : list) {
                ChatHelper.sendMessage(Text.literal(" §8- §e" + a.getName() + " §7v" + a.getVersion() + " §8(автор: §f" + a.getAuthor() + "§8) §7" + a.getDescription()));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            AddonManager.getInstance().reload();
            return;
        }

        if (args[0].equalsIgnoreCase("folder") || args[0].equalsIgnoreCase("open")) {
            File folder = AddonManager.getInstance().getAddonsFolder().toFile();
            try {
                if (!folder.exists()) folder.mkdirs();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(folder);
                    ChatHelper.sendMessage(Text.literal("§7[§bCollective§7] Открыта папка аддонов: §f" + folder.getAbsolutePath()));
                } else {
                    ChatHelper.sendMessage(Text.literal("§7[§bCollective§7] Папка аддонов: §f" + folder.getAbsolutePath()));
                }
            } catch (Exception e) {
                ChatHelper.sendMessage(Text.literal("§cОшибка открытия папки: " + e.getMessage()));
            }
            return;
        }

        ChatHelper.sendMessage(Text.literal("§7Использование: §b.addon <list | reload | folder>"));
    }
}
