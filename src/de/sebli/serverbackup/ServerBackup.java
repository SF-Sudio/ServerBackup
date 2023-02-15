package de.sebli.serverbackup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
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

	File backupInfo = new File("plugins//ServerBackup//backupInfo.yml");
	YamlConfiguration bpInf = YamlConfiguration.loadConfiguration(backupInfo);

	boolean isUpdated = false;

	@Override
	public void onDisable() {
		stopTimer();

		for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
			task.cancel();

			this.getLogger().log(Level.WARNING, "WARNING - ServerBackup: Task [" + task.getTaskId()
					+ "] cancelled due to server shutdown. There might be some unfinished Backups.");
		}

		this.getLogger().log(Level.INFO, "ServerBackup: Plugin disabled.");
	}

	@Override
	public void onEnable() {
		sb = this;

		loadFiles();

		getCommand("backup").setExecutor(new SBCommand());

		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new DynamicBackup(), this);

		startTimer();

		this.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");

		if (getConfig().getBoolean("UpdateAvailableMessage")) {
			checkVersion();
		}

		int mpid = 14673;

		Metrics metrics = new Metrics(this, mpid);

		metrics.addCustomChart(new Metrics.SimplePie("player_per_server", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return String.valueOf(Bukkit.getOnlinePlayers().size());
			}
		}));

		metrics.addCustomChart(new Metrics.SimplePie("using_ftp_server", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (getConfig().getBoolean("Ftp.UploadBackup")) {
					return "yes";
				} else {
					return "no";
				}
			}
		}));

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
					} else {
						this.getLogger().log(Level.INFO, "ServerBackup: There is a newer version available - " + latest
								+ ", you are on - " + current);

						if (getConfig().getBoolean("AutomaticUpdates")) {
							this.getLogger().log(Level.INFO, "ServerBackup: Downloading newest version...");

							URL url = new URL("https://server-backup.net/assets/downloads/ServerBackup.jar");

							int bVer = Integer
									.parseInt(Bukkit.getVersion().split(" ")[Bukkit.getVersion().split(" ").length - 1]
											.replaceAll("\\)", "").replaceAll("\\.", ""));
							if (bVer < 118) {
								url = new URL("https://server-backup.net/assets/downloads/alt/ServerBackup.jar");
							}

							try (InputStream in = url.openStream();
									ReadableByteChannel rbc = Channels.newChannel(in);
									FileOutputStream fos = new FileOutputStream("plugins/ServerBackup.jar")) {
								fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

								this.getLogger().log(Level.INFO,
										"ServerBackup: Download finished. Please reload the server to complete the update.");

								isUpdated = true;
							}
						} else {
							this.getLogger().log(Level.INFO,
									"ServerBackup: Please download the latest version - https://server-backup.net/");
						}
					}
				}
			} catch (IOException exception) {
				this.getLogger().log(Level.WARNING,
						"ServerBackup: Cannot search for updates - " + exception.getMessage());
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
						+ "\nBlacklist - A list of files/directories that will not be backed up."
//						+ "\nCollectiveZipFile - Type 'true' if you want to have all backed up worlds in just one zip file.\n"
						+ "\nIMPORTANT FTP information: Set 'UploadBackup' to 'true' if you want to store your backups on a ftp server (sftp does not work at the moment - if you host your own server (e.g. vps/root server) you need to set up a ftp server on it)."
						+ "\nIf you use ftp backups, you can set 'DeleteLocalBackup' to 'true' if you want the plugin to remove the created backup from your server once it has been uploaded to your ftp server."
						+ "\nContact me if you need help or have a question: https://server-backup.net/#support");
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

		List<String> blacklist = new ArrayList<>();
		blacklist.add("libraries");
		blacklist.add("plugins/ServerBackup/config.yml");

		getConfig().addDefault("Blacklist", blacklist);

		getConfig().addDefault("DeleteOldBackups", 14);
		getConfig().addDefault("BackupLimiter", 0);

		getConfig().addDefault("KeepUniqueBackups", false);
