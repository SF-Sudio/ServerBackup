package de.sebli.serverbackup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import de.sebli.serverbackup.commands.SBCommand;

public class ServerBackup extends JavaPlugin {

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

		startTimer();

		System.out.println("ServerBackup: Plugin enabled.");
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

		getConfig().addDefault("ZipCompression", true);

		getConfig().addDefault("SendLogMessages", false);

		saveConfig();
	}

	public void startTimer() {
		if (getConfig().getBoolean("AutomaticBackups")) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new BackupTimer(),
					20 * 60 * getConfig().getInt("BackupTimer"), 20 * 60 * getConfig().getInt("BackupTimer"));
		}
	}

	public void stopTimer() {
		Bukkit.getScheduler().cancelTasks(this);
	}

}
