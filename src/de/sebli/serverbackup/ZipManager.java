package de.sebli.serverbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ZipManager {

	private String sourceFilePath;
	private String targetFilePath;
	private CommandSender sender;
	private boolean sendDebugMsg;
	private boolean isSaving;

	public ZipManager(String sourceFilePath, String targetFilePath, CommandSender sender, boolean sendDebugMsg,
			boolean isSaving) {
		this.sourceFilePath = sourceFilePath;
		this.targetFilePath = targetFilePath;
		this.sender = sender;
		this.sendDebugMsg = sendDebugMsg;
		this.isSaving = isSaving;
	}

	public void zip() throws IOException {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {

			long sTime = System.nanoTime();

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Start zipping...");
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
					ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
					try {
						if (sendDebugMsg) {
							if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
								ServerBackup.getInstance().getLogger().log(Level.INFO, "Zipping '" + path.toString());

								if (Bukkit.getConsoleSender() != sender) {
									sender.sendMessage("Zipping '" + path.toString());
								}
							}
						}

						zs.putNextEntry(zipEntry);
						Files.copy(path, zs);
						zs.closeEntry();
					} catch (IOException e) {
						e.printStackTrace();
						ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while zipping files.");
						return;
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

			sender.sendMessage("");
			sender.sendMessage("Backup [" + sourceFilePath + "] zipped.");
			sender.sendMessage("Backup [" + sourceFilePath + "] saved.");

			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all.hasPermission("backup.notification")) {
					all.sendMessage("Backup [" + sourceFilePath + "] saved.");
				}
			}
		});
	}

	public void unzip() {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {

			long sTime = System.nanoTime();

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Start unzipping...");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			byte[] buffer = new byte[1024];
			try {
				File folder = new File(targetFilePath);
				if (!folder.exists()) {
					folder.mkdir();
				}
				ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFilePath));
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
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
					ze = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
			} catch (IOException e) {
				e.printStackTrace();
				ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while unzipping files.");
				return;
			}

			long time = (System.nanoTime() - sTime) / 1000000;

			ServerBackup.getInstance().getLogger().log(Level.INFO, "");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Files unzipped. [" + time + "ms]");
			ServerBackup.getInstance().getLogger().log(Level.INFO, "");

			File file = new File(sourceFilePath);

			file.delete();

			sender.sendMessage("");
			sender.sendMessage("Backup [" + sourceFilePath + "] unzipped.");
		});
	}

}
