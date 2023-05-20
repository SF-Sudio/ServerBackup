package de.seblii.serverbackup;

import de.seblii.serverbackup.commands.*;
import de.seblii.serverbackup.utils.FtpManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;

public class SBCommand implements CommandExecutor, TabCompleter {

	private final ServerBackup backup = ServerBackup.getInstance();

	final Map<String, ServerCommand> sbCommands = new HashMap<>();

	{
		sbCommands.put("shutdown", new ShutdownCommand());
		sbCommands.put("list", new ListCommand());
		sbCommands.put("reload", new ReloadCommand());
		sbCommands.put("rl", new ReloadCommand());
		sbCommands.put("tasks", new TasksCommand());
		sbCommands.put("task", new TasksCommand());
		sbCommands.put("ftp", new FtpCommand());
		sbCommands.put("zip", new ZipCommand());
		sbCommands.put("unzip", new UnzipCommand());
		sbCommands.put("remove", new RemoveCommand());
		sbCommands.put("search", new SearchCommand());
		sbCommands.put("dropbox", new DropboxCommand());
		sbCommands.put("create", new CreateCommand());
		sbCommands.put("webdav", new WebDavCommand());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender.hasPermission("backup.admin")) {
			if (args.length > 0 && sbCommands.containsKey(args[0])) {
				sbCommands.get(args[0]).execute(sender, args, backup);
			} else {
				sendHelp(sender);
			}
		} else {
			sender.sendMessage(backup.processMessage("Error.NoPermission"));
		}
		return false;
	}

	private void sendHelp(CommandSender sender) {
		sender.sendMessage("/backup reload - reloads the config");
		sender.sendMessage("");
		sender.sendMessage("/backup list <page> - shows a list of 10 backups");
		sender.sendMessage("");
		sender.sendMessage(
				"/backup search <search argument> <page> - shows a list of 10 backups that contain the given search argument");
		sender.sendMessage("");
		sender.sendMessage("/backup create <world> - creates a new backup of a world");
		sender.sendMessage("");
		sender.sendMessage("/backup remove <folder> - removes an existing backup");
		sender.sendMessage("");
		sender.sendMessage("/backup zip <folder> - zipping folder");
		sender.sendMessage("");
		sender.sendMessage("/backup unzip <file> - unzipping file");
		sender.sendMessage("");
		sender.sendMessage("/backup ftp <download/upload/list> - download, upload or list ftp backup files");
		sender.sendMessage("");
		sender.sendMessage("/backup dropbox upload <file> - upload backup files to dropbox");
		sender.sendMessage("");
		sender.sendMessage("/backup webdav upload <file> - upload backup files to dropbox");
		sender.sendMessage("");
		sender.sendMessage("/backup dropbox upload <file> - upload a backup to dropbox");
		sender.sendMessage("");
		sender.sendMessage("/backup shutdown - shut downs the server after backup tasks are finished");
		sender.sendMessage("");
		sender.sendMessage("/backup tasks - prints a list of currently running backup jobs");
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		List<String> commands = new ArrayList<>();

		if (sender.hasPermission("backup.admin")) {
			if (args.length == 1) {
				commands.add("reload");
				commands.add("list");
				commands.add("search");
				commands.add("create");
				commands.add("remove");
				commands.add("zip");
				commands.add("unzip");
				commands.add("ftp");
				commands.add("dropbox");
				commands.add("webdav");
				commands.add("tasks");
				commands.add("shutdown");

				StringUtil.copyPartialMatches(args[0], commands, completions);
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("list")) {
					File[] backups = getFiles();

					int maxPages = backups.length / 10;

					if (backups.length % 10 != 0) {
						maxPages++;
					}

					for (int i = 1; i < maxPages + 1; i++) {
						commands.add(String.valueOf(i));
					}
				} else if (args[0].equalsIgnoreCase("remove")) {
					File[] backups = getFiles();

					for (File file : backups) {
						commands.add(file.getName());
					}
				} else if (args[0].equalsIgnoreCase("create")) {
					for (World world : Bukkit.getWorlds()) {
						commands.add((!Bukkit.getWorldContainer().getPath().equalsIgnoreCase("."))
								? Bukkit.getWorldContainer() + "/" + world.getName()
								: world.getName());
					}

					commands.add("@server");
				} else if (args[0].equalsIgnoreCase("zip")) {
					File[] backups = getFiles();

					for (File backup : backups) {
						if (!backup.getName().endsWith(".zip")) {
							commands.add(backup.getName());
						}
					}
				} else if (args[0].equalsIgnoreCase("unzip")) {
					File[] backups = getFiles();

					for (File backup : backups) {
						if (backup.getName().endsWith(".zip")) {
							commands.add(backup.getName());
						}
					}
				} else if (args[0].equalsIgnoreCase("ftp")) {
					commands.add("list");
					commands.add("download");
					commands.add("upload");
				} else if (args[0].equalsIgnoreCase("dropbox")) {
					commands.add("upload");
				} else if (args[0].equalsIgnoreCase("webdav")) {
					commands.add("upload");
				}

				StringUtil.copyPartialMatches(args[1], commands, completions);
			} else if (args.length == 3) {
				if (args[0].equalsIgnoreCase("ftp")) {
					if (args[1].equalsIgnoreCase("list")) {
						FtpManager ftpm = new FtpManager(sender);

						List<String> backups = ftpm.getFtpBackupList();

						int maxPages = backups.size() / 10;

						if (backups.size() % 10 != 0) {
							maxPages++;
						}

						for (int i = 1; i < maxPages + 1; i++) {
							commands.add(String.valueOf(i));
						}
					} else if (args[1].equalsIgnoreCase("download")) {
						FtpManager ftpm = new FtpManager(sender);

						List<String> backups = ftpm.getFtpBackupList();

						for (String backup : backups) {
							commands.add(backup.split(" ")[1]);
						}
					} else if (args[1].equalsIgnoreCase("upload")) {
						File[] backups = getFiles();

						for (File backup : backups) {
							if (backup.getName().endsWith(".zip")) {
								commands.add(backup.getName());
							}
						}
					}

				} else if(args[0].equalsIgnoreCase("dropbox")) {
					File[] backups = getFiles();

					for (File backup : backups) {
							if (backup.getName().endsWith(".zip")) {
								commands.add(backup.getName());
							}
						}
				} else if(args[0].equalsIgnoreCase("webdav")) {
					File[] backups = getFiles();

					for (File backup : backups) {
						if (backup.getName().endsWith(".zip")) {
							commands.add(backup.getName());
						}
					}
				} else if (args[0].equalsIgnoreCase("create")) {
					commands.add("-full");
				}

				StringUtil.copyPartialMatches(args[2], commands, completions);
			} else if (args.length > 3) {
				if (args[0].equalsIgnoreCase("create")) {
					commands.add("-full");
				}

				StringUtil.copyPartialMatches(args[args.length - 1], commands, completions);
			}
		}

		Collections.sort(completions);

		return completions;
	}

	private static File[] getFiles() {
		File[] backups = new File(ServerBackup.getInstance().backupDestination).listFiles();
		assert backups != null;
		return backups;
	}

}
