package de.sebli.serverbackup;

import java.io.File;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import org.bukkit.Bukkit;

public class BackupTimer implements Runnable {

	List<String> worlds = ServerBackup.getInstance().getConfig().getStringList("BackupWorlds");
	List<String> days = ServerBackup.getInstance().getConfig().getStringList("BackupTimer.Days");
	List<String> times = ServerBackup.getInstance().getConfig().getStringList("BackupTimer.Times");

	Calendar cal = Calendar.getInstance();

	@Override
	public void run() {
		cal = Calendar.getInstance();

		boolean isBackupDay = days.stream().filter(d -> d.equalsIgnoreCase(getDayName(cal.get(Calendar.DAY_OF_WEEK))))
				.findFirst().isPresent();

		if (isBackupDay) {
			for (String time : times) {
				try {
					String[] timeStr = time.split("-");

					if (timeStr[0].startsWith("0")) {
						timeStr[0] = timeStr[0].substring(1);
					}

					if (timeStr[1].startsWith("0")) {
						timeStr[1] = timeStr[1].substring(1);
					}

					int hour = Integer.valueOf(timeStr[0]);
					int minute = Integer.valueOf(timeStr[1]);

					if (cal.get(Calendar.HOUR_OF_DAY) == hour && cal.get(Calendar.MINUTE) == minute) {
						for (String world : worlds) {
							BackupManager bm = new BackupManager(world, Bukkit.getConsoleSender());
							bm.createBackup();
						}
					}
				} catch (Exception e) {
					System.err.println(
							"ServerBackup: Automatic Backup failed. Please check that you set the BackupTimer correctly.");
				}
			}
		}

		if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) {
			if (ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups") <= 0)
				return;

			File[] backups = new File("Backups").listFiles();

			if (backups.length == 0)
				return;

			LocalDate date = LocalDate.now()
					.minusDays(ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups"));

			System.out.println("");
			System.out.println("ServerBackup | Backup deletion started...");
			System.out.println("");

			long time = System.currentTimeMillis();

			for (int i = 0; i < backups.length; i++) {
				String[] backupDateStr = backups[i].getName().split("-");
				LocalDate backupDate = LocalDate
						.parse(backupDateStr[1] + "-" + backupDateStr[2] + "-" + backupDateStr[3].split("~")[0]);

				if (backupDate.isBefore(date.plusDays(1))) {
					if (backups[i].exists()) {
						backups[i].delete();

						System.out.println("Backup [" + backups[i].getName() + "] removed.");
					} else {
						System.out.println("No Backup named '" + backups[i].getName() + "' found.");
					}
				}
//				if (backups[i].getName().contains(df.format(date))) {
//					if (backups[i].exists()) {
//						backups[i].delete();
//
//						System.out.println("Backup [" + backups[i].getName() + "] removed.");
//					} else {
//						System.out.println("No Backup named '" + backups[i].getName() + "' found.");
//					}
//				}
			}

			System.out.println("");
			System.out.println("ServerBackup | Backup deletion finished. ["
					+ Long.valueOf(System.currentTimeMillis() - time) + "ms]");
			System.out.println("");
		}
	}

	private String getDayName(int dayNumber) {
		if (dayNumber == 1) {
			return "SUNDAY";
		}

		if (dayNumber == 2) {
			return "MONDAY";
		}

		if (dayNumber == 3) {
			return "TUESDAY";
		}

		if (dayNumber == 4) {
			return "WEDNESDAY";
		}

		if (dayNumber == 5) {
			return "THURSDAY";
		}

		if (dayNumber == 6) {
			return "FRIDAY";
		}

		if (dayNumber == 7) {
			return "SATURDAY";
		}

		System.err.println("Error while converting number in day.");

		return null;
	}

}
