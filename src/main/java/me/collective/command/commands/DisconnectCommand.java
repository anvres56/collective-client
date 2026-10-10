package me.collective.command.commands;

import me.collective.command.Command;
import net.minecraft.text.Text;

public class DisconnectCommand extends Command {
    public DisconnectCommand() {
        super("disconnect", "Disconnects from current server or world.", ".disconnect", "dc", "leave");
    }

    @Override
    public void execute(String[] args) {
        mc.disconnect(Text.literal("Disconnected"));
    }
}
