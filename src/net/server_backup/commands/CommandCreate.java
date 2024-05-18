package net.server_backup.commands;

import com.google.common.io.Files;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.Backup;
import net.server_backup.core.OperationHandler;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class CommandCreate {

    public static void execute(CommandSender sender, String[] args) {
        String fileName = args[1];

        boolean fullBackup = false;

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-full")) {
                    fullBackup = true;
                } else {
                    fileName = fileName + " " + args[i];
                }
            }
        }

        File file = new File(fileName);

        if (!file.isDirectory() && !args[1].equalsIgnoreCase("@server")) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), new Runnable() {

                @Override
                public void run() {
                    try {
                        File des = new File(Configuration.backupDestination + "//Files//"
                                + file.getName().replaceAll("/", "-"));

                        if (des.exists()) {
                            des = new File(des.getPath()
                                    .replaceAll("." + FilenameUtils.getExtension(des.getName()), "") + " "
                                    + String.valueOf(System.currentTimeMillis() / 1000) + "."
                                    + FilenameUtils.getExtension(file.getName()));
                        }

                        Files.copy(file, des);

                        sender.sendMessage(OperationHandler.processMessage("Info.BackupFinished").replaceAll("%file%", args[1]));
                    } catch (IOException e) {
                        sender.sendMessage(OperationHandler.processMessage("Error.BackupFailed").replaceAll("%file%", args[1]));
                        e.printStackTrace();
                    }
                }

            });
        } else {
            Backup backup = new Backup(fileName, sender, fullBackup);

            backup.create();
        }
    }

}
