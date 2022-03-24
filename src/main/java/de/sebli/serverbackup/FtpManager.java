package de.sebli.serverbackup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FtpManager {

    final String server = ServerBackup.getInstance().getConfig().getString("Ftp.Server.IP");
    final int port = ServerBackup.getInstance().getConfig().getInt("Ftp.Server.Port");
    final String user = ServerBackup.getInstance().getConfig().getString("Ftp.Server.User");
    final String pass = ServerBackup.getInstance().getConfig().getString("Ftp.Server.Password");
    private final CommandSender sender;

    public FtpManager(CommandSender sender) {
        this.sender = sender;
    }

    public void uploadFileToFtp(String filePath) {
        File file = new File(filePath);

        if (!file.getPath().contains(ServerBackup.getInstance().backupDestination.replaceAll("/", ""))) {
            file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
        }

        if (!file.exists()) {
            sender.sendMessage("Ftp: Backup '" + file.getName() + "' not found.");

            return;
        }

        FTPSClient ftpClient = new FTPSClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            sender.sendMessage("Ftp: Uploading backup [" + file.getName() + "] ...");

            InputStream inputStream = new FileInputStream(file);

            boolean done = ftpClient.storeFile(file.getName(), inputStream);
            inputStream.close();
            if (done) {
                sender.sendMessage("Ftp: Upload successful. Backup stored on ftp server.");

                if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
                    boolean exists = false;
                    for (FTPFile backup : ftpClient.listFiles()) {
                        if (backup.getName().equalsIgnoreCase(file.getName()))
                            exists = true;
                    }

                    if (exists) {
                        file.delete();
                    } else {
                        sender.sendMessage(
                                "Ftp: Local backup deletion failed because the uploaded file was not found on the ftp server. Try again.");
                    }
                }
            } else {
                sender.sendMessage(
                        "Ftp: Error while uploading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
            }
        } catch (IOException e) {
            sender.sendMessage("Ftp: Error while uploading backup to ftp server.");
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void downloadFileFromFtp(String filePath) {
        File file = new File(filePath);

        FTPSClient ftpClient = new FTPSClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean exists = false;
            for (FTPFile backup : ftpClient.listFiles()) {
                if (backup.getName().equalsIgnoreCase(file.getName()))
                    exists = true;
            }

            if (!exists) {
                sender.sendMessage("Ftp: ftp-backup '" + file.getName() + "' not found.");

                return;
            }

            sender.sendMessage("Ftp: Downloading backup [" + file.getName() + "] ...");

            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
            boolean success = ftpClient.retrieveFile(file.getName(), outputStream);
            outputStream.close();

            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                File dFile = new File(ServerBackup.getInstance().backupDestination + "//" + file.getPath());

                try {
                    FileUtils.copyFile(file, dFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (dFile.exists()) {
                    file.delete();
                }
            });

            if (success) {
                sender.sendMessage("Ftp: Download successful. Backup downloaded from ftp server.");
            } else {
                sender.sendMessage(
                        "Ftp: Error while downloading backup from ftp server. Check server details in config.yml (ip, port, user, password).");
            }
        } catch (IOException e) {
            sender.sendMessage("Ftp: Error while downloading backup from ftp server.");
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getFtpBackupList() {
        List<String> backups = new ArrayList<>();

        FTPSClient ftpClient = new FTPSClient();

        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");

            FTPFile[] files = ftpClient.listFiles();

            int c = 1;

            for (FTPFile file : files) {
                double fileSize = (double) file.getSize() / 1000 / 1000;
                fileSize = Math.round(fileSize * 100.0) / 100.0;

                backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");

                c++;
            }

        } catch (IOException e) {
            sender.sendMessage("Error while connecting to FTP server.");
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return backups;
    }

}
