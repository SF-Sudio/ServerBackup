package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ZipManager;
import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.io.File;

public class UnzipCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args.length != 2) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup unzip <file>");
        }
        String filePath = args[1];

        if (!args[1].contains(".zip")) {
            sender.sendMessage(backup.processMessage("Error.NotAZip").replaceAll("%file%", args[1]));
            return false;
        }

        File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
        File newFile = new File(
                ServerBackup.getInstance().backupDestination + "//" + filePath.replaceAll(".zip", ""));

        if (!newFile.exists()) {
            sender.sendMessage(backup.processMessage("Command.Unzip.Header"));

            if (file.exists()) {
                ZipManager zm = new ZipManager(file.getPath(),
                        ServerBackup.getInstance().backupDestination + "//" + newFile.getName(), sender,
                        false, true, true);

                zm.unzip();
            } else {
                sender.sendMessage(backup.processMessage("Error.NoBackupFound").replaceAll("%file%", args[1]));
            }
        } else {
            sender.sendMessage(backup.processMessage("Error.ZipExists").replaceAll("%file%", args[1]));
        }
        return false;
    }
}
