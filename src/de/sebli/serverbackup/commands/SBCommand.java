package de.sebli.serverbackup.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.io.Files;

import de.sebli.serverbackup.BackupManager;
import de.sebli.serverbackup.FtpManager;
import de.sebli.serverbackup.ServerBackup;
import de.sebli.serverbackup.ZipManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class SBCommand implements CommandExecutor, TabCompleter {

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender.hasPermission("backup.admin")) {
			if (args.length == 1) {
				if (args[0].equalsIgnoreCase("list")) {
					Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
						File[] backups = new File(ServerBackup.getInstance().backupDestination).listFiles();

						if (backups.length == 0
								|| backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
							sender.sendMessage("No backups found.");

							return;
						}

						Arrays.sort(backups);

						if ((backups.length - 1) < 10) {
							sender.sendMessage(
									"----- Backup 1-" + (backups.length - 1) + "/" + (backups.length - 1) + " -----");
						} else {
							sender.sendMessage("----- Backup 1-10/" + (backups.length - 1) + " -----");
						}
						sender.sendMessage("");

						for (int i = 0; i < (backups.length - 1) && i < 10; i++) {
							if (backups[i].getName().equalsIgnoreCase("Files")) {
								i--;
								continue;
							}

							double fileSize = (double) FileUtils.sizeOf(backups[i]) / 1000 / 1000;
							fileSize = Math.round(fileSize * 100.0) / 100.0;

							if (sender instanceof Player) {
								Player p = (Player) sender;

								TextComponent msg = new TextComponent("§7[" + Integer.valueOf(i + 1) + "] §r"
										+ backups[i].getName() + " §7[" + fileSize + "MB]");
								msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										new ComponentBuilder("Click to get Backup name").create()));
								msg.setClickEvent(
										new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, backups[i].getName()));

								p.spigot().sendMessage(msg);
							} else {
								sender.sendMessage(backups[i].getName());
							}
						}

						int maxPages = (backups.length - 1) / 10;

						if ((backups.length - 1) % 10 != 0) {
							maxPages++;
						}

						sender.sendMessage("");
						sender.sendMessage("-------- Page 1/" + maxPages + " --------");
					});
				} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					ServerBackup.getInstance().reloadConfig();

					ServerBackup.getInstance().stopTimer();
					ServerBackup.getInstance().startTimer();

					String oldDes = ServerBackup.getInstance().backupDestination;

					if (!oldDes
							.equalsIgnoreCase(ServerBackup.getInstance().getConfig().getString("BackupDestination"))) {
						ServerBackup.getInstance().backupDestination = ServerBackup.getInstance().getConfig()
								.getString("BackupDestination");

						ServerBackup.getInstance().getLogger().log(Level.INFO,
								"ServerBackup: Backup destination [" + oldDes + " >> "
										+ ServerBackup.getInstance().backupDestination + "] updated successfully.");
					}

					ServerBackup.getInstance().loadFiles();

					sender.sendMessage("Config reloaded.");
				} else if (args[0].equalsIgnoreCase("tasks") || args[0].equalsIgnoreCase("task")) {
					if (BackupManager.tasks.size() > 0) {
						sender.sendMessage("----- Backup tasks -----");
						sender.sendMessage("");

						for (String task : BackupManager.tasks) {
							sender.sendMessage(task);
						}

						sender.sendMessage("");
						sender.sendMessage("----- Backup tasks -----");
					} else {
						sender.sendMessage("No backup tasks are running.");
					}
				} else {
					sendHelp(sender);
				}
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("list")) {
					Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
						File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

						if (backups.length == 0
								|| backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
							sender.sendMessage("No backups found.");

							return;
						}

						Arrays.sort(backups);

						try {
							int page = Integer.valueOf(args[1]);

							if ((backups.length - 1) < page * 10 - 9) {
								sender.sendMessage("Try a lower value.");

								return;
							}

							if ((backups.length - 1) <= page * 10 && (backups.length - 1) >= page * 10 - 10) {
								sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
										+ (backups.length - 1) + "/" + (backups.length - 1) + " -----");
							} else {
								sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
										+ Integer.valueOf(page * 10) + "/" + (backups.length - 1) + " -----");
							}
							sender.sendMessage("");

							for (int i = page * 10 - 10; i < (backups.length - 1) && i < page * 10; i++) {
								if (backups[0].getName().equalsIgnoreCase("Files")) {
									i--;
									continue;
								}

								double fileSize = (double) FileUtils.sizeOf(backups[i]) / 1000 / 1000;
								fileSize = Math.round(fileSize * 100.0) / 100.0;

								if (sender instanceof Player) {
									Player p = (Player) sender;

									TextComponent msg = new TextComponent("§7[" + Integer.valueOf(i + 1) + "] §r"
											+ backups[i].getName() + " §7[" + fileSize + "MB]");
									msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new ComponentBuilder("Click to get Backup name").create()));
									msg.setClickEvent(
											new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, backups[i].getName()));

									p.spigot().sendMessage(msg);
								} else {
									sender.sendMessage(backups[i].getName());
								}
							}

							int maxPages = (backups.length - 1) / 10;

							if ((backups.length - 1) % 10 != 0) {
								maxPages++;
							}

							sender.sendMessage("");
							sender.sendMessage("-------- Page " + page + "/" + maxPages + " --------");
						} catch (Exception e) {
							sender.sendMessage("'" + args[1] + "' is not a valid number.");
						}
					});
				} else if (args[0].equalsIgnoreCase("ftp")) {
					if (args[1].equalsIgnoreCase("list")) {
						Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
							FtpManager ftpm = new FtpManager(sender);

							List<String> backups = ftpm.getFtpBackupList();

							if (backups.size() == 0) {
								sender.sendMessage("No ftp backups found.");

								return;
							}

							if (backups.size() < 10) {
								sender.sendMessage(
										"----- Ftp-Backup 1-" + backups.size() + "/" + backups.size() + " -----");
							} else {
								sender.sendMessage("----- Ftp-Backup 1-10/" + backups.size() + " -----");
							}
							sender.sendMessage("");

							for (int i = 0; i < backups.size() && i < 10; i++) {
								if (sender instanceof Player) {
									Player p = (Player) sender;
									TextComponent msg = new TextComponent(backups.get(i));
									msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new ComponentBuilder("Click to get Backup name").create()));
									msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
											backups.get(i).split(" ")[1]));

									p.spigot().sendMessage(msg);
								} else {
									sender.sendMessage(backups.get(i));
								}
							}

							int maxPages = backups.size() / 10;

							if (backups.size() % 10 != 0) {
								maxPages++;
							}

							sender.sendMessage("");
							sender.sendMessage("--------- Page 1/" + maxPages + " ---------");
						});
					}
				} else if (args[0].equalsIgnoreCase("zip")) {
					String filePath = args[1];

					if (args[1].contains(".zip")) {
						sender.sendMessage("'" + args[1] + "' is already a ZIP file.");
						return false;
					}

					File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
					File newFile = new File(ServerBackup.getInstance().backupDestination + "//" + filePath + ".zip");

					if (!newFile.exists()) {
						sender.sendMessage("Zipping Backup...");
						sender.sendMessage("");

						if (file.exists()) {
							try {
								ZipManager zm = new ZipManager(file.getPath(), newFile.getPath(), sender, true, false);

								zm.zip();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							sender.sendMessage("No Backup named '" + args[1] + "' found.");
						}
					} else {
						sender.sendMessage("There is already a folder named '" + args[1].replaceAll(".zip", "") + "'");
					}
				} else if (args[0].equalsIgnoreCase("unzip")) {
					String filePath = args[1];

					if (!args[1].contains(".zip")) {
						sender.sendMessage("'" + args[1] + "' is not a ZIP file.");
						return false;
					}

					File file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
					File newFile = new File(
							ServerBackup.getInstance().backupDestination + "//" + filePath.replaceAll(".zip", ""));

					if (!newFile.exists()) {
						sender.sendMessage("Unzipping Backup...");
						sender.sendMessage("");

						if (file.exists()) {
							ZipManager zm = new ZipManager(file.getPath(),
									ServerBackup.getInstance().backupDestination + "//" + newFile.getName(), sender,
									false, true);

							zm.unzip();
						} else {
							sender.sendMessage("No Backup named '" + args[1] + "' found.");
						}
					} else {
						sender.sendMessage("There is already a ZIP file named '" + args[1] + ".zip'");
					}
				} else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
					if (args[1].equalsIgnoreCase("Files")) {
						sender.sendMessage("You can not delete the 'Files' backup folder.");

						return false;
					}

					BackupManager bm = new BackupManager(args[1], sender);

					bm.removeBackup();
				} else if (args[0].equalsIgnoreCase("search")) {
					Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
						File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

						if (backups.length == 0) {
							sender.sendMessage("No backups found.");

							return;
						}

						List<File> backupsMatch = new ArrayList<>();

						for (int i = 0; i < backups.length; i++) {
							if (backups[i].getName().contains(args[1])) {
								backupsMatch.add(backups[i]);
							}
						}

						if (backupsMatch.size() == 0) {
							sender.sendMessage("No backups for search argument '" + args[1] + "' found.");

							return;
						}

						Collections.sort(backupsMatch);

						int count = 1;

						if (backupsMatch.size() < 10) {
							sender.sendMessage(
									"----- Backup 1-" + backupsMatch.size() + "/" + backupsMatch.size() + " -----");
						} else {
							sender.sendMessage("----- Backup 1-10/" + backupsMatch.size() + " -----");
						}
						sender.sendMessage("");

						for (File file : backupsMatch) {
							if (count <= 10 && count <= backupsMatch.size()) {
								double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
								fileSize = Math.round(fileSize * 100.0) / 100.0;

								if (sender instanceof Player) {
									Player p = (Player) sender;

									TextComponent msg = new TextComponent("§7[" + Integer.valueOf(count) + "] §r"
											+ file.getName() + " §7[" + fileSize + "MB]");
									msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new ComponentBuilder("Click to get Backup name").create()));
									msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
											"/backup remove " + file.getName()));

									p.spigot().sendMessage(msg);
								} else {
									sender.sendMessage(file.getName());
								}
							}
							count++;
						}

						int maxPages = backupsMatch.size() / 10;

						if (backupsMatch.size() % 10 != 0) {
							maxPages++;
						}

						sender.sendMessage("");
						sender.sendMessage("-------- Page 1/" + maxPages + " --------");
					});
				}
			} else if (args.length == 3) {
				if (args[0].equalsIgnoreCase("search")) {
					Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
						File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

						if (backups.length == 0
								|| backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
							sender.sendMessage("No backups found.");

							return;
						}

						List<File> backupsMatch = new ArrayList<>();

						for (int i = 0; i < backups.length; i++) {
							if (backups[i].getName().contains(args[1])) {
								backupsMatch.add(backups[i]);
							}
						}

						if (backupsMatch.size() == 0) {
							sender.sendMessage("No backups for search argument '" + args[1] + "' found.");

							return;
						}

						Collections.sort(backupsMatch);

						try {
							int page = Integer.valueOf(args[2]);

							if (backups.length < page * 10 - 9) {
								sender.sendMessage("Try a lower value.");

								return;
							}

							int count = page * 10 - 9;

							if (backupsMatch.size() <= page * 10 && backupsMatch.size() >= page * 10 - 10) {
								sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
										+ backupsMatch.size() + "/" + backupsMatch.size() + " -----");
							} else {
								sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
										+ Integer.valueOf(page * 10) + "/" + backupsMatch.size() + " -----");
							}
							sender.sendMessage("");

							for (File file : backupsMatch) {
								if (count <= page * 10 && count <= backupsMatch.size()) {
									double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
									fileSize = Math.round(fileSize * 100.0) / 100.0;

									if (sender instanceof Player) {
										Player p = (Player) sender;

										TextComponent msg = new TextComponent("§7[" + Integer.valueOf(count) + "] §r"
												+ file.getName() + " §7[" + fileSize + "MB]");
										msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
												new ComponentBuilder("Click to get Backup name").create()));
										msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
												"/backup remove " + file.getName()));

										p.spigot().sendMessage(msg);
									} else {
										sender.sendMessage(file.getName());
									}
								}
								count++;
							}

							int maxPages = backupsMatch.size() / 10;

							if (backupsMatch.size() % 10 != 0) {
								maxPages++;
							}

							sender.sendMessage("");
							sender.sendMessage("-------- Page " + page + "/" + maxPages + " --------");
						} catch (Exception e) {
							sender.sendMessage("'" + args[2] + "' is not a valid number.");
						}
					});
				} else if (args[0].equalsIgnoreCase("ftp")) {
					if (args[1].equalsIgnoreCase("list")) {
						Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
							FtpManager ftpm = new FtpManager(sender);

							List<String> backups = ftpm.getFtpBackupList();

							if (backups.size() == 0) {
								sender.sendMessage("No ftp backups found.");

								return;
							}

							try {
								int page = Integer.valueOf(args[2]);

								if (backups.size() < page * 10 - 9) {
									sender.sendMessage("Try a lower value.");

									return;
								}

								if (backups.size() <= page * 10 && backups.size() >= page * 10 - 10) {
									sender.sendMessage("----- Ftp-Backup " + Integer.valueOf(page * 10 - 9) + "-"
											+ backups.size() + "/" + backups.size() + " -----");
								} else {
									sender.sendMessage("----- Ftp-Backup " + Integer.valueOf(page * 10 - 9) + "-"
											+ Integer.valueOf(page * 10) + "/" + backups.size() + " -----");
								}
								sender.sendMessage("");

								for (int i = page * 10 - 10; i < backups.size() && i < page * 10; i++) {
									if (sender instanceof Player) {
										Player p = (Player) sender;

										TextComponent msg = new TextComponent(backups.get(i));
										msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
												new ComponentBuilder("Click to get Backup name").create()));
										msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
												backups.get(i).split(" ")[1]));

										p.spigot().sendMessage(msg);
									} else {
										sender.sendMessage(backups.get(i));
									}
								}

								int maxPages = backups.size() / 10;

								if (backups.size() % 10 != 0) {
									maxPages++;
								}

								sender.sendMessage("");
								sender.sendMessage("--------- Page " + page + "/" + maxPages + " ---------");
							} catch (Exception e) {
								sender.sendMessage("'" + args[1] + "' is not a valid number.");
							}
						});
					} else if (args[1].equalsIgnoreCase("download")) {
						Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
							FtpManager ftpm = new FtpManager(sender);

							ftpm.downloadFileFromFtp(args[2]);
						});
					} else if (args[1].equalsIgnoreCase("upload")) {
						Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
							FtpManager ftpm = new FtpManager(sender);

							ftpm.uploadFileToFtp(args[2]);
						});
					}
				}
			} else if (args.length == 0) {
				sendHelp(sender);
			}

			if (args.length >= 2) {
				if (args[0].equalsIgnoreCase("create")) {
					String fileName = args[1];

					if (args.length > 2) {
						for (int i = 2; i < args.length; i++) {
							fileName = fileName + " " + args[i];
						}
					}

					File file = new File(fileName);

					if (!file.isDirectory() && !args[1].equalsIgnoreCase("@server")) {
						Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), new Runnable() {

							@Override
							public void run() {
								try {
									File des = new File(ServerBackup.getInstance().backupDestination + "//Files//"
											+ file.getName().replaceAll("/", "-"));

									if (des.exists()) {
										des = new File(des.getPath()
												.replaceAll("." + FilenameUtils.getExtension(des.getName()), "") + " "
												+ String.valueOf(System.currentTimeMillis() / 1000) + "."
												+ FilenameUtils.getExtension(file.getName()));
									}

									Files.copy(file, des);

									sender.sendMessage("Backup [" + file.getName() + "] saved.");
								} catch (IOException e) {
									sender.sendMessage("An error occured while saving Backup [" + file.getName()
											+ "]. See console for more information.");
									e.printStackTrace();
								}
							}

						});
					} else {
						BackupManager bm = new BackupManager(fileName, sender);

						bm.createBackup();
					}
				} else {
					sendHelp(sender);
				}
			}
		} else {
			sender.sendMessage("§cI'm sorry but you do not have permission to perform this command.");
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
		sender.sendMessage(
				"/backup ftp <download/upload/list> - download, upload or list ftp backup files");
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
				commands.add("tasks");

				StringUtil.copyPartialMatches(args[0], commands, completions);
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("list")) {
					File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

					int maxPages = backups.length / 10;

					if (backups.length % 10 != 0) {
						maxPages++;
					}

					for (int i = 1; i < maxPages + 1; i++) {
						commands.add(String.valueOf(i));
					}
				} else if (args[0].equalsIgnoreCase("remove")) {
					File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

					for (int i = 0; i < backups.length; i++) {
						commands.add(backups[i].getName());
					}
				} else if (args[0].equalsIgnoreCase("create")) {
					for (World world : Bukkit.getWorlds()) {
						commands.add((!Bukkit.getWorldContainer().getPath().equalsIgnoreCase("."))
								? Bukkit.getWorldContainer() + "/" + world.getName()
								: world.getName());
					}

					commands.add("@server");
				} else if (args[0].equalsIgnoreCase("zip")) {
					File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

					for (File backup : backups) {
						if (!backup.getName().endsWith(".zip")) {
							commands.add(backup.getName());
						}
					}
				} else if (args[0].equalsIgnoreCase("unzip")) {
					File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

					for (File backup : backups) {
						if (backup.getName().endsWith(".zip")) {
							commands.add(backup.getName());
						}
					}
				} else if (args[0].equalsIgnoreCase("ftp")) {
					commands.add("list");
					commands.add("download");
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
						File[] backups = new File(ServerBackup.getInstance().backupDestination + "").listFiles();

						for (File backup : backups) {
							if (backup.getName().endsWith(".zip")) {
								commands.add(backup.getName());
							}
						}
					}
				}

				StringUtil.copyPartialMatches(args[2], commands, completions);
			}
		}

		Collections.sort(completions);

		return completions;
	}

}
