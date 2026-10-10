package me.collective.command.commands;

import me.collective.command.ChatHelper;
import me.collective.command.Command;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CoordsCommand extends Command {
    public CoordsCommand() {
        super("coords", "Copies current coordinates to clipboard.", ".coords", "c", "coord", "pos");
    }

    @Override
    public void execute(String[] args) {
        if (mc.player == null) return;
        int x = mc.player.getBlockX();
        int y = mc.player.getBlockY();
        int z = mc.player.getBlockZ();
        String coords = x + " " + y + " " + z;
        mc.keyboard.setClipboard(coords);
        ChatHelper.sendMessage(Text.literal("Coordinates copied to clipboard: ").styled(s -> s.withColor(Formatting.GRAY))
            .append(Text.literal(coords).styled(s -> s.withColor(Formatting.GREEN))));
    }
}
