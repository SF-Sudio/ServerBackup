package net.server_backup.core;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

public class Backup {

    private final String backupFilePath;
    private CommandSender sender;
    private final boolean isFullBackup;

    public Backup(String backupFilePath, CommandSender sender, boolean isFullBackup) {
        this.backupFilePath = backupFilePath;
        this.sender = sender;
        this.isFullBackup = isFullBackup;
    }

    public String getBackupFilePath() {
        return backupFilePath;
    }

    public void create() {
        String filePath = backupFilePath;
        File worldFolder = new File(filePath);

        if (filePath.equalsIgnoreCase("@server")) {
            filePath = new File(".").getPath();
            worldFolder = new File(filePath);
        }

        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH-mm-ss");
        df.setTimeZone(TimeZone.getDefault());

        File backupFolder = new File(Configuration.backupDestination + "//backup-" + df.format(date) + "-"
                + filePath + "//" + filePath);

        if(worldFolder.exists()) {
            try {
                if (!backupFolder.exists()) {
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("backup.notification")) {
                            all.sendMessage(OperationHandler.processMessage("Info.BackupStarted").replaceAll("%file%", worldFolder.getName()));
                        }
                    }

                    ZipManager zm = new ZipManager(
                            worldFolder.getPath(), Configuration.backupDestination + "//backup-"
                            + df.format(date) + "-" + filePath.replaceAll("/", "-") + ".zip",
                            Bukkit.getConsoleSender(), true, true, isFullBackup);

                    zm.zip();

                    OperationHandler.tasks.add("CREATE {" + filePath.replace("\\", "/") + "}");
                } else {
                    Bukkit.getLogger().log(Level.WARNING, "Backup already exists.");
                }
            } catch (IOException e) {
                e.printStackTrace();

                Bukkit.getLogger().log(Level.WARNING, "Backup failed.");
            }
        }
    }

    public void remove() {
        Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
            File file = new File(Configuration.backupDestination + "//" + backupFilePath);

            if (file.exists()) {
                if (file.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(file);

                        sender.sendMessage(OperationHandler.processMessage("Info.BackupRemoved").replaceAll("%file%", backupFilePath));
                    } catch (IOException e) {
                        e.printStackTrace();

                        sender.sendMessage(OperationHandler.processMessage("Error.DeletionFailed").replaceAll("%file%", backupFilePath));
                    }
                } else {
                    file.delete();

                    sender.sendMessage(OperationHandler.processMessage("Info.BackupRemoved").replaceAll("%file%", backupFilePath));
                }
            } else {
                sender.sendMessage(OperationHandler.processMessage("Error.NoBackupFound").replaceAll("%file%", backupFilePath));
            }
        });
    }

}
