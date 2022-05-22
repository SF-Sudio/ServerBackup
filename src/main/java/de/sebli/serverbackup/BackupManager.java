package de.sebli.serverbackup;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

public class BackupManager {

    private final CommandSender sender;
    private String filePath;

    public BackupManager(String filePath, CommandSender sender) {
        this.filePath = filePath;
        this.sender = sender;
    }

    public void createBackup() {
        File worldFolder = new File(filePath);

        if (filePath.equalsIgnoreCase("@server")) {
            worldFolder = new File(Bukkit.getWorldContainer().getPath());
            filePath = worldFolder.getPath();
        }

        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH-mm-ss");
        df.setTimeZone(TimeZone.getDefault());

        File backupFolder = new File(ServerBackup.getInstance().backupDestination + "//backup-" + df.format(date) + "-" + filePath + "//" + filePath);

        if (worldFolder.exists()) {
            for (Player all : Bukkit.getOnlinePlayers()) {
                if (all.hasPermission("backup.notification")) {
                    all.sendMessage("Backup [" + worldFolder + "] started.");
                }
            }

            if (!backupFolder.exists()) {
                ZipManager zm = new ZipManager(worldFolder.getName(),
                        ServerBackup.getInstance().backupDestination + "//backup-" + df.format(date) + "-" + filePath + ".zip", Bukkit.getConsoleSender(),
                        true, true);

                zm.zip();
            } else {
                ServerBackup.getInstance().getLogger().log(Level.WARNING, "Backup already exists.");
            }

        } else {
            ServerBackup.getInstance().getLogger().log(Level.WARNING, "Couldn't find '" + filePath + "' folder.");
        }
    }

    public void removeBackup() {
        Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
            File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);

            if (file.exists()) {
                if (file.isDirectory()) {
                    try {
                        Files.delete(file.toPath());

                        sender.sendMessage("Backup [" + filePath + "] removed.");
                    } catch (IOException e) {
                        e.printStackTrace();

                        sender.sendMessage("Error while deleting '" + filePath + "'.");
                    }
                } else {
                    file.delete();

                    sender.sendMessage("Backup [" + filePath + "] removed.");
                }
            } else {
                sender.sendMessage("No Backup named '" + filePath + "' found.");
            }
        });
    }

}
