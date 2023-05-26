package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.BackupManager;
import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

public class RemoveCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if ( args.length != 2) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup remove <file/folder>");
            return false;
        }
        if (args[1].equalsIgnoreCase("Files")) {
            sender.sendMessage("You can not delete the 'Files' backup folder.");

            return false;
        }

        BackupManager bm = new BackupManager(args[1], sender, true);

        bm.removeBackup();
        return false;
    }
}
