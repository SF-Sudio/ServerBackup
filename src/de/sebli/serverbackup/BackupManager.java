package de.sebli.serverbackup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackupManager {

	private String filePath;
	private CommandSender sender;

	public BackupManager(String filePath, CommandSender sender) {
		this.filePath = filePath;
		this.sender = sender;
	}

	public void createBackup() {
		File worldFolder = new File(filePath);

		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH-mm-ss");
		df.setTimeZone(TimeZone.getDefault());

		File backupFolder = new File("Backups//backup-" + df.format(date) + "-" + filePath + "//" + filePath);

		if (worldFolder.exists()) {
			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all.hasPermission("backup.notification")) {
					all.sendMessage("Backup [" + worldFolder + "] started.");
				}
			}

			try {
				if (!backupFolder.exists()) {
					ZipManager zm = new ZipManager(worldFolder.getName(),
							"Backups//backup-" + df.format(date) + "-" + filePath + ".zip", Bukkit.getConsoleSender(),
							false, true);

					zm.zip();
				} else {
					System.err.println("Backup already exists.");
				}
			} catch (IOException e) {
				e.printStackTrace();

				System.out.println("Backup failed.");
			}
		} else {
			System.out.println("Couldn't find '" + filePath + "' folder.");
		}
	}

	public void removeBackup() {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
			File file = new File("Backups//" + filePath);

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
