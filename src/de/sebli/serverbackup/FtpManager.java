package de.sebli.serverbackup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class FtpManager {

	private CommandSender sender;

	String server = ServerBackup.getInstance().getConfig().getString("Ftp.Server.IP");
	int port = ServerBackup.getInstance().getConfig().getInt("Ftp.Server.Port");
	String user = ServerBackup.getInstance().getConfig().getString("Ftp.Server.User");
	String pass = ServerBackup.getInstance().getConfig().getString("Ftp.Server.Password");

	public FtpManager(CommandSender sender) {
		this.sender = sender;
	}

	boolean isSSL = true;

	public void uploadFileToFtp(String filePath) {
		File file = new File(filePath);

		if (!file.getPath().contains(ServerBackup.getInstance().backupDestination.replaceAll("/", ""))) {
			file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
			filePath = file.getPath();
		}

		if (!file.exists()) {
			sender.sendMessage("Ftp: Backup '" + file.getName() + "' not found.");

			return;
		}

		FTPSClient ftpClient = new FTPSClient();
		FTPClient ftpC = new FTPClient();

		try {
			if (!isSSL) {
				try {
					if (ftpClient.isConnected()) {
						ftpClient.logout();
						ftpClient.disconnect();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				ftpC.connect(server, port);
				ftpC.login(user, pass);
				ftpC.enterLocalPassiveMode();

				ftpC.setFileType(FTP.BINARY_FILE_TYPE);
				ftpC.setFileTransferMode(FTP.BINARY_FILE_TYPE);

				ftpC.changeWorkingDirectory(
						ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

				sender.sendMessage("Ftp: Uploading backup [" + file.getName() + "] ...");

				InputStream inputStream = new FileInputStream(file);

				ftpC.setControlKeepAliveTimeout(100);

				boolean done = ftpC.storeFile(file.getName(), inputStream);

				// DEBUG
				Bukkit.getLogger().log(Level.INFO, "(BETA) FTP-DEBUG INFO: " + ftpC.getReplyString());
				Bukkit.getLogger().log(Level.INFO,
						"Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

				inputStream.close();

				if (done) {
					sender.sendMessage("Ftp: Upload successfull. Backup stored on ftp server.");

					if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
						boolean exists = false;
						for (FTPFile backup : ftpC.listFiles()) {
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
			} else {
				try {
					try {
						if (ftpC.isConnected()) {
							ftpC.logout();
							ftpC.disconnect();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					ftpClient.connect(server, port);
					ftpClient.login(user, pass);
					ftpClient.enterLocalPassiveMode();

					ftpClient.execPBSZ(0);
					ftpClient.execPROT("P");

					ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
					ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

					ftpClient.changeWorkingDirectory(
							ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

					InputStream inputStream = new FileInputStream(file);

					ftpClient.setControlKeepAliveTimeout(100);

					boolean done = ftpClient.storeFile(file.getName(), inputStream);

					// DEBUG
					Bukkit.getLogger().log(Level.INFO, "(BETA) FTPS-DEBUG INFO: " + ftpClient.getReplyString());
					Bukkit.getLogger().log(Level.INFO,
							"Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

					inputStream.close();

					if (done) {
						sender.sendMessage("Ftp: Upload successfull. Backup stored on ftp server.");

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
				} catch (Exception e) {
					isSSL = false;
					uploadFileToFtp(filePath);
				}
			}
		} catch (IOException e) {
			sender.sendMessage("Ftp: Error while uploading backup to ftp server.");
			e.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
				}

				if (ftpC.isConnected()) {
					ftpC.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void downloadFileFromFtp(String filePath) {
		File file = new File(filePath);

		FTPSClient ftpClient = new FTPSClient();
		FTPClient ftpC = new FTPClient();

		try {
			if (!isSSL) {
				ftpC.connect(server, port);
				ftpC.login(user, pass);
				ftpC.enterLocalPassiveMode();

				ftpC.setFileType(FTP.BINARY_FILE_TYPE);

				ftpC.changeWorkingDirectory(
						ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

				boolean exists = false;
				for (FTPFile backup : ftpC.listFiles()) {
					if (backup.getName().equalsIgnoreCase(file.getName()))
						exists = true;
				}

				if (!exists) {
					sender.sendMessage("Ftp: ftp-backup '" + file.getName() + "' not found.");

					return;
				}

				sender.sendMessage("Ftp: Downloading backup [" + file.getName() + "] ...");

				OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
				boolean success = ftpC.retrieveFile(file.getName(), outputStream);
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
					sender.sendMessage("Ftp: Download successfull. Backup downloaded from ftp server.");
				} else {
					sender.sendMessage(
							"Ftp: Error while downloading backup from ftp server. Check server details in config.yml (ip, port, user, password).");
				}
			} else {
				try {
					ftpClient.connect(server, port);
					ftpClient.login(user, pass);
					ftpClient.enterLocalPassiveMode();

					ftpClient.execPBSZ(0);
					ftpClient.execPROT("P");

					ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

					ftpClient.changeWorkingDirectory(
							ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

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
						sender.sendMessage("Ftp: Download successfull. Backup downloaded from ftp server.");
					} else {
						sender.sendMessage(
								"Ftp: Error while downloading backup from ftp server. Check server details in config.yml (ip, port, user, password).");
					}
				} catch (Exception e) {
					isSSL = false;
					downloadFileFromFtp(filePath);
				}
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
		FTPClient ftpC = new FTPClient();

		try {
			if (!isSSL) {
				ftpC.connect(server, port);
				ftpC.login(user, pass);
				ftpC.enterLocalPassiveMode();

				ftpC.changeWorkingDirectory(
						ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

				FTPFile[] files = ftpC.listFiles();

				int c = 1;

				for (FTPFile file : files) {
					double fileSize = (double) file.getSize() / 1000 / 1000;
					fileSize = Math.round(fileSize * 100.0) / 100.0;

					backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");

					c++;
				}
			} else {
				try {
					ftpClient.connect(server, port);
					ftpClient.login(user, pass);
					ftpClient.enterLocalPassiveMode();

					ftpClient.execPBSZ(0);
					ftpClient.execPROT("P");

					ftpClient.changeWorkingDirectory(
							ServerBackup.getInstance().getConfig().getString("Ftp.Server.BackupDirectory"));

					FTPFile[] files = ftpClient.listFiles();

					int c = 1;

					for (FTPFile file : files) {
						double fileSize = (double) file.getSize() / 1000 / 1000;
						fileSize = Math.round(fileSize * 100.0) / 100.0;

						backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");

						c++;
					}
				} catch (Exception e) {
					isSSL = false;
					getFtpBackupList();
				}
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
