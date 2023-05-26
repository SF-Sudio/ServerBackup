package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class ReloadCommand implements ServerCommand {
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        ServerBackup.getInstance().reloadConfig();

        ServerBackup.getInstance().stopTimer();
        ServerBackup.getInstance().startTimer();

        String oldDes = ServerBackup.getInstance().backupDestination;

        if (!oldDes
                .equalsIgnoreCase(ServerBackup.getInstance().getConfig().getString("BackupDestination"))) {
            ServerBackup.getInstance().backupDestination = ServerBackup.getInstance().getConfig()
                    .getString("BackupDestination");

            ServerBackup.getInstance().getLogger().log(Level.INFO,
                    "ServerBackup: Backup destination [" + oldDes + " >> "
                            + ServerBackup.getInstance().backupDestination + "] updated successfully.");
        }

        if(ServerBackup.getInstance().cloudInfo.exists()){
            ServerBackup.getInstance().saveCloud();
        }

        ServerBackup.getInstance().loadFiles();

        sender.sendMessage(backup.processMessage("Command.Reload"));
        return false;
    }
}
