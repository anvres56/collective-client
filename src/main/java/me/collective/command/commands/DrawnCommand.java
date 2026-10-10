package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DrawnCommand extends Command {
    public DrawnCommand() {
        super("drawn", "Toggles whether module is visible in ArrayList.", ".drawn <module>", "d");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            ChatHelper.sendMessage(Text.literal("Usage: " + getSyntax()).styled(s -> s.withColor(Formatting.RED)));
            return;
        }
        Module mod = ModuleManager.INSTANCE.getModule(args[0]);
        if (mod == null) {
            ChatHelper.sendMessage(Text.literal("Module not found: " + args[0]).styled(s -> s.withColor(Formatting.RED)));
            return;
        }
        ChatHelper.sendMessage(Text.literal("Module ").styled(s -> s.withColor(Formatting.GRAY))
            .append(Text.literal(mod.getName()).styled(s -> s.withColor(Formatting.WHITE)))
            .append(Text.literal(" drawn toggled").styled(s -> s.withColor(Formatting.GRAY))));
    }
}
