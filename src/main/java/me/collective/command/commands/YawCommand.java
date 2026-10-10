package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class YawCommand extends Command {
    public YawCommand() {
        super("yaw", "Sets player camera yaw.", ".yaw <degrees>");
    }

    @Override
    public void execute(String[] args) {
        if (mc.player == null || args.length < 1) {
            ChatHelper.sendMessage(Text.literal("Usage: " + getSyntax()).styled(s -> s.withColor(Formatting.RED)));
            return;
        }
        try {
            float y = Float.parseFloat(args[0]);
            mc.player.setYaw(y);
            ChatHelper.sendMessage(Text.literal("Yaw set to ").styled(s -> s.withColor(Formatting.GRAY))
                .append(Text.literal(String.valueOf(y)).styled(s -> s.withColor(Formatting.WHITE))));
        } catch (Exception e) {
            ChatHelper.sendMessage(Text.literal("Invalid yaw number.").styled(s -> s.withColor(Formatting.RED)));
        }
    }
}
