package de.sebli.serverbackup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import de.sebli.serverbackup.commands.SBCommand;
import de.sebli.serverbackup.utils.Metrics;

public class ServerBackup extends JavaPlugin implements Listener {

	private static ServerBackup sb;

	public static ServerBackup getInstance() {
		return sb;
	}

	public String backupDestination = "Backups//";

	@Override
	public void onDisable() {
		stopTimer();

		for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
			task.cancel();

			this.getLogger().log(Level.WARNING, "WARNING - ServerBackup: Task [" + task.getTaskId()
					+ "] cancelled due to server shutdown. There might be some unfinished Backups.");
		}

		this.getLogger().log(Level.INFO, "ServerBackup: Plugin disabled.");
//		System.out.println("ServerBackup: Plugin disabled.");
	}

	@Override
	public void onEnable() {
		sb = this;

		loadFiles();

		getCommand("backup").setExecutor(new SBCommand());

		Bukkit.getPluginManager().registerEvents(this, this);

		startTimer();

		this.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");
//		System.out.println("ServerBackup: Plugin enabled.");

		if (getConfig().getBoolean("UpdateAvailabeMessage")) {
			checkVersion();
		}

		if (getConfig().getBoolean("FirstStart")) {
			if (System.getProperty("os.name").startsWith("Windows")) {
				Bukkit.getScheduler().runTaskLater(this, new Runnable() {
					@Override
					public void run() {
						ServerBackup.getInstance().getLogger().log(Level.WARNING,
								"ServerBackup: Seems like you running this plugin on Windows. Please keep in mind, that this plugin does not support Windows officially. It may work fine, but some features might not work properly.");
						getConfig().set("FirstStart", false);
					}
				}, 30);
			} else {
				getConfig().set("FirstStart", false);
			}
			saveConfig();
		}

		int mpid = 14673;

		Metrics metrics = new Metrics(this, mpid);

		metrics.addCustomChart(new Metrics.SingleLineChart("total_backup_space", new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				File file = new File(backupDestination);

				double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
				fileSize = Math.round(fileSize * 100.0) / 100.0;

				return (int) fileSize;
			}

		}));
	}

	private void checkVersion() {
		this.getLogger().log(Level.INFO, "ServerBackup: Searching for updates...");

		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			int resourceID = 79320;
			try (InputStream inputStream = (new URL(
					"https://api.spigotmc.org/legacy/update.php?resource=" + resourceID)).openStream();
					Scanner scanner = new Scanner(inputStream)) {
				if (scanner.hasNext()) {
					String latest = scanner.next();
					String current = getDescription().getVersion();

					int late = Integer.parseInt(latest.replaceAll("\\.", ""));
					int curr = Integer.parseInt(current.replaceAll("\\.", ""));

					if (curr >= late) {
						this.getLogger().log(Level.INFO,
								"ServerBackup: No updates found. The server is running the latest version.");

//						System.out.println("ServerBackup: No updates found. The server is running the latest version.");
					} else {
						this.getLogger().log(Level.INFO, "\nServerBackup: There is a newer version available - "
								+ latest + ", you are on - " + current);
						this.getLogger().log(Level.INFO,
								"ServerBackup: Please download the latest version - https://www.spigotmc.org/resources/"
										+ resourceID + "\n");

//						System.out.println("");
//						System.out.println("ServerBackup: There is a newer version available - " + latest
//								+ ", you are on - " + current);
//						System.out.println(
//								"ServerBackup: Please download the latest version - https://www.spigotmc.org/resources/"
//										+ resourceID);
//						System.out.println("");
					}
				}
			} catch (IOException exception) {
				this.getLogger().log(Level.WARNING,
						"ServerBackup: Cannot search for updates - " + exception.getMessage());
//				System.err.println("ServerBackup: Cannot search for updates - " + exception.getMessage());
			}
		});

	}

	@SuppressWarnings("deprecation")
	public void loadFiles() {
		if (getConfig().contains("BackupDestination"))
			backupDestination = getConfig().getString("BackupDestination");

		if (!Files.exists(Paths.get(backupDestination))) {
			try {
				Files.createDirectories(Paths.get(backupDestination));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		File files = new File(backupDestination + "//Files");

		if (!files.exists()) {
			files.mkdir();
		}

		getConfig().options()
				.header("BackupTimer = At what time should a Backup be created? The format is: 'hh-mm' e.g. '12-30'."
						+ "\nDeleteOldBackups = Deletes old backups automatically after a specific time (in days, standard = 7 days)"
						+ "\nDeleteOldBackups - Type '0' at DeleteOldBackups to disable the deletion of old backups."
						+ "\nBackupLimiter = Deletes old backups automatically if number of total backups is greater than this number (e.g. if you enter '5' - the oldest backup will be deleted if there are more than 5 backups, so you will always keep the latest 5 backups)"
						+ "\nBackupLimiter - Type '0' to disable this feature. If you don't type '0' the feature 'DeleteOldBackups' will be disabled and this feature ('BackupLimiter') will be enabled."
						+ "\nKeepUniqueBackups - Type 'true' to disable the deletion of unique backups. The plugin will keep the newest backup of all backed up worlds or folders, no matter how old it is."
						+ "\nCollectiveZipFile - Type 'true' if you want to have all backed up worlds in just one zip file.");
		getConfig().options().copyDefaults(true);

		getConfig().addDefault("AutomaticBackups", true);

		List<String> days = new ArrayList<>();
		days.add("MONDAY");
		days.add("TUESDAY");
		days.add("WEDNESDAY");
		days.add("THURSDAY");
		days.add("FRIDAY");
		days.add("SATURDAY");
		days.add("SUNDAY");

		List<String> times = new ArrayList<>();
		times.add("00-00");

		getConfig().addDefault("BackupTimer.Days", days);
		getConfig().addDefault("BackupTimer.Times", times);

		List<String> worlds = new ArrayList<>();
		worlds.add("world");
		worlds.add("world_nether");
		worlds.add("world_the_end");

		getConfig().addDefault("BackupWorlds", worlds);

		getConfig().addDefault("DeleteOldBackups", 7);
		getConfig().addDefault("BackupLimiter", 0);

		getConfig().addDefault("KeepUniqueBackups", false);
		getConfig().addDefault("CollectiveZipFile", false);
		getConfig().addDefault("UpdateAvailabeMessage", true);

//		getConfig().addDefault("ZipCompression", true);

		if (getConfig().contains("ZipCompression")) {
			getConfig().set("ZipCompression", null);
		}

		if (getConfig().contains("BackupDestination")) {
			if (getConfig().getString("BackupDestination")
					.equalsIgnoreCase("- this feature will be available soon -")) {
				getConfig().set("BackupDestination", null);
			}
		}

		getConfig().addDefault("BackupDestination", "Backups//");

		getConfig().addDefault("SendLogMessages", false);
		getConfig().addDefault("FirstStart", true);

		saveConfig();

		backupDestination = getConfig().getString("BackupDestination");
	}

	public void startTimer() {
		if (getConfig().getBoolean("AutomaticBackups")) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(this, new BackupTimer(), 20 * 60, 20 * 60);
		}
	}

	public void stopTimer() {
		Bukkit.getScheduler().cancelTasks(this);
	}

	// Events
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		if (p.hasPermission("backup.admin")) {
			if (getConfig().getBoolean("UpdateAvailabeMessage")) {
				Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
					int resourceID = 79320;
					try (InputStream inputStream = (new URL(
							"https://api.spigotmc.org/legacy/update.php?resource=" + resourceID)).openStream();
							Scanner scanner = new Scanner(inputStream)) {
						if (scanner.hasNext()) {
							String latest = scanner.next();
							String current = getDescription().getVersion();

							int late = Integer.parseInt(latest.replaceAll("\\.", ""));
							int curr = Integer.parseInt(current.replaceAll("\\.", ""));

							if (curr >= late) {
							} else {
								p.sendMessage("§8=====§fServerBackup§8=====");
								p.sendMessage("");
								p.sendMessage("§7There is a newer version available - §a" + latest
										+ "§7, you are on - §c" + current);
								p.sendMessage(
										"§7Please download the latest version - §4https://www.spigotmc.org/resources/"
												+ resourceID);
								p.sendMessage("");
								p.sendMessage("§8=====§9Plugin by Seblii§8=====");
							}
						}
					} catch (IOException exception) {
					}
				});
			}
		}
	}

}
