package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

public class ShutdownCommand implements ServerCommand{

    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (backup.shutdownProgress) {
            backup.shutdownProgress = false;

            sender.sendMessage(backup.processMessage("Command.Shutdown.Cancel"));
        } else {
            ServerBackup.getInstance().shutdownProgress = true;

            sender.sendMessage(backup.processMessage("Command.Shutdown.Start"));
        }
        return false;
    }
}
