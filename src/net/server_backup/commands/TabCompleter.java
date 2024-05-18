package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.utils.FtpManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

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
                commands.add("tasks");
                commands.add("shutdown");

                StringUtil.copyPartialMatches(args[0], commands, completions);
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("list")) {
                    File[] backups = new File(Configuration.backupDestination + "").listFiles();

                    int maxPages = backups.length / 10;

                    if (backups.length % 10 != 0) {
                        maxPages++;
                    }

                    for (int i = 1; i < maxPages + 1; i++) {
                        commands.add(String.valueOf(i));
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    File[] backups = new File(Configuration.backupDestination + "").listFiles();

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
                    File[] backups = new File(Configuration.backupDestination + "").listFiles();

                    for (File backup : backups) {
                        if (!backup.getName().endsWith(".zip")) {
                            commands.add(backup.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("unzip")) {
                    File[] backups = new File(Configuration.backupDestination + "").listFiles();

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
                }

                StringUtil.copyPartialMatches(args[1], commands, completions);
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("ftp")) {
                    if (args[1].equalsIgnoreCase("download")) {
                        FtpManager ftpm = new FtpManager(sender);

                        List<String> backups = ftpm.getFtpBackupList(false);

                        for (String backup : backups) {
                            commands.add(backup.split(" ")[1]);
                        }
                    } else if (args[1].equalsIgnoreCase("upload")) {
                        File[] backups = new File(Configuration.backupDestination + "").listFiles();

                        for (File backup : backups) {
                            if (backup.getName().endsWith(".zip")) {
                                commands.add(backup.getName());
                            }
                        }
                    }

                } else if (args[0].equalsIgnoreCase("dropbox")) {
                    File[] backups = new File(Configuration.backupDestination + "").listFiles();

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

}
