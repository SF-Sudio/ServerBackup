package de.seblii.serverbackup.utils;

import de.seblii.serverbackup.ServerBackup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FtpManager {

	private final CommandSender sender;

	final String server = ServerBackup.getInstance().getConfig().getString("Ftp.Server.IP");
	final int port = ServerBackup.getInstance().getConfig().getInt("Ftp.Server.Port");
	final String user = ServerBackup.getInstance().getConfig().getString("Ftp.Server.User");
	final String pass = ServerBackup.getInstance().getConfig().getString("Ftp.Server.Password");

	public FtpManager(CommandSender sender) {
		this.sender = sender;
	}

	boolean isSSL = true;

	final ServerBackup backup = ServerBackup.getInstance();

	public void uploadFileToFtp(String filePath, boolean direct) {
		File file = new File(filePath);

		if (!file.getPath().contains(ServerBackup.getInstance().backupDestination.replaceAll("/", ""))) {
			file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
			filePath = file.getPath();
		}

		if (!file.exists()) {
			sender.sendMessage(backup.processMessage("Error.NoBackupFound").replaceAll("%file%", file.getName()));

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

				sender.sendMessage(backup.processMessage("Info.FtpUpload").replaceAll("%file%", file.getName()));

				ftpC.setControlKeepAliveTimeout(100);

				InputStream inputStream = Files.newInputStream(file.toPath());

				boolean done = ftpC.storeFile(file.getName(), inputStream);

				// DEBUG
				Bukkit.getLogger().log(Level.INFO, "(BETA) FTP-DEBUG INFO: " + ftpC.getReplyString());
				Bukkit.getLogger().log(Level.INFO,
						"Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

				inputStream.close();

				if (done) {
					sender.sendMessage(backup.processMessage("Info.FtpUploadSuccess"));

					if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
						boolean exists = false;
						for (FTPFile backup : ftpC.listFiles()) {
							if (backup.getName().equalsIgnoreCase(file.getName()))
								exists = true;
						}

						if (exists) {
							try {
								Files.delete(file.toPath());
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						} else {
							sender.sendMessage(backup.processMessage("Error.FtpLocalDeletionFailed"));
						}
					}
				} else {
					sender.sendMessage(backup.processMessage("Error.FtpUploadFailed"));
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

					ftpClient.setControlKeepAliveTimeout(100);

					InputStream inputStream = Files.newInputStream(file.toPath());

					boolean done = ftpClient.storeFile(file.getName(), inputStream);

					// DEBUG
					Bukkit.getLogger().log(Level.INFO, "(BETA) FTPS-DEBUG INFO: " + ftpClient.getReplyString());
					Bukkit.getLogger().log(Level.INFO,
							"Use this info for reporting ftp related bugs. Ignore it if everything is fine.");

					inputStream.close();

					if (done) {
						sender.sendMessage(backup.processMessage("Info.FtpUploadSuccess"));

						if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
							boolean exists = false;
							for (FTPFile backup : ftpClient.listFiles()) {
								if (backup.getName().equalsIgnoreCase(file.getName()))
									exists = true;
							}

							if (exists) {
								try {
									Files.delete(file.toPath());
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							} else {
								sender.sendMessage(backup.processMessage("Error.FtpLocalDeletionFailed"));
							}
						}
					} else {
						sender.sendMessage(backup.processMessage("Error.FtpUploadFailed"));
					}
				} catch (Exception e) {
					isSSL = false;
					uploadFileToFtp(filePath, direct);
				}
			}
		} catch (IOException e) {
			sender.sendMessage(backup.processMessage("Error.FtpUploadFailed"));
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
					sender.sendMessage(backup.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

					return;
				}

				sender.sendMessage(backup.processMessage("Info.FtpDownload").replaceAll("%file%", file.getName()));

				OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
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
						try {
							Files.delete(file.toPath());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				});

				if (success) {
					sender.sendMessage(backup.processMessage("Info.FtpDownloadSuccess"));
				} else {
					sender.sendMessage(backup.processMessage("Error.FtpDownloadFailed"));
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
						sender.sendMessage(backup.processMessage("Error.FtpNotFound").replaceAll("%file%", file.getName()));

						return;
					}

					sender.sendMessage(backup.processMessage("Info.FtpDownload").replaceAll("%file%", file.getName()));

					OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
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
							try {
								Files.delete(file.toPath());
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					});

					if (success) {
						sender.sendMessage(backup.processMessage("Info.FtpDownloadSuccess"));
					} else {
						sender.sendMessage(backup.processMessage("Error.FtpDownloadFailed"));
					}
				} catch (Exception e) {
					isSSL = false;
					downloadFileFromFtp(filePath);
				}
			}
		} catch (IOException e) {
			sender.sendMessage(backup.processMessage("Error.FtpDownloadFailed"));
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
			sender.sendMessage(backup.processMessage("Error.FtpConnectionFailed"));
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
