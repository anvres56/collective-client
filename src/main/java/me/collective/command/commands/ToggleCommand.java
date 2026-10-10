package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import me.collective.module.Module;
import me.collective.module.ModuleManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggles a module on or off.", ".toggle <module> [state]", "t");
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

        if (args.length >= 2) {
            boolean state = Boolean.parseBoolean(args[1]);
            mod.setToggled(state);
        } else {
            mod.toggle();
        }

        MutableText msg = Text.literal("Module ").styled(s -> s.withColor(Formatting.GRAY))
            .append(Text.literal(mod.getName()).styled(s -> s.withColor(ChatHelper.getPrimaryTextColor())))
            .append(Text.literal(" is now ").styled(s -> s.withColor(Formatting.GRAY)))
            .append(Text.literal(mod.isToggled() ? "enabled" : "disabled").styled(s -> s.withColor(mod.isToggled() ? Formatting.GREEN : Formatting.RED)));

        ChatHelper.sendMessage(msg);
    }
}
