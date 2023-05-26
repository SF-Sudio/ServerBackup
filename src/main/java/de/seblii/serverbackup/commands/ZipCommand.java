package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import de.seblii.serverbackup.ZipManager;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class ZipCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args.length < 2) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup zip <folder>");
            return false;
        }
        String filePath = args[1];

        if (args[1].contains(".zip")) {
            sender.sendMessage(backup.processMessage("Error.AlreadyZip").replaceAll("%file%", args[1]));
            return false;
        }

        File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
        File newFile = new File(ServerBackup.getInstance().backupDestination + "//" + filePath + ".zip");

        if (!newFile.exists()) {
            sender.sendMessage(backup.processMessage("Command.Zip.Header"));

            if (file.exists()) {
                ZipManager zm = new ZipManager(file.getPath(), newFile.getPath(), sender, true, false, true);

                zm.zip();
            } else {
                sender.sendMessage(backup.processMessage("Error.NoBackupFound").replaceAll("%file%", args[1]));
            }
        } else {
            sender.sendMessage(backup.processMessage("Error.FolderExists").replaceAll("%file%", args[1]));
        }
        return false;
    }
}
