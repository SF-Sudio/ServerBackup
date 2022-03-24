package de.sebli.serverbackup;

import de.sebli.serverbackup.commands.SBCommand;
import de.sebli.serverbackup.utils.Metrics;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

public class ServerBackup extends JavaPlugin implements Listener {

    private static ServerBackup sb;
    public String backupDestination = "Backups//";

    public static ServerBackup getInstance() {
        return sb;
    }

    @Override
    public void onDisable() {
        stopTimer();

        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            task.cancel();

            this.getLogger().log(Level.WARNING, "WARNING - ServerBackup: Task [" + task.getTaskId()
                    + "] cancelled due to server shutdown. There might be some unfinished Backups.");
        }

        this.getLogger().log(Level.INFO, "ServerBackup: Plugin disabled.");
    }

    @Override
    public void onEnable() {
        sb = this;

        loadFiles();

        getCommand("backup").setExecutor(new SBCommand());

        Bukkit.getPluginManager().registerEvents(this, this);

        startTimer();

        this.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");

        if (getConfig().getBoolean("UpdateAvailabeMessage")) {
            checkVersion();
        }

        int mpid = 14673;

        Metrics metrics = new Metrics(this, mpid);

        metrics.addCustomChart(new Metrics.SimplePie("player_per_server", () -> String.valueOf(Bukkit.getOnlinePlayers().size())));

        metrics.addCustomChart(new Metrics.SimplePie("using_ftp_server", () -> {
            if (getConfig().getBoolean("Ftp.UploadBackup")) {
                return "yes";
            } else {
                return "no";
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("total_backup_space", () -> {
            File file = new File(backupDestination);

            double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
            fileSize = Math.round(fileSize * 100.0) / 100.0;

            return (int) fileSize;
        }));
    }

    private void checkVersion() {
        this.getLogger().log(Level.INFO, "ServerBackup: Searching for updates...");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            int resourceID = 79320;
            try (InputStream inputStream = (new URL(
                    "https://api.spigotmc.org/legacy/update.php?resource=" + resourceID)).openStream();
                 Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    String latest = scanner.next();
                    String current = getDescription().getVersion();

                    int late = Integer.parseInt(latest.replaceAll("\\.", ""));
                    int curr = Integer.parseInt(current.replaceAll("\\.", ""));

                    if (curr >= late) {
                        this.getLogger().log(Level.INFO,
                                "ServerBackup: No updates found. The server is running the latest version.");
                    } else {
                        this.getLogger().log(Level.INFO, "\nServerBackup: There is a newer version available - "
                                + latest + ", you are on - " + current);
                        this.getLogger().log(Level.INFO,
                                "ServerBackup: Please download the latest version - https://www.spigotmc.org/resources/"
                                        + resourceID + "\n");
                    }
                }
            } catch (IOException exception) {
                this.getLogger().log(Level.WARNING,
                        "ServerBackup: Cannot search for updates - " + exception.getMessage());
            }
        });

    }

    public void loadFiles() {
        if (getConfig().contains("BackupDestination"))
            backupDestination = getConfig().getString("BackupDestination");

        if (!Files.exists(Paths.get(backupDestination))) {
            try {
                Files.createDirectories(Paths.get(backupDestination));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File files = new File(backupDestination + "//Files");

        if (!files.exists()) {
            files.mkdir();
        }

        getConfig().options()
                .header("BackupTimer = At what time should a Backup be created? The format is: 'hh-mm' e.g. '12-30'."
                        + "\nDeleteOldBackups = Deletes old backups automatically after a specific time (in days, standard = 7 days)"
                        + "\nDeleteOldBackups - Type '0' at DeleteOldBackups to disable the deletion of old backups."
                        + "\nBackupLimiter = Deletes old backups automatically if number of total backups is greater than this number (e.g. if you enter '5' - the oldest backup will be deleted if there are more than 5 backups, so you will always keep the latest 5 backups)"
                        + "\nBackupLimiter - Type '0' to disable this feature. If you don't type '0' the feature 'DeleteOldBackups' will be disabled and this feature ('BackupLimiter') will be enabled."
                        + "\nKeepUniqueBackups - Type 'true' to disable the deletion of unique backups. The plugin will keep the newest backup of all backed up worlds or folders, no matter how old it is."
                        + "\nCollectiveZipFile - Type 'true' if you want to have all backed up worlds in just one zip file.\n"
                        + "\nIMPORTANT FTP information [BETA feature]: Set 'UploadBackup' to 'true' if you want to store your backups on a ftp server (sftp does not work at the moment - if you host your own server (e.g. vps/root server) you need to set up a ftp server on it)."
                        + "\nIf you use ftp backups, you can set 'DeleteLocalBackup' to 'true' if you want the plugin to remove the created backup from your server once it has been uploaded to your ftp server."
                        + "\nContact me if you need help or have a question: https://www.spigotmc.org/conversations/add?to=SebliYT");
        getConfig().options().copyDefaults(true);

        getConfig().addDefault("AutomaticBackups", true);

        List<String> days = new ArrayList<>();
        days.add("MONDAY");
        days.add("TUESDAY");
        days.add("WEDNESDAY");
        days.add("THURSDAY");
        days.add("FRIDAY");
        days.add("SATURDAY");
        days.add("SUNDAY");

        List<String> times = new ArrayList<>();
        times.add("00-00");

        getConfig().addDefault("BackupTimer.Days", days);
        getConfig().addDefault("BackupTimer.Times", times);

        List<String> worlds = new ArrayList<>();
        worlds.add("world");
        worlds.add("world_nether");
        worlds.add("world_the_end");

        getConfig().addDefault("BackupWorlds", worlds);

        getConfig().addDefault("DeleteOldBackups", 14);
        getConfig().addDefault("BackupLimiter", 0);

        getConfig().addDefault("KeepUniqueBackups", false);
        getConfig().addDefault("CollectiveZipFile", false);
        getConfig().addDefault("UpdateAvailabeMessage", true);

        if (getConfig().contains("ZipCompression")) {
            getConfig().set("ZipCompression", null);
        }

        if (getConfig().contains("BackupDestination")) {
            if (getConfig().getString("BackupDestination")
                    .equalsIgnoreCase("- this feature will be available soon -")) {
                getConfig().set("BackupDestination", null);
            }
        }

        getConfig().addDefault("BackupDestination", "Backups//");

        getConfig().addDefault("Ftp.UploadBackup", false);
        getConfig().addDefault("Ftp.DeleteLocalBackup", false);
        getConfig().addDefault("Ftp.Server.IP", "127.0.0.1");
        getConfig().addDefault("Ftp.Server.Port", 21);
        getConfig().addDefault("Ftp.Server.User", "username");
        getConfig().addDefault("Ftp.Server.Password", "password");

        getConfig().addDefault("SendLogMessages", false);

        if (getConfig().contains("FirstStart")) {
            getConfig().set("FirstStart", null);
        }

        saveConfig();

        backupDestination = getConfig().getString("BackupDestination");
    }

    public void startTimer() {
        if (getConfig().getBoolean("AutomaticBackups")) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new BackupTimer(), 20 * 60, 20 * 60);
        }
    }

    public void stopTimer() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    // Events
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (p.hasPermission("backup.admin")) {
            if (getConfig().getBoolean("UpdateAvailabeMessage")) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    int resourceID = 79320;
                    try (InputStream inputStream = (new URL(
                            "https://api.spigotmc.org/legacy/update.php?resource=" + resourceID)).openStream();
                         Scanner scanner = new Scanner(inputStream)) {
                        if (scanner.hasNext()) {
                            String latest = scanner.next();
                            String current = getDescription().getVersion();

                            int late = Integer.parseInt(latest.replaceAll("\\.", ""));
                            int curr = Integer.parseInt(current.replaceAll("\\.", ""));

                            if (curr < late) {
                                p.sendMessage("§8=====§fServerBackup§8=====");
                                p.sendMessage("");
                                p.sendMessage("§7There is a newer version available - §a" + latest
                                        + "§7, you are on - §c" + current);
                                p.sendMessage(
                                        "§7Please download the latest version - §4https://www.spigotmc.org/resources/"
                                                + resourceID);
                                p.sendMessage("");
                                p.sendMessage("§8=====§9Plugin by Seblii§8=====");
                            }
                        }
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

}
