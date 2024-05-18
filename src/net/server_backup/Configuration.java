package net.server_backup;

import net.server_backup.core.OperationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Configuration {

    public static String prefix;
    public static String backupDestination = "Backups//";

    private static File bpInf = new File("plugins//ServerBackup//backupInfo.yml");
    public static YamlConfiguration backupInfo = YamlConfiguration.loadConfiguration(bpInf);

    private static File cloudInf = new File("plugins//ServerBackup//cloudAccess.yml");
    public static YamlConfiguration cloudInfo = YamlConfiguration.loadConfiguration(cloudInf);

    private static File messagesFile = new File("plugins//ServerBackup//messages.yml");
    public static YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);

    public static void loadUp() {
        loadConfig();
        loadCloud();
        loadBackupInfo();
        loadMessages();
    }

    public static void loadConfig() {
        if (ServerBackup.getInstance().getConfig().contains("BackupDestination")) {
            backupDestination = ServerBackup.getInstance().getConfig().getString("BackupDestination");
        }

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

        ServerBackup.getInstance().getConfig().options()
                .header("BackupTimer = At what time should a Backup be created? The format is: 'hh-mm' e.g. '12-30'."
                        + "\nDeleteOldBackups = Deletes old backups automatically after a specific time (in days, standard = 7 days)"
                        + "\nDeleteOldBackups - Type '0' at DeleteOldBackups to disable the deletion of old backups."
                        + "\nBackupLimiter = Deletes old backups automatically if number of total backups is greater than this number (e.g. if you enter '5' - the oldest backup will be deleted if there are more than 5 backups, so you will always keep the latest 5 backups)"
                        + "\nBackupLimiter - Type '0' to disable this feature. If you don't type '0' the feature 'DeleteOldBackups' will be disabled and this feature ('BackupLimiter') will be enabled."
                        + "\nKeepUniqueBackups - Type 'true' to disable the deletion of unique backups. The plugin will keep the newest backup of all backed up worlds or folders, no matter how old it is."
                        + "\nBlacklist - A list of files/directories that will not be backed up."
                        + "\nIMPORTANT FTP information: Set 'UploadBackup' to 'true' if you want to store your backups on a ftp server (sftp does not work at the moment - if you host your own server (e.g. vps/root server) you need to set up a ftp server on it)."
                        + "\nIf you use ftp backups, you can set 'DeleteLocalBackup' to 'true' if you want the plugin to remove the created backup from your server once it has been uploaded to your ftp server."
                        + "\nCompressBeforeUpload compresses the backup to a zip file before uploading it. Set it to 'false' if you want the files to be uploaded directly to your ftp server."
                        + "\nJoin the discord server if you need help or have a question: https://discord.gg/rNzngsCWFC");
        ServerBackup.getInstance().getConfig().options().copyDefaults(true);

        ServerBackup.getInstance().getConfig().addDefault("AutomaticBackups", true);
        ServerBackup.getInstance().getConfig().addDefault("CommandAfterAutomaticBackup", "/");

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

        ServerBackup.getInstance().getConfig().addDefault("BackupTimer.Days", days);
        ServerBackup.getInstance().getConfig().addDefault("BackupTimer.Times", times);

        List<String> worlds = new ArrayList<>();
        worlds.add("world");
        worlds.add("world_nether");
        worlds.add("world_the_end");

        ServerBackup.getInstance().getConfig().addDefault("BackupWorlds", worlds);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("libraries");
        blacklist.add("plugins/ServerBackup/config.yml");

        ServerBackup.getInstance().getConfig().addDefault("Blacklist", blacklist);

        ServerBackup.getInstance().getConfig().addDefault("DeleteOldBackups", 14);
        ServerBackup.getInstance().getConfig().addDefault("BackupLimiter", 0);

        ServerBackup.getInstance().getConfig().addDefault("KeepUniqueBackups", false);

        ServerBackup.getInstance().getConfig().addDefault("UpdateAvailableMessage", true);
        ServerBackup.getInstance().getConfig().addDefault("AutomaticUpdates", true);

        ServerBackup.getInstance().getConfig().addDefault("BackupDestination", "Backups//");

        ServerBackup.getInstance().getConfig().addDefault("CloudBackup.Dropbox", false);
        ServerBackup.getInstance().getConfig().addDefault("CloudBackup.Options.Destination", "/");
        ServerBackup.getInstance().getConfig().addDefault("CloudBackup.Options.DeleteLocalBackup", false);

        ServerBackup.getInstance().getConfig().addDefault("Ftp.UploadBackup", false);
        ServerBackup.getInstance().getConfig().addDefault("Ftp.DeleteLocalBackup", false);
        ServerBackup.getInstance().getConfig().addDefault("Ftp.Server.IP", "127.0.0.1");
        ServerBackup.getInstance().getConfig().addDefault("Ftp.Server.Port", 21);
        ServerBackup.getInstance().getConfig().addDefault("Ftp.Server.User", "username");
        ServerBackup.getInstance().getConfig().addDefault("Ftp.Server.Password", "password");
        ServerBackup.getInstance().getConfig().addDefault("Ftp.Server.BackupDirectory", "Backups/");

        ServerBackup.getInstance().getConfig().addDefault("DynamicBackup", false);
        ServerBackup.getInstance().getConfig().addDefault("SendLogMessages", false);

        ServerBackup.getInstance().saveConfig();

        backupDestination = ServerBackup.getInstance().getConfig().getString("BackupDestination");
    }

    public static void loadCloud() {
        if (!cloudInf.exists()) {
            try {
                cloudInf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        cloudInfo.options().header("Dropbox - Watch this video for explanation: https://youtu.be/k-0aIohxRUA");

        if (!cloudInfo.contains("Cloud.Dropbox")) {
            cloudInfo.set("Cloud.Dropbox.AppKey", "appKey");
            cloudInfo.set("Cloud.Dropbox.AppSecret", "appSecret");
        } else {
            if (cloudInfo.getString("Cloud.Dropbox.AppKey") != "appKey" && !cloudInfo.contains("Cloud.Dropbox.ActivationLink")) {
                cloudInfo.set("Cloud.Dropbox.ActivationLink", "https://www.dropbox.com/oauth2/authorize?client_id=" + cloudInfo.getString("Cloud.Dropbox.AppKey") + "&response_type=code&token_access_type=offline");
                if (!cloudInfo.contains("Cloud.Dropbox.AccessToken")) {
                    cloudInfo.set("Cloud.Dropbox.AccessToken", "accessToken");
                }
            }
        }

        saveCloud();
    }

    public static void saveCloud() {
        try {
            cloudInfo.save(cloudInf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadBackupInfo() {
        if (!bpInf.exists()) {
            try {
                bpInf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveBackupInfo();
    }

    public static void loadMessages() {
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Messages.loadMessages();
    }

    public static void saveBackupInfo() {
        try {
            backupInfo.save(bpInf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reloadConfig(CommandSender sender) {
        ServerBackup.getInstance().reloadConfig();

        OperationHandler.stopTimer();
        OperationHandler.startTimer();

        String oldDes = backupDestination;

        if (!oldDes
                .equalsIgnoreCase(ServerBackup.getInstance().getConfig().getString("BackupDestination"))) {
            backupDestination = ServerBackup.getInstance().getConfig()
                    .getString("BackupDestination");

            ServerBackup.getInstance().getLogger().log(Level.INFO,
                    "ServerBackup: Backup destination [" + oldDes + " >> "
                            + backupDestination + "] updated successfully.");
        }

        if (cloudInf.exists()) {
            saveCloud();
        }

        loadUp();

        sender.sendMessage(OperationHandler.processMessage("Command.Reload"));
    }

}
