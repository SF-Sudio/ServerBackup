package de.sebli.serverbackup;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BackupTimer implements Runnable {

	List<String> worlds = ServerBackup.getInstance().getConfig().getStringList("BackupWorlds");

	@Override
	public void run() {
		for (String world : worlds) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "backup create " + world);

			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all.hasPermission("backup.notification")) {
					all.sendMessage("Backup [" + world + "] saved.");
				}
			}
		}

		if (ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups") == 0)
			return;

		File[] backups = new File("Backups").listFiles(File::isDirectory);

		if (backups.length == 0)
			return;

		LocalDate date = LocalDate.now().minusDays(ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups"));
		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		for (int i = 0; i < backups.length; i++) {
			if (backups[i].getName().contains(df.format(date))) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "backup remove " + backups[i].getName());
			}
		}

		System.out.println("Removed old backups.");
	}

}
