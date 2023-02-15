package de.seblii.serverbackup;

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

		if (worldFolder.exists()) {
			try {
				if (!backupFolder.exists()) {
					for (Player all : Bukkit.getOnlinePlayers()) {
						if (all.hasPermission("backup.notification")) {
							all.sendMessage("Backup [" + worldFolder + "] started.");
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

						sender.sendMessage("Backup [" + filePath + "] removed.");
					} catch (IOException e) {
						e.printStackTrace();

						sender.sendMessage("Error while deleting '" + filePath + "'.");
					}
				} else {
					file.delete();

					sender.sendMessage("Backup [" + filePath + "] removed.");
				}
			} else {
				sender.sendMessage("No Backup named '" + filePath + "' found.");
			}
		});
	}

}
