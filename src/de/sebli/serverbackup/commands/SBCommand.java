package de.sebli.serverbackup.commands;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.sebli.serverbackup.ServerBackup;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class SBCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender.hasPermission("backup.admin")) {
			if (args.length == 1) {
				if (args[0].equalsIgnoreCase("list")) {
					File[] backups = new File("Backups").listFiles(File::isDirectory);

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					Arrays.sort(backups);

					sender.sendMessage("----- Backup 1-10/" + backups.length + " -----");
					sender.sendMessage("");

					for (int i = 0; i < backups.length && i < 10; i++) {
						TextComponent msg = new TextComponent(
								"[" + Integer.valueOf(i + 1) + "] " + backups[i].getName());
						msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder("Click to delete this backup").create()));
						msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
								"/backup remove " + backups[i].getName()));

						if (sender instanceof Player) {
							Player p = (Player) sender;

							p.spigot().sendMessage(msg);
						} else {
							sender.sendMessage(backups[i].getName());
						}
					}

					int maxPages = backups.length / 10;

					if (backups.length % 10 != 0) {
						maxPages++;
					}

					sender.sendMessage("");
					sender.sendMessage("-------- Page 1/" + maxPages + " --------");
				} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					ServerBackup.getInstance().reloadConfig();

					ServerBackup.getInstance().stopTimer();
					ServerBackup.getInstance().startTimer();

					sender.sendMessage("Config reloaded.");
				} else {
					sendHelp(sender);
				}
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
					String filePath = args[1];

					File file = new File("Backups//" + filePath);

					if (file.exists()) {
						try {
							FileUtils.deleteDirectory(file);

							sender.sendMessage("Backup [" + args[1] + "] removed.");
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						sender.sendMessage("No backup found.");
					}
				} else if (args[0].equalsIgnoreCase("create")) {
					File worldFolder = new File(args[1]);

					Date date = new Date();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH:mm:ss");
					df.setTimeZone(TimeZone.getDefault());

					File backupFolder = new File("Backups//backup-" + df.format(date) + "-" + args[1] + "//world");

					if (worldFolder.exists()) {
						sender.sendMessage("Starting Backup...");

						try {
							if (!backupFolder.exists()) {
								FileUtils.copyDirectory(worldFolder, backupFolder);

								sender.sendMessage("Backup [" + args[1] + "] saved.");
							} else {
								sender.sendMessage("Backup already exists.");
							}
						} catch (IOException e) {
							e.printStackTrace();

							sender.sendMessage("Error.");
						}
					} else {
						sender.sendMessage("Couldn't find world folder.");
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					File[] backups = new File("Backups").listFiles(File::isDirectory);

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					Arrays.sort(backups);

					try {
						int page = Integer.valueOf(args[1]);

						if (backups.length < page * 10 - 9) {
							sender.sendMessage("Try a lower value.");

							return false;
						}

						sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
								+ Integer.valueOf(page * 10) + "/" + backups.length + " -----");
						sender.sendMessage("");

						for (int i = page * 10 - 10; i < backups.length && i < page * 10; i++) {
							TextComponent msg = new TextComponent(
									"[" + Integer.valueOf(i + 1) + "] " + backups[i].getName());
							msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
									new ComponentBuilder("Click to delete this backup").create()));
							msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
									"/backup remove " + backups[i].getName()));

							if (sender instanceof Player) {
								Player p = (Player) sender;

								p.spigot().sendMessage(msg);
							} else {
								sender.sendMessage(backups[i].getName());
							}
						}

						int maxPages = backups.length / 10;

						if (backups.length % 10 != 0) {
							maxPages++;
						}

						sender.sendMessage("");
						sender.sendMessage("-------- Page " + page + "/" + maxPages + " --------");
					} catch (Exception e) {
						boolean fileFound = false;

						for (int i = 0; i < backups.length && i < 10; i++) {
							if (backups[i].getName().contains(args[1])) {
								TextComponent msg = new TextComponent(backups[i].getName());
								msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new ComponentBuilder("Click to delete this backup").create()));
								msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
										"/backup remove " + backups[i].getName()));

								if (sender instanceof Player) {
									Player p = (Player) sender;

									p.spigot().sendMessage(msg);
								} else {
									sender.sendMessage(backups[i].getName());
								}

								fileFound = true;
							}
						}

						if (!fileFound) {
							sender.sendMessage("No backups for search argument '" + args[1] + "' found.");
						}
					}
				} else {
					sendHelp(sender);
				}
			} else if (args.length == 3) {
				if (args[0].equalsIgnoreCase("list")) {
					File[] backups = new File("Backups").listFiles(File::isDirectory);

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					Arrays.sort(backups);

					try {
						int page = Integer.valueOf(args[2]);

						if (backups.length < page * 10 - 9) {
							sender.sendMessage("Try a lower value.");

							return false;
						}

						for (int i = page * 10 - 10; i < backups.length && i < page * 10; i++) {
							if (backups[i].getName().contains(args[1])) {
								TextComponent msg = new TextComponent(backups[i].getName());
								msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new ComponentBuilder("Click to delete this backup").create()));
								msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
										"/backup remove " + backups[i].getName()));

								if (sender instanceof Player) {
									Player p = (Player) sender;

									p.spigot().sendMessage(msg);
								} else {
									sender.sendMessage(backups[i].getName());
								}
							}
						}

					} catch (Exception e) {
						sender.sendMessage("'" + args[2] + "' is not a valid number.");
					}
				}
			} else {
				sendHelp(sender);
			}
		} else {
			sender.sendMessage("§cI'm sorry but you do not have permission to perform this command.");
		}

		return false;
	}

	private void sendHelp(CommandSender sender) {
		sender.sendMessage("/backup reload - reloads the config");
		sender.sendMessage("");
		sender.sendMessage("/backup list - shows a list of all backups");
		sender.sendMessage("");
		sender.sendMessage(
				"/backup list <search argument> - shows a list of all backups that contain the given search arguments");
		sender.sendMessage("");
		sender.sendMessage("/backup create <world> - creates a new backup of a world");
		sender.sendMessage("");
		sender.sendMessage("/backup remove <folder> - removes an existing backup");
	}

}
