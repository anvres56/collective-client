package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PitchCommand extends Command {
    public PitchCommand() {
        super("pitch", "Sets player camera pitch.", ".pitch <degrees>");
    }

    @Override
    public void execute(String[] args) {
        if (mc.player == null || args.length < 1) {
            ChatHelper.sendMessage(Text.literal("Usage: " + getSyntax()).styled(s -> s.withColor(Formatting.RED)));
            return;
        }
        try {
            float p = Float.parseFloat(args[0]);
            mc.player.setPitch(p);
            ChatHelper.sendMessage(Text.literal("Pitch set to ").styled(s -> s.withColor(Formatting.GRAY))
                .append(Text.literal(String.valueOf(p)).styled(s -> s.withColor(Formatting.WHITE))));
        } catch (Exception e) {
            ChatHelper.sendMessage(Text.literal("Invalid pitch number.").styled(s -> s.withColor(Formatting.RED)));
        }
    }
}