//		getConfig().addDefault("CollectiveZipFile", false);
		if (getConfig().contains("CollectiveZipFile")) {
			getConfig().set("CollectiveZipFile", null);
		}

		getConfig().addDefault("UpdateAvailableMessage", true);
		getConfig().addDefault("AutomaticUpdates", true);

		if (getConfig().contains("UpdateAvailabeMessage")) {
			getConfig().set("UpdateAvailableMessage", getConfig().getBoolean("UpdateAvailabeMessage"));
			getConfig().set("UpdateAvailabeMessage", null);
		}

		if (getConfig().contains("BackupDestination")) {
			if (getConfig().getString("BackupDestination")
					.equalsIgnoreCase("- this feature will be available soon -")) {
				getConfig().set("BackupDestination", null);
			}
		}

		getConfig().addDefault("BackupDestination", "Backups//");

		getConfig().addDefault("Ftp.UploadBackup", false);
		getConfig().addDefault("Ftp.DeleteLocalBackup", false);
		getConfig().addDefault("Ftp.Server.IP", "127.0.0.1");
		getConfig().addDefault("Ftp.Server.Port", 21);
		getConfig().addDefault("Ftp.Server.User", "username");
		getConfig().addDefault("Ftp.Server.Password", "password");
		getConfig().addDefault("Ftp.Server.BackupDirectory", "Backups/");

		getConfig().addDefault("CheckWorldChange", false);
		getConfig().addDefault("DynamicBackup", false);
		getConfig().addDefault("SendLogMessages", false);

		if (getConfig().contains("FirstStart")) {
			getConfig().set("FirstStart", null);
		}

		saveConfig();

		backupDestination = getConfig().getString("BackupDestination");

		if (getConfig().getBoolean("DynamicBackup")) {
			loadBpInf();
		}
	}

	public void loadBpInf() {
		if (!backupInfo.exists()) {
			try {
				backupInfo.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			bpInf.save(backupInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveBpInf() {
		try {
			bpInf.save(backupInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

		if (p.hasPermission("backup.update")) {
			if (getConfig().getBoolean("UpdateAvailableMessage")) {
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
								if (isUpdated) {
									p.sendMessage("§8=====§fServerBackup§8=====");
									p.sendMessage("");
									p.sendMessage("§7There was a newer version available - §a" + latest
											+ "§7, you are on - §c" + current);
									p.sendMessage(
											"\n§7The latest version has been downloaded automatically, please reload the server to complete the update.");
									p.sendMessage("");
									p.sendMessage("§8=====§9Plugin by Seblii§8=====");
								} else {
									if (getConfig().getBoolean("AutomaticUpdates")) {
										if (p.hasPermission("backup.admin")) {
											p.sendMessage("§8=====§fServerBackup§8=====");
											p.sendMessage("");
											p.sendMessage("§7There is a newer version available - §a" + latest
													+ "§7, you are on - §c" + current);
											p.sendMessage("");
											p.sendMessage("§8=====§9Plugin by Seblii§8=====");
											p.sendMessage("");
											p.sendMessage("ServerBackup§7: Automatic update started...");

											URL url = new URL(
													"https://server-backup.net/assets/downloads/ServerBackup.jar");

											int bVer = Integer.parseInt(
													Bukkit.getVersion().split(" ")[Bukkit.getVersion().split(" ").length
															- 1].replaceAll("\\)", "").replaceAll("\\.", ""));

											if (bVer < 118) {
												url = new URL(
														"https://server-backup.net/assets/downloads/alt/ServerBackup.jar");
											}

											try (InputStream in = url.openStream();
													ReadableByteChannel rbc = Channels.newChannel(in);
													FileOutputStream fos = new FileOutputStream(
															"plugins/ServerBackup.jar")) {
												fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

												p.sendMessage(
														"ServerBackup§7: Download finished. Please reload the server to complete the update.");

												isUpdated = true;
											}
										}
									} else {
										p.sendMessage("§8=====§fServerBackup§8=====");
										p.sendMessage("");
										p.sendMessage("§7There is a newer version available - §a" + latest
												+ "§7, you are on - §c" + current);
										p.sendMessage(
												"§7Please download the latest version - §4https://server-backup.net/");
										p.sendMessage("");
										p.sendMessage("§8=====§9Plugin by Seblii§8=====");
									}
								}
							}
						}
					} catch (IOException exception) {
					}
				});
			}
		}
	}

}