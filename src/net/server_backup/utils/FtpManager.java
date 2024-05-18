package net.server_backup.utils;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FtpManager {

    private CommandSender sender;

    private static final String server = ServerBackup.getInstance().getConfig().getString("Ftp.Server.IP");
    private static final int port = ServerBackup.getInstance().getConfig().getInt("Ftp.Server.Port");
    private static final String user = ServerBackup.getInstance().getConfig().getString("Ftp.Server.User");
    private static final String pass = ServerBackup.getInstance().getConfig().getString("Ftp.Server.Password");

    public FtpManager(CommandSender sender) {
        this.sender = sender;
    }

    boolean isSSL = true;

    ServerBackup backup = ServerBackup.getInstance();

    public void uploadFileToFtp(String filePath, boolean direct) {
        File file = new File(filePath);

        if (!file.getPath().contains(Configuration.backupDestination.replaceAll("/", ""))) {
            file = new File(Configuration.backupDestination + "//" + filePath);
            filePath = file.getPath();
        }

        if (!file.exists()) {
            sender.sendMessage(OperationHandler.processMessage("Error.NoBackupFound").replaceAll("%file%", file.getName()));

            return;
        }

        FTPSClient ftpsClient = new FTPSClient();
        FTPClient ftpClient = new FTPClient();

        try {
            if (!isSSL) {
                connect(ftpClient);

                sender.sendMessage(OperationHandler.processMessage("Info.FtpUpload").replaceAll("%file%", file.getName()));
                OperationHandler.tasks.add("FTP UPLOAD {" + filePath + "}");

                InputStream inputStream = new FileInputStream(file);

                boolean done = ftpClient.storeFile(file.getName(), inputStream);

                // DEBUG
                Bukkit.getLogger().log(Level.INFO, "(BETA) FTP-DEBUG INFO: " + ftpClient.getReplyString());
                Bukkit.getLogger().log(Level.INFO,
                        "Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

                inputStream.close();

                if (done) {
                    sender.sendMessage(OperationHandler.processMessage("Info.FtpUploadSuccess"));

                    if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
                        boolean exists = false;
                        for (FTPFile backup : ftpClient.listFiles()) {
                            if (backup.getName().equalsIgnoreCase(file.getName()))
                                exists = true;
                        }

                        if (exists) {
                            file.delete();
                        } else {
                            sender.sendMessage(OperationHandler.processMessage("Error.FtpLocalDeletionFailed"));
                        }
                    }
                } else {
                    sender.sendMessage(OperationHandler.processMessage("Error.FtpUploadFailed"));
                }

                OperationHandler.tasks.remove("FTP UPLOAD {" + filePath + "}");
            } else {
                try {
                    connect(ftpsClient);

                    InputStream inputStream = new FileInputStream(file);

                    boolean done = ftpsClient.storeFile(file.getName(), inputStream);

                    // DEBUG
                    Bukkit.getLogger().log(Level.INFO, "(BETA) FTPS-DEBUG INFO: " + ftpsClient.getReplyString());
                    Bukkit.getLogger().log(Level.INFO,
                            "Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

                    inputStream.close();

                    if (done) {
                        sender.sendMessage(OperationHandler.processMessage("Info.FtpUploadSuccess"));

                        if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
                            boolean exists = false;
                            for (FTPFile backup : ftpsClient.listFiles()) {
                                if (backup.getName().equalsIgnoreCase(file.getName()))
                                    exists = true;
                            }

                            if (exists) {
                                file.delete();
                            } else {
                                sender.sendMessage(OperationHandler.processMessage("Error.FtpLocalDeletionFailed"));
                            }
                        }
                    } else {
                        sender.sendMessage(OperationHandler.processMessage("Error.FtpUploadFailed"));
                    }

                    if (OperationHandler.tasks.contains("FTP UPLOAD {" + filePath + "}")) {
                        OperationHandler.tasks.remove("FTP UPLOAD {" + filePath + "}");
                    }
                } catch (Exception e) {
                    isSSL = false;
                    uploadFileToFtp(filePath, direct);
                }
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.FtpUploadFailed"));
            e.printStackTrace();
        } finally {
            disconnect(ftpsClient);
            disconnect(ftpClient);
        }
    }

    public void downloadFileFromFtp(String filePath) {
        File file = new File(filePath);

        FTPSClient ftpsClient = new FTPSClient();
        FTPClient ftpClient = new FTPClient();

        try {
            if (!isSSL) {
                connect(ftpClient);

                boolean exists = false;
                for (FTPFile backup : ftpClient.listFiles()) {
                    if (backup.getName().equalsIgnoreCase(file.getName()))
                        exists = true;
                }

                if (!exists) {
                    sender.sendMessage(OperationHandler.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

                    return;
                }

                sender.sendMessage(OperationHandler.processMessage("Info.FtpDownload").replaceAll("%file%", file.getName()));

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                boolean success = ftpClient.retrieveFile(file.getName(), outputStream);
                outputStream.close();

                Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                    File dFile = new File(Configuration.backupDestination + "//" + file.getPath());

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
                    sender.sendMessage(OperationHandler.processMessage("Info.FtpDownloadSuccess"));
                } else {
                    sender.sendMessage(OperationHandler.processMessage("Error.FtpDownloadFailed"));
                }
            } else {
                try {
                    connect(ftpsClient);

                    boolean exists = false;
                    for (FTPFile backup : ftpsClient.listFiles()) {
                        if (backup.getName().equalsIgnoreCase(file.getName()))
                            exists = true;
                    }

                    if (!exists) {
                        sender.sendMessage(OperationHandler.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

                        return;
                    }

                    sender.sendMessage(OperationHandler.processMessage("Info.FtpDownload").replaceAll("%file%", file.getName()));

                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                    boolean success = ftpsClient.retrieveFile(file.getName(), outputStream);
                    outputStream.close();

                    Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                        File dFile = new File(Configuration.backupDestination + "//" + file.getPath());

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
                        sender.sendMessage(OperationHandler.processMessage("Info.FtpDownloadSuccess"));
                    } else {
                        sender.sendMessage(OperationHandler.processMessage("Error.FtpDownloadFailed"));
                    }
                } catch (Exception e) {
                    isSSL = false;
                    downloadFileFromFtp(filePath);
                }
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.FtpDownloadFailed"));
            e.printStackTrace();
        } finally {
            disconnect(ftpsClient);
            disconnect(ftpClient);
        }
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);

        FTPSClient ftpsClient = new FTPSClient();
        FTPClient ftpClient = new FTPClient();

        try {
            if (!isSSL) {
                connect(ftpClient);

                boolean exists = false;
                for (FTPFile backup : ftpClient.listFiles()) {
                    if (backup.getName().equalsIgnoreCase(file.getName()))
                        exists = true;
                }

                if (!exists) {
                    sender.sendMessage(OperationHandler.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

                    return;
                }

                sender.sendMessage(OperationHandler.processMessage("Info.FtpDeletion").replaceAll("%file%", file.getName()));

                boolean success = ftpClient.deleteFile(file.getPath());

                if (success) {
                    sender.sendMessage(OperationHandler.processMessage("Info.FtpDeletionSuccess"));
                } else {
                    sender.sendMessage(OperationHandler.processMessage("Error.FtpDeletionFailed"));
                }
            } else {
                try {
                    connect(ftpsClient);

                    boolean exists = false;
                    for (FTPFile backup : ftpsClient.listFiles()) {
                        if (backup.getName().equalsIgnoreCase(file.getName()))
                            exists = true;
                    }

                    if (!exists) {
                        sender.sendMessage(OperationHandler.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

                        return;
                    }

                    sender.sendMessage(OperationHandler.processMessage("Info.FtpDeletion").replaceAll("%file%", file.getName()));

                    boolean success = ftpsClient.deleteFile(file.getPath());

                    if (success) {
                        sender.sendMessage(OperationHandler.processMessage("Info.FtpDeletionSuccess"));
                    } else {
                        sender.sendMessage(OperationHandler.processMessage("Error.FtpDeletionFailed"));
                    }
                } catch (Exception e) {
                    isSSL = false;
                    deleteFile(filePath);
                }
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.FtpDeletionFailed"));
            e.printStackTrace();
        } finally {
            disconnect(ftpsClient);
            disconnect(ftpClient);
        }
    }

    public List<String> getFtpBackupList(boolean rawList) {
        List<String> backups = new ArrayList<>();

        FTPSClient ftpsClient = new FTPSClient();
        FTPClient ftpClient = new FTPClient();

        try {
            connect(ftpClient);

            FTPFile[] files = ftpClient.listFiles();

            int c = 1;

            for (FTPFile file : files) {
                double fileSize = (double) file.getSize() / 1000 / 1000;
                fileSize = Math.round(fileSize * 100.0) / 100.0;

                if (rawList) {
                    backups.add(file.getName() + ":" + file.getSize());
                } else {
                    backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");
                }

                c++;
            }
        } catch (Exception e) {
            isSSL = true;
            getFtpBackupList(rawList);

            try {
                connect(ftpsClient);

                FTPFile[] files = ftpsClient.listFiles();

                int c = 1;

                for (FTPFile file : files) {
                    double fileSize = (double) file.getSize() / 1000 / 1000;
                    fileSize = Math.round(fileSize * 100.0) / 100.0;

                    if (rawList) {
                        backups.add(file.getName() + ":" + fileSize);
                    } else {
                        backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");
                    }

                    c++;
                }
            } catch (Exception ex) {
                isSSL = false;
                getFtpBackupList(rawList);
            }
        } finally {
            disconnect(ftpsClient);
            disconnect(ftpClient);

            return backups;
        }
    }

    private void connect(FTPClient client) throws IOException {
        client.connect(server, port);
        client.login(user, pass);
        client.enterLocalPassiveMode();

        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.setFileTransferMode(FTP.BINARY_FILE_TYPE);

        client.changeWorkingDirectory(
                ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));
    }

    private void connect(FTPSClient client) throws IOException {
        client.connect(server, port);
        client.login(user, pass);
        client.enterLocalPassiveMode();

        client.execPBSZ(0);
        client.execPROT("P");

        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.setFileTransferMode(FTP.BINARY_FILE_TYPE);

        client.changeWorkingDirectory(
                ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));
    }

    private void disconnect(FTPClient client) {
        try {
            if (client.isConnected()) {
                client.logout();
                client.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect(FTPSClient client) {
        try {
            if (client.isConnected()) {
                client.logout();
                client.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
