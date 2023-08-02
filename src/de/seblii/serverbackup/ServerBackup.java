package de.seblii.serverbackup;

import de.seblii.serverbackup.commands.SBCommand;
import org.apache.commons.io.FileUtils;
import org.bstats.MetricsBase;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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

public class ServerBackup extends JavaPlugin implements Listener {

	private static ServerBackup sb;

	public static ServerBackup getInstance() {
		return sb;
	}

	public String backupDestination = "Backups//";

	public File backupInfo = new File("plugins//ServerBackup//backupInfo.yml");
	public YamlConfiguration bpInf = YamlConfiguration.loadConfiguration(backupInfo);

	public File cloudInfo = new File("plugins//ServerBackup//cloudAccess.yml");
	public YamlConfiguration cloud = YamlConfiguration.loadConfiguration(cloudInfo);

	public File messagesFile = new File("plugins//ServerBackup//messages.yml");
	public YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);

	boolean isUpdated = false;

	public boolean shutdownProgress = false;

	public String prefix = "";

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

		Bukkit.getPluginManager().registerEvents(this, this); //CHANGE
		Bukkit.getPluginManager().registerEvents(new DynamicBackup(), this);

		startTimer();

		this.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");

		if (getConfig().getBoolean("UpdateAvailableMessage")) {
			checkVersion(); //CHANGE
		}

		int mpid = 14673; //17639 //CHANGE

		Metrics metrics = new Metrics(this, mpid);

		metrics.addCustomChart(new SimplePie("player_per_server", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return String.valueOf(Bukkit.getOnlinePlayers().size());
			}
		}));

		metrics.addCustomChart(new SimplePie("using_ftp_server", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (getConfig().getBoolean("Ftp.UploadBackup")) {
					return "yes";
				} else {
					return "no";
				}
			}
		}));

		metrics.addCustomChart(new SimplePie("using_dropbox", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (getConfig().getBoolean("CloudBackup.Dropbox")) {
					return "yes";
				} else {
					return "no";
				}
			}
		}));

		metrics.addCustomChart(new SimplePie("using_gdrive", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (getConfig().getBoolean("CloudBackup.GoogleDrive")) {
					return "yes";
				} else {
					return "no";
				}
			}
		}));

		metrics.addCustomChart(new SingleLineChart("total_backup_space", new Callable<Integer>() {
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

							URL url = new URL("https://server-backup.net/assets/downloads/alt/ServerBackup.jar");

							if (Bukkit.getVersion().contains("1.18") || Bukkit.getVersion().contains("1.19")) {
								url = new URL("https://server-backup.net/assets/downloads/ServerBackup.jar");
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
						+ "\nIMPORTANT FTP information: Set 'UploadBackup' to 'true' if you want to store your backups on a ftp server (sftp does not work at the moment - if you host your own server (e.g. vps/root server) you need to set up a ftp server on it)."
						+ "\nIf you use ftp backups, you can set 'DeleteLocalBackup' to 'true' if you want the plugin to remove the created backup from your server once it has been uploaded to your ftp server."
						+ "\nCompressBeforeUpload compresses the backup to a zip file before uploading it. Set it to 'false' if you want the files to be uploaded directly to your ftp server."
						+ "\nJoin the discord server if you need help or have a question: https://discord.gg/rNzngsCWFC");
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

		getConfig().addDefault("UpdateAvailableMessage", true);
		getConfig().addDefault("AutomaticUpdates", true);

		getConfig().addDefault("BackupDestination", "Backups//");

		getConfig().addDefault("CloudBackup.Dropbox", false);
		//getConfig().addDefault("CloudBackup.GoogleDrive", true); //CHANGE
		getConfig().addDefault("CloudBackup.Options.Destination", "/");
		getConfig().addDefault("CloudBackup.Options.DeleteLocalBackup", false);

		getConfig().addDefault("Ftp.UploadBackup", false);
		getConfig().addDefault("Ftp.DeleteLocalBackup", false);
		getConfig().addDefault("Ftp.Server.IP", "127.0.0.1");
		getConfig().addDefault("Ftp.Server.Port", 21);
		getConfig().addDefault("Ftp.Server.User", "username");
		getConfig().addDefault("Ftp.Server.Password", "password");
		getConfig().addDefault("Ftp.Server.BackupDirectory", "Backups/");

		getConfig().addDefault("DynamicBackup", false);
		getConfig().addDefault("SendLogMessages", false);

		saveConfig();

		backupDestination = getConfig().getString("BackupDestination");

		if (getConfig().getBoolean("DynamicBackup")) {
			loadBpInf();
		}

		if (getConfig().getBoolean("CloudBackup.Dropbox")) {
			loadCloud();
		}

		loadMessages();
	}

	public void loadCloud() {
		if (!cloudInfo.exists()) {
			try {
				cloudInfo.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		cloud.options().header("Dropbox - Watch this video for explanation: https://youtu.be/k-0aIohxRUA"
				/*+"Google Drive - Watch this video for explanation: x"*/);

		if(!cloud.contains("Cloud.Dropbox")) {
			cloud.set("Cloud.Dropbox.AppKey", "appKey");
			cloud.set("Cloud.Dropbox.AppSecret", "appSecret");
		} else {
			if(cloud.getString("Cloud.Dropbox.AppKey") != "appKey" && !cloud.contains("Cloud.Dropbox.ActivationLink")) {
				cloud.set("Cloud.Dropbox.ActivationLink", "https://www.dropbox.com/oauth2/authorize?client_id=" + cloud.getString("Cloud.Dropbox.AppKey") + "&response_type=code&token_access_type=offline");
				if(!cloud.contains("Cloud.Dropbox.AccessToken")) {
					cloud.set("Cloud.Dropbox.AccessToken", "accessToken");
				}
			}
		}

		saveCloud();
	}

	public void saveCloud() {
		try {
			cloud.save(cloudInfo);
		} catch (IOException e) {
			e.printStackTrace();
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

		saveBpInf();
	}

	public void saveBpInf() {
		try {
			bpInf.save(backupInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadMessages() {
		if (!messagesFile.exists()) {
			try {
				messagesFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		messages.options().header("Use '&nl' to add a new line. Use '&' for color codes (e.g. '&4' for color red). For some messages you can use a placeholder (e.g. '%file%' for file name)." +
				"\nMinecraft color codes: https://htmlcolorcodes.com/minecraft-color-codes/");
		messages.options().copyDefaults(true);

		messages.addDefault("Prefix", "");

		messages.addDefault("Command.Zip.Header", "Zipping Backup..."
				+ "&nl");
		messages.addDefault("Command.Zip.Footer", "&nl"
				+ "&nlBackup [%file%] zipped."
				+ "&nlBackup [%file%] saved.");
		messages.addDefault("Command.Unzip.Header", "Unzipping Backup..."
				+ "&nl");
		messages.addDefault("Command.Unzip.Footer", "&nl"
				+ "&nlBackup [%file%] unzipped.");
		messages.addDefault("Command.Reload", "Config reloaded.");
		messages.addDefault("Command.Tasks.Header", "----- Backup tasks -----"
				+ "&nl");
		messages.addDefault("Command.Tasks.Footer", "&nl"
				+ "----- Backup tasks -----");
		messages.addDefault("Command.Shutdown.Start", "The server will shut down after backup tasks (check with: '/backup tasks') are finished."
				+ "&nlYou can cancel the shutdown by running this command again.");
		messages.addDefault("Command.Shutdown.Cancel", "Shutdown canceled.");

		messages.addDefault("Info.BackupFinished", "Backup [%file%] saved.");
		messages.addDefault("Info.BackupStarted", "Backup [%file%] started.");
		messages.addDefault("Info.BackupRemoved", "Backup [%file%] removed.");
		messages.addDefault("Info.FtpUpload", "Ftp: Uploading backup [%file%] ...");
		messages.addDefault("Info.FtpUploadSuccess", "Ftp: Upload successfully. Backup stored on ftp server.");
		messages.addDefault("Info.FtpDownload", "Ftp: Downloading backup [%file%] ...");
		messages.addDefault("Info.FtpDownloadSuccess", "Ftp: Download successful. Backup downloaded from ftp server.");

		messages.addDefault("Error.NoPermission", "&cI'm sorry but you do not have permission to perform this command.");
		messages.addDefault("Error.NoBackups", "No backups found.");
		messages.addDefault("Error.NoBackupFound", "No Backup named '%file%' found.");
		messages.addDefault("Error.NoBackupSearch", "No backups for search argument '%input%' found.");
		messages.addDefault("Error.DeletionFailed", "Error while deleting '%file%'.");
		messages.addDefault("Error.FolderExists", "There is already a folder named '%file%'.");
		messages.addDefault("Error.ZipExists", "There is already a ZIP file named '%file%.zip'.");
		messages.addDefault("Error.NoFtpBackups", "No ftp backups found.");
		messages.addDefault("Error.NoTasks", "No backup tasks are running.");
		messages.addDefault("Error.AlreadyZip", "%file% is already a ZIP file.");
		messages.addDefault("Error.NotAZip", "%file% is not a ZIP file.");
		messages.addDefault("Error.NotANumber", "%input% is not a valid number.");
		messages.addDefault("Error.BackupFailed", "An error occurred while saving Backup [%file%]. See console for more information.");
		messages.addDefault("Error.FtpUploadFailed", "Ftp: Error while uploading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
		messages.addDefault("Error.FtpDownloadFailed", "Ftp: Error while downloading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
		messages.addDefault("Error.FtpLocalDeletionFailed", "Ftp: Local backup deletion failed because the uploaded file was not found on the ftp server. Try again.");
		messages.addDefault("Error.FtpNotFound", "Ftp: ftp-backup %file% not found.");
		messages.addDefault("Error.FtpConnectionFailed", "Ftp: Error while connecting to FTP server.");

		saveMessages();

		prefix = ((messages.getString("Prefix").equals("")) ? messages.getString("Prefix") : (messages.getString("Prefix") + " "));
	}

	public void saveMessages() {
		try {
			messages.save(messagesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String processMessage(String msgCode) {
		return (prefix + messages.getString(msgCode)).replace("&nl", "\n").replace("&", "§");
	}

	public void startTimer() {
		if (getConfig().getBoolean("AutomaticBackups")) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(this, new BackupTimer(), 20 * 20, 20 * 20);
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
