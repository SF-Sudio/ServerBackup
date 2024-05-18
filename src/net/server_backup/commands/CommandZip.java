package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.core.OperationHandler;
import net.server_backup.core.ZipManager;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class CommandZip {

    public static void execute(CommandSender sender, String[] args) {
        String filePath = args[1];

        if (args[1].contains(".zip")) {
            sender.sendMessage(OperationHandler.processMessage("Error.AlreadyZip").replaceAll("%file%", args[1]));
            return;
        }

        File file = new File(Configuration.backupDestination + "//" + filePath);
        File newFile = new File(Configuration.backupDestination + "//" + filePath + ".zip");

        if (!newFile.exists()) {
            sender.sendMessage(OperationHandler.processMessage("Command.Zip.Header"));

            if (file.exists()) {
                try {
                    ZipManager zm = new ZipManager(file.getPath(), newFile.getPath(), sender, true, false,
                            true);

                    zm.zip();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                sender.sendMessage(OperationHandler.processMessage("Error.NoBackupFound").replaceAll("%file%", args[1]));
            }
        } else {
            sender.sendMessage(OperationHandler.processMessage("Error.FolderExists").replaceAll("%file%", args[1]));
        }
    }

}
