package de.seblii.serverbackup;

import de.seblii.serverbackup.utils.DropboxManager;
import de.seblii.serverbackup.utils.FtpManager;
import de.seblii.serverbackup.utils.WebDavManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipManager {

	private final String sourceFilePath;
	private String targetFilePath;
	private final CommandSender sender;
	private final boolean sendDebugMsg;
	private final boolean isSaving;
	private final boolean fullBackup;

	private final ServerBackup backup = ServerBackup.getInstance();

	public ZipManager(String sourceFilePath, String targetFilePath, CommandSender sender, boolean sendDebugMsg,
			boolean isSaving, boolean fullBackup) {
		this.sourceFilePath = sourceFilePath;
		this.targetFilePath = targetFilePath;
		this.sender = sender;
		this.sendDebugMsg = sendDebugMsg;
		this.isSaving = isSaving;
		this.fullBackup = fullBackup;
	}

	public void zip(){
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
			long sTime = System.nanoTime();

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Started zipping: " + sourceFilePath);
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			Path p;
			try {
				p = Files.createFile(Paths.get(targetFilePath));
			} catch (IOException e) {
				e.printStackTrace();
				ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while zipping files.");
				return;
			}

			try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
				Path pp = Paths.get(sourceFilePath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
					if (!path.toString().contains(Objects.requireNonNull(ServerBackup.getInstance().getConfig().getString("BackupDestination"))
							.replaceAll("/", ""))) {
						ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());

						for (String blacklist : ServerBackup.getInstance().getConfig().getStringList("Blacklist")) {
							File bl = new File(blacklist);

							if (bl.isDirectory()) {
								if (path.toFile().getParent().startsWith(bl.toString())
										|| path.toFile().getParent().startsWith(".\\" + bl)) {
									return;
								}
							} else {
								if (path.equals(new File(blacklist).toPath())
										|| path.equals(new File(".\\" + blacklist).toPath())) {
									sender.sendMessage("Found '" + path + "' in blacklist. Skipping file.");
									return;
								}
							}
						}

						if (!fullBackup) {
							if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
								if (path.getParent().toString().endsWith("region")
										|| path.getParent().toString().endsWith("entities")
										|| path.getParent().toString().endsWith("poi")) {
									boolean found = ServerBackup.getInstance().bpInf
											.contains("Data." + path.getParent().getParent().toString() + ".Chunk."
													+ path.getFileName().toString());

									if (!found)
										return;
								}
							}
						}

						try {
							if (sendDebugMsg) {
								if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
									ServerBackup.getInstance().getLogger().log(Level.INFO,
											"Zipping '" + path + "'");

									if (Bukkit.getConsoleSender() != sender) {
										sender.sendMessage("Zipping '" + path);
									}
								}
							}

							zs.putNextEntry(zipEntry);

							if (!(System.getProperty("os.name").startsWith("Windows")
									&& path.toString().contains("session.lock"))) {
								try {
									Files.copy(path, zs);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							zs.closeEntry();
						} catch (IOException e) {
							e.printStackTrace();
							ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while zipping files.");
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
				ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while zipping files.");
				return;
			}

			long time = (System.nanoTime() - sTime) / 1000000;

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Files zipped. [" + time + "ms]");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			if (!isSaving) {
				File file = new File(sourceFilePath);

				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			sender.sendMessage(backup.processMessage("Command.Zip.Footer").replaceAll("%file%", sourceFilePath));

			BackupManager.tasks.remove("CREATE {" + sourceFilePath.replace("\\", "/") + "}");

			if (!fullBackup) {
				if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
					if (!sourceFilePath.equalsIgnoreCase(".")) {
						ServerBackup.getInstance().bpInf.set("Data." + sourceFilePath, "");

						new File(targetFilePath).renameTo(new File(targetFilePath.split("backup")[0] + "dynamic-backup"
								+ targetFilePath.split("backup")[1]));
						targetFilePath = targetFilePath.split("backup")[0] + "dynamic-backup"
								+ targetFilePath.split("backup")[1];

						ServerBackup.getInstance().saveBpInf();
					}
				}
			}

			if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.UploadBackup")) {
				FtpManager ftpm = new FtpManager(sender);
				ftpm.uploadFileToFtp(targetFilePath, false);
			}

			if(ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Dropbox")) {
				DropboxManager dm = new DropboxManager(sender);
				dm.uploadToDropbox(targetFilePath);
			}

			if(ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.WebDav")) {
				WebDavManager webDav = new WebDavManager(sender);

				try {
					webDav.uploadToWebDav(targetFilePath);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			// CHANGE
//			if(ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.GoogleDrive")) {
//				GDriveManager.uploadBasic(targetFilePath);
//			}

			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all.hasPermission("backup.notification")) {
					all.sendMessage(backup.processMessage("Info.BackupFinished").replaceAll("%file%", sourceFilePath));
				}
			}
		});

	}

	private void uploadToWebDav() {
		WebDavManager webDav = new WebDavManager(sender);

		String updatedFilePath = targetFilePath.replaceFirst(Pattern.quote(ServerBackup.getInstance().backupDestination), "");
		updatedFilePath = updatedFilePath.substring(2);

		try {
			webDav.uploadToWebDav(updatedFilePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void unzip() {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {

			long sTime = System.nanoTime();

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Start unzipping: " + sourceFilePath);
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			byte[] buffer = new byte[1024];
			try {
				File folder = new File(targetFilePath);
				if (!folder.exists()) {
					folder.mkdir();
				}
				ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(sourceFilePath)));
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String fileName = ze.getName();
					File newFile = new File(targetFilePath + File.separator + fileName);

					if (sendDebugMsg) {
						if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
							ServerBackup.getInstance().getLogger().log(Level.INFO, "Unzipping '" + newFile.getPath());

							if (Bukkit.getConsoleSender() != sender) {
								sender.sendMessage("Unzipping '" + newFile.getPath());
							}
						}
					}

					new File(newFile.getParent()).mkdirs();
					FileOutputStream fos = new FileOutputStream(newFile);
					if (!newFile.toPath().normalize().startsWith(targetFilePath)) {
						throw new Exception("Bad zip entry");
					}
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
					ze = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
			} catch (Exception e) {
				e.printStackTrace();
				ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while unzipping files.");
				return;
			}

			long time = (System.nanoTime() - sTime) / 1000000;

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Files unzipped. [" + time + "ms]");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			File file = new File(sourceFilePath);

			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			sender.sendMessage(backup.processMessage("Command.Unzip.Footer").replaceAll("%file%", sourceFilePath));
		});
	}

}
