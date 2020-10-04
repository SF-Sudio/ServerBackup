package de.sebli.serverbackup.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
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
					File[] backups = new File("Backups").listFiles();

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					Arrays.sort(backups);

					if (backups.length < 10) {
						sender.sendMessage("----- Backup 1-" + backups.length + "/" + backups.length + " -----");
					} else {
						sender.sendMessage("----- Backup 1-10/" + backups.length + " -----");
					}
					sender.sendMessage("");

					for (int i = 0; i < backups.length && i < 10; i++) {
						double fileSize = (double) FileUtils.sizeOf(backups[i]) / 1000 / 1000;
						fileSize = Math.round(fileSize * 100.0) / 100.0;

						TextComponent msg = new TextComponent("§7[" + Integer.valueOf(i + 1) + "] §r"
								+ backups[i].getName() + " §7[" + fileSize + "MB]");
						msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								new ComponentBuilder("Click to get Backup name").create()));
						msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, backups[i].getName()));

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
				if (args[0].equalsIgnoreCase("list")) {
					File[] backups = new File("Backups").listFiles();

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

						if (backups.length <= page * 10 && backups.length >= page * 10 - 10) {
							sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-" + backups.length
									+ "/" + backups.length + " -----");
						} else {
							sender.sendMessage("----- Backup " + Integer.valueOf(page * 10 - 9) + "-"
									+ Integer.valueOf(page * 10) + "/" + backups.length + " -----");
						}
						sender.sendMessage("");

						for (int i = page * 10 - 10; i < backups.length && i < page * 10; i++) {
							double fileSize = (double) FileUtils.sizeOf(backups[i]) / 1000 / 1000;
							fileSize = Math.round(fileSize * 100.0) / 100.0;

							TextComponent msg = new TextComponent("§7[" + Integer.valueOf(i + 1) + "] §r"
									+ backups[i].getName() + " §7[" + fileSize + "MB]");
							msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
									new ComponentBuilder("Click to get Backup name").create()));
							msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, backups[i].getName()));

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
						sender.sendMessage("'" + args[1] + "' is not a valid number.");
					}
				} else if (args[0].equalsIgnoreCase("zip")) {
					String filePath = args[1];

					if (args[1].contains(".zip")) {
						sender.sendMessage("'" + args[1] + "' is already a ZIP file.");
						return false;
					}

					File file = new File("Backups//" + filePath);
					File newFile = new File("Backups//" + filePath + ".zip");

					if (!newFile.exists()) {
						sender.sendMessage("Zipping Backup...");
						sender.sendMessage("");

						if (file.exists()) {
							try {
								zip(file.getPath(), newFile.getPath(), sender, true, false);
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

					File file = new File("Backups//" + filePath);
					File newFile = new File("Backups//" + filePath.replaceAll(".zip", ""));

					if (!newFile.exists()) {
						sender.sendMessage("Unzipping Backup...");
						sender.sendMessage("");

						if (file.exists()) {
							unzip(file.getPath(), "Backups//" + newFile.getName(), sender, true);
						} else {
							sender.sendMessage("No Backup named '" + args[1] + "' found.");
						}
					} else {
						sender.sendMessage("There is already a ZIP file named '" + args[1] + ".zip'");
					}
				} else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
					String filePath = args[1];

					File file = new File("Backups//" + filePath);

					if (file.exists()) {
						file.delete();

						sender.sendMessage("Backup [" + args[1] + "] removed.");
					} else {
						sender.sendMessage("No Backup named '" + args[1] + "' found.");
					}
				} else if (args[0].equalsIgnoreCase("create")) {
					File worldFolder = new File(args[1]);

					Date date = new Date();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'~'HH-mm-ss");
					df.setTimeZone(TimeZone.getDefault());

					File backupFolder = new File("Backups//backup-" + df.format(date) + "-" + args[1] + "//" + args[1]);

					if (worldFolder.exists()) {
						sender.sendMessage("Starting Backup...");

						try {
							if (!backupFolder.exists()) {
//								if (ServerBackup.getInstance().getConfig().getBoolean("ZipCompression")) {
								zip(worldFolder.getName(),
										"Backups//backup-" + df.format(date) + "-" + args[1] + ".zip", sender, false,
										true);
//								} else {
//									FileUtils.copyDirectory(worldFolder, backupFolder);
//									sender.sendMessage("Backup [" + args[1] + "] saved.");
//								}
							} else {
								sender.sendMessage("Backup already exists.");
							}
						} catch (IOException e) {
							e.printStackTrace();

							sender.sendMessage("Error.");
						}
					} else {
						sender.sendMessage("Couldn't find '" + args[1] + "' folder.");
					}
				} else if (args[0].equalsIgnoreCase("search")) {
					File[] backups = new File("Backups").listFiles();

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					List<File> backupsMatch = new ArrayList<>();

					for (int i = 0; i < backups.length; i++) {
						if (backups[i].getName().contains(args[1])) {
							backupsMatch.add(backups[i]);
						}
					}

					if (backupsMatch.size() == 0) {
						sender.sendMessage("No backups for search argument '" + args[1] + "' found.");

						return false;
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
				} else {
					sendHelp(sender);
				}
			} else if (args.length == 3) {
				if (args[0].equalsIgnoreCase("search")) {
					File[] backups = new File("Backups").listFiles();

					if (backups.length == 0) {
						sender.sendMessage("No backups found.");

						return false;
					}

					List<File> backupsMatch = new ArrayList<>();

					for (int i = 0; i < backups.length; i++) {
						if (backups[i].getName().contains(args[1])) {
							backupsMatch.add(backups[i]);
						}
					}

					if (backupsMatch.size() == 0) {
						sender.sendMessage("No backups for search argument '" + args[1] + "' found.");

						return false;
					}

					Collections.sort(backupsMatch);

					try {
						int page = Integer.valueOf(args[2]);

						if (backups.length < page * 10 - 9) {
							sender.sendMessage("Try a lower value.");

							return false;
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
				}
			} else {
				sendHelp(sender);
			}
		} else

		{
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
	}

	public void zip(String sourceDirPath, String zipFilePath, CommandSender sender, boolean sendDebugMsg,
			boolean isSaving) throws IOException {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {

			long sTime = System.nanoTime();

			System.out.println(" ");
			System.out.println("ServerBackup | Start zipping...");
			System.out.println(" ");

			Path p;
			try {
				p = Files.createFile(Paths.get(zipFilePath));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error while zipping files.");
				return;
			}

			try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
					ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
					try {
						if (sendDebugMsg) {
							System.out.println("Zipping '" + path.toString());

							if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
								if (Bukkit.getConsoleSender() != sender) {
									sender.sendMessage("Zipping '" + path.toString());
								}
							}
						}

						zs.putNextEntry(zipEntry);
						Files.copy(path, zs);
						zs.closeEntry();
					} catch (IOException e) {
						e.printStackTrace();
						System.err.println("Error while zipping files.");
						return;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error while zipping files.");
				return;
			}

			long time = (System.nanoTime() - sTime) / 1000000;

			System.out.println(" ");
			System.out.println("ServerBackup | Files zipped. [" + time + "ms]");
			System.out.println(" ");

			if (!isSaving) {
				File file = new File(sourceDirPath);

				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			sender.sendMessage("");
			sender.sendMessage("Backup [" + sourceDirPath + "] zipped.");
			sender.sendMessage("Backup [" + sourceDirPath + "] saved.");
		});
	}

	public void unzip(String zipFile, String outputFolder, CommandSender sender, boolean sendDebugMsg) {
		Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {

			long sTime = System.nanoTime();

			System.out.println(" ");
			System.out.println("ServerBackup | Start unzipping...");
			System.out.println(" ");

			byte[] buffer = new byte[1024];
			try {
				File folder = new File(outputFolder);
				if (!folder.exists()) {
					folder.mkdir();
				}
				ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String fileName = ze.getName();
					File newFile = new File(outputFolder + File.separator + fileName);

					if (sendDebugMsg) {
						System.out.println("Unzipping '" + newFile.getPath());

						if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
							if (Bukkit.getConsoleSender() != sender) {
								sender.sendMessage("Unzipping '" + newFile.getPath());
							}
						}
					}

					new File(newFile.getParent()).mkdirs();
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
					ze = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error while unzipping files.");
				return;
			}

			long time = (System.nanoTime() - sTime) / 1000000;

			System.out.println(" ");
			System.out.println("ServerBackup | Files unzipped. [" + time + "ms]");
			System.out.println(" ");

			File file = new File(zipFile);

			file.delete();

			sender.sendMessage("");
			sender.sendMessage("Backup [" + zipFile + "] unzipped.");
		});
	}

}
