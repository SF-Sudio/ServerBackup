package de.sebli.serverbackup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.sebli.serverbackup.commands.SBCommand;

public class ServerBackup extends JavaPlugin implements Listener {

	private static ServerBackup sb;

	public static ServerBackup getInstance() {
		return sb;
	}

	public static File folder = new File("Backups");

	@Override
	public void onDisable() {
		System.out.println("ServerBackup: Plugin disabled.");
	}

	@Override
	public void onEnable() {
		sb = this;

		loadFiles();

		getCommand("backup").setExecutor(new SBCommand());

		Bukkit.getPluginManager().registerEvents(this, this);

		startTimer();

		System.out.println("ServerBackup: Plugin enabled.");

		checkVersion();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		if (p.hasPermission("")) {
			if (p.hasPermission("backup.admin")) {
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

	private void checkVersion() {
		System.out.println("ServerBackup: Searching for updates...");

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
						System.out.println("ServerBackup: No updates found. The server is running the latest version.");
					} else {
						System.out.println("");
						System.out.println("ServerBackup: There is a newer version available - " + latest
								+ ", you are on - " + current);
						System.out.println(
								"ServerBackup: Please download the latest version - https://www.spigotmc.org/resources/"
										+ resourceID);
						System.out.println("");
					}
				}
			} catch (IOException exception) {
				System.err.println("ServerBackup: Cannot search for updates - " + exception.getMessage());
			}
		});

	}

	private void loadFiles() {
		if (!folder.exists()) {
			folder.mkdir();
		}

		getConfig().options().header(
				"BackupTimer = The time (in minutes) how often an automatic backup of the worlds (BackupWorlds) will be created."
						+ "\nType '0' at DeleteOldBackups to disable the deletion of old backups.");
		getConfig().options().copyDefaults(true);

		getConfig().addDefault("AutomaticBackups", true);
		getConfig().addDefault("BackupTimer", 1440);

		List<String> worlds = new ArrayList<>();
		worlds.add("world");
		worlds.add("world_nether");
		worlds.add("world_the_end");

		getConfig().addDefault("BackupWorlds", worlds);

		getConfig().addDefault("DeleteOldBackups", 7);

//		getConfig().addDefault("ZipCompression", true);

		if (getConfig().contains("ZipCompression")) {
			getConfig().set("ZipCompression", null);
		}

		getConfig().addDefault("SendLogMessages", false);

		saveConfig();
	}

	public void startTimer() {
		if (getConfig().getBoolean("AutomaticBackups")) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(this, new BackupTimer(),
					20 * 60 * getConfig().getInt("BackupTimer"), 20 * 60 * getConfig().getInt("BackupTimer"));
		}
	}

	public void stopTimer() {
		Bukkit.getScheduler().cancelTasks(this);
	}

}
