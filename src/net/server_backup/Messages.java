package net.server_backup;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;

public class Messages {

    private static YamlConfiguration messages = Configuration.messages;

    public static void loadMessages() {
        messages.options().header("Use '&nl' to add a new line. Use '&' for color codes (e.g. '&4' for color red). For some messages you can use a placeholder (e.g. '%file%' for file name)." +
                "\nMinecraft color codes: https://htmlcolorcodes.com/minecraft-color-codes/");
        messages.options().copyDefaults(true);

        messages.addDefault("Prefix", "");

        messages.addDefault("Command.Zip.Header", "Zipping Backup..."
                + "&nl");
        messages.addDefault("Command.Zip.Footer", "&nl"
                + "&nlBackup [%file%] zipped."
                + "&nlBackup [%file%] saved.");
        messages.addDefault("Command.Unzip.Header", "Unzipping Backup..."
                + "&nl");
        messages.addDefault("Command.Unzip.Footer", "&nl"
                + "&nlBackup [%file%] unzipped.");
        messages.addDefault("Command.Reload", "Config reloaded.");
        messages.addDefault("Command.Tasks.Header", "----- Backup tasks -----"
                + "&nl");
        messages.addDefault("Command.Tasks.Footer", "&nl"
                + "----- Backup tasks -----");
        messages.addDefault("Command.Shutdown.Start", "The server will shut down after backup tasks (check with: '/backup tasks') are finished."
                + "&nlYou can cancel the shutdown by running this command again.");
        messages.addDefault("Command.Shutdown.Cancel", "Shutdown canceled.");

        messages.addDefault("Info.BackupFinished", "Backup [%file%] saved.");
        messages.addDefault("Info.BackupStarted", "Backup [%file%] started.");
        messages.addDefault("Info.BackupRemoved", "Backup [%file%] removed.");
        messages.addDefault("Info.FtpUpload", "Ftp: Uploading backup [%file%] ...");
        messages.addDefault("Info.FtpUploadSuccess", "Ftp: Upload successfully. Backup stored on ftp server.");
        messages.addDefault("Info.FtpDownload", "Ftp: Downloading backup [%file%] ...");
        messages.addDefault("Info.FtpDownloadSuccess", "Ftp: Download successful. Backup downloaded from ftp server.");

        messages.addDefault("Error.NoPermission", "&cI'm sorry but you do not have permission to perform this command.");
        messages.addDefault("Error.NoBackups", "No backups found.");
        messages.addDefault("Error.NoBackupFound", "No Backup named '%file%' found.");
        messages.addDefault("Error.NoBackupSearch", "No backups for search argument '%input%' found.");
        messages.addDefault("Error.DeletionFailed", "Error while deleting '%file%'.");
        messages.addDefault("Error.FolderExists", "There is already a folder named '%file%'.");
        messages.addDefault("Error.ZipExists", "There is already a ZIP file named '%file%.zip'.");
        messages.addDefault("Error.NoFtpBackups", "No ftp backups found.");
        messages.addDefault("Error.NoTasks", "No backup tasks are running.");
        messages.addDefault("Error.AlreadyZip", "%file% is already a ZIP file.");
        messages.addDefault("Error.NotAZip", "%file% is not a ZIP file.");
        messages.addDefault("Error.NotANumber", "%input% is not a valid number.");
        messages.addDefault("Error.BackupFailed", "An error occurred while saving Backup [%file%]. See console for more information.");
        messages.addDefault("Error.FtpUploadFailed", "Ftp: Error while uploading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
        messages.addDefault("Error.FtpDownloadFailed", "Ftp: Error while downloading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
        messages.addDefault("Error.FtpLocalDeletionFailed", "Ftp: Local backup deletion failed because the uploaded file was not found on the ftp server. Try again.");
        messages.addDefault("Error.FtpNotFound", "Ftp: ftp-backup %file% not found.");
        messages.addDefault("Error.FtpConnectionFailed", "Ftp: Error while connecting to FTP server.");

        Configuration.saveMessages();

        Configuration.prefix = ((messages.getString("Prefix").equals("")) ? messages.getString("Prefix") : (messages.getString("Prefix") + " "));
    }
}
