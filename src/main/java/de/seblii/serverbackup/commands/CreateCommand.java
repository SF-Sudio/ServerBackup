package de.seblii.serverbackup.commands;

import com.google.common.io.Files;
import de.seblii.serverbackup.BackupManager;
import de.seblii.serverbackup.ServerBackup;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class CreateCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args.length < 2) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup create <world/@server> [-full]");
            return false;
        }
        StringBuilder fileName = new StringBuilder(args[1]);

        boolean fullBackup = false;
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-full")) {
                    fullBackup = true;
                } else {
                    fileName.append(" ").append(args[i]);
                }
            }
        }

        File file = new File(fileName.toString());

        if (!file.isDirectory() && !args[1].equalsIgnoreCase("@server")) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                try {
                    File des = new File(ServerBackup.getInstance().backupDestination + "//Files//"
                            + file.getName().replaceAll("/", "-"));

                    if (des.exists()) {
                        des = new File(des.getPath()
                                .replaceAll("." + FilenameUtils.getExtension(des.getName()), "") + " "
                                + System.currentTimeMillis() / 1000 + "."
                                + FilenameUtils.getExtension(file.getName()));
                    }

                    Files.copy(file, des);

                    sender.sendMessage(backup.processMessage("Info.BackupFinished").replaceAll("%file%", file.getName()));
                } catch (IOException e) {
                    sender.sendMessage(backup.processMessage("Error.BackupFailed").replaceAll("%file%", file.getName()));
                    e.printStackTrace();
                }
            });
        } else {
            BackupManager bm = new BackupManager(fileName.toString(), sender, fullBackup);
            bm.createBackup();
        }
        return false;
    }
}
