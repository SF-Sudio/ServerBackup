package de.seblii.serverbackup;

import de.seblii.serverbackup.utils.FtpManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

public class BackupManager {

	private String filePath;
	private CommandSender sender;
	private boolean fullBackup;

	public BackupManager(String filePath, CommandSender sender, boolean fullBackup) {
		this.filePath = filePath;
		this.sender = sender;
		this.fullBackup = fullBackup;
	}

	FtpManager ftpm = new FtpManager(Bukkit.getConsoleSender());

	public static List<String> tasks = new ArrayList<>();

	public void createBackup() {
		File worldFolder = new File(filePath);

		if (filePath.equalsIgnoreCase("@server")) {
			filePath = new File(".").getPath();
			worldFolder = new File(filePath);
		}

		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH-mm-ss");
		df.setTimeZone(TimeZone.getDefault());

		File backupFolder = new File(ServerBackup.getInstance().backupDestination + "//backup-" + df.format(date) + "-"
				+ filePath + "//" + filePath);

		if(ServerBackup.getInstance().getConfig().getBoolean("Ftp.UploadBackup")) {
			if (!ServerBackup.getInstance().getConfig().getBoolean("Ftp.CompressBeforeUpload")) {
				ftpm.uploadFileToFtp(filePath, true);

				return;
			}
		}

		if (worldFolder.exists()) {
			try {
				if (!backupFolder.exists()) {
					for (Player all : Bukkit.getOnlinePlayers()) {
						if (all.hasPermission("backup.notification")) {
							all.sendMessage(ServerBackup.getInstance().processMessage("Info.BackupStarted").replaceAll("%file%", worldFolder.getName()));
						}
					}

					ZipManager zm = new ZipManager(
							worldFolder.getPath(), ServerBackup.getInstance().backupDestination + "//backup-"
									+ df.format(date) + "-" + filePath.replaceAll("/", "-") + ".zip",
							Bukkit.getConsoleSender(), true, true, fullBackup);

					zm.zip();

					tasks.add("CREATE {" + filePath.replace("\\", "/") + "}");
				} else {
					ServerBackup.getInstance().getLogger().log(Level.WARNING, "Backup already exists.");
				}
			} catch (IOException e) {
				e.printStackTrace();

				ServerBackup.getInstance().getLogger().log(Level.WARNING, "Backup failed.");
			}
		} else {
			ServerBackup.getInstance().getLogger().log(Level.WARNING, "Couldn't find '" + filePath + "' folder.");
		}
	}

	public void removeBackup() {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
			File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);

			if (file.exists()) {
				if (file.isDirectory()) {
					try {
						FileUtils.deleteDirectory(file);

						sender.sendMessage(ServerBackup.getInstance().processMessage("Info.BackupRemoved").replaceAll("%file%", filePath));
					} catch (IOException e) {
						e.printStackTrace();

						sender.sendMessage(ServerBackup.getInstance().processMessage("Error.DeletionFailed").replaceAll("%file%", filePath));
					}
				} else {
					file.delete();

					sender.sendMessage(ServerBackup.getInstance().processMessage("Info.BackupRemoved").replaceAll("%file%", filePath));
				}
			} else {
				sender.sendMessage(ServerBackup.getInstance().processMessage("Error.NoBackupFound").replaceAll("%file%", filePath));
			}
		});
	}

}
