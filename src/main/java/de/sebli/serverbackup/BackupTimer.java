package de.sebli.serverbackup;

import org.bukkit.Bukkit;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class BackupTimer implements Runnable {

    final List<String> worlds = ServerBackup.getInstance().getConfig().getStringList("BackupWorlds");
    final List<String> days = ServerBackup.getInstance().getConfig().getStringList("BackupTimer.Days");
    final List<String> times = ServerBackup.getInstance().getConfig().getStringList("BackupTimer.Times");

    final Calendar cal = Calendar.getInstance();

    @Override
    public void run() {
        boolean isBackupDay = days.stream().anyMatch(d -> d.equalsIgnoreCase(getDayName(cal.get(Calendar.DAY_OF_WEEK))));

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

                    int hour = Integer.parseInt(timeStr[0]);
                    int minute = Integer.parseInt(timeStr[1]);

                    if (cal.get(Calendar.HOUR_OF_DAY) == hour && cal.get(Calendar.MINUTE) == minute) {
                        for (String world : worlds) {
                            BackupManager bm = new BackupManager(world, Bukkit.getConsoleSender());
                            bm.createBackup();
                        }
                    }
                } catch (Exception e) {
                    ServerBackup.getInstance().getLogger().log(Level.WARNING,
                            "ServerBackup: Automatic Backup failed. Please check that you set the BackupTimer correctly.");
                }
            }
        }

        if (ServerBackup.getInstance().getConfig().getInt("BackupLimiter") <= 0) {
            if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) {
                if (ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups") <= 0)
                    return;

                File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

                if (backups.length == 0)
                    return;

                Arrays.sort(backups, Collections.reverseOrder());

                LocalDate date = LocalDate.now()
                        .minusDays(ServerBackup.getInstance().getConfig().getInt("DeleteOldBackups"));

                ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Backup deletion started...");

                long time = System.currentTimeMillis();

                List<String> backupNames = new ArrayList<>();

                for (File backup : backups) {
                    try {
                        String[] backupDateStr = backup.getName().split("-");
                        LocalDate backupDate = LocalDate.parse(
                                backupDateStr[1] + "-" + backupDateStr[2] + "-" + backupDateStr[3].split("~")[0]);
                        String backupName = backupDateStr[6];

                        if (ServerBackup.getInstance().getConfig().getBoolean("KeepUniqueBackups")) {
                            if (!backupNames.contains(backupName)) {
                                backupNames.add(backupName);
                                continue;
                            }
                        }

                        if (backupDate.isBefore(date.plusDays(1))) {
                            if (backup.exists()) {
                                backup.delete();

                                ServerBackup.getInstance().getLogger().log(Level.INFO,
                                        "Backup [" + backup.getName() + "] removed.");
                            } else {
                                ServerBackup.getInstance().getLogger().log(Level.WARNING,
                                        "No Backup named '" + backup.getName() + "' found.");
                            }
                        }
                    } catch (Exception ignored) {
                    }

                }

                ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup | Backup deletion finished. ["
                        + (System.currentTimeMillis() - time) + "ms]");
            }
        } else {
            File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();
            Arrays.sort(backups);

            int dobc = ServerBackup.getInstance().getConfig().getInt("BackupLimiter");
            int c = 0;

            while (backups.length > dobc) {
                if (backups[c].exists()) {
                    backups[c].delete();

                    ServerBackup.getInstance().getLogger().log(Level.INFO,
                            "Backup [" + backups[c].getName() + "] removed.");
                } else {
                    ServerBackup.getInstance().getLogger().log(Level.WARNING,
                            "No Backup named '" + backups[c].getName() + "' found.");
                }

                c++;
                dobc++;
            }
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

        ServerBackup.getInstance().getLogger().log(Level.WARNING, "Error while converting number in day.");

        return null;
    }

}
