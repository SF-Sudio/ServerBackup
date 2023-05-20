package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args.length < 2 || args.length > 4) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup search <world> [page]");
            return false;
        } else if (args.length == 2) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                File[] backups = new File(ServerBackup.getInstance().backupDestination).listFiles();
                assert backups != null;

                List<File> backupsMatch = matchBackups(sender, args, backup, backups);
                if (backupsMatch == null) return;

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
                        calcFileSize(sender, count, file);
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
        } else if (args.length == 3) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                File[] backups = new File(ServerBackup.getInstance().backupDestination).listFiles();
                assert backups != null;

                List<File> backupsMatch = matchBackups(sender, args, backup, backups);
                if (backupsMatch == null) return;

                try {
                    int page = Integer.parseInt(args[2]);

                    if (backups.length < page * 10 - 9) {
                        sender.sendMessage("Try a lower value.");

                        return;
                    }

                    int count = page * 10 - 9;

                    if (backupsMatch.size() <= page * 10 && backupsMatch.size() >= page * 10 - 10) {
                        sender.sendMessage("----- Backup " + (page * 10 - 9) + "-"
                                + backupsMatch.size() + "/" + backupsMatch.size() + " -----");
                    } else {
                        sender.sendMessage("----- Backup " + (page * 10 - 9) + "-"
                                + page * 10 + "/" + backupsMatch.size() + " -----");
                    }
                    sender.sendMessage("");

                    for (File file : backupsMatch) {
                        if (count <= page * 10 && count <= backupsMatch.size()) {
                            SearchCommand.calcFileSize(sender, count, file);
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
                    sender.sendMessage(backup.processMessage("Error.NotANumber").replaceAll("%input%", args[2]));
                }
            });
        }
        return false;
    }

    private static List<File> matchBackups(CommandSender sender, String[] args, ServerBackup backup, File[] backups) {
        if (backups.length == 0
                || backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
            sender.sendMessage(backup.processMessage("Error.NoBackups"));

            return null;
        }

        List<File> backupsMatch = new ArrayList<>();

        for (File file : backups) {
            if (file.getName().contains(args[1])) {
                backupsMatch.add(file);
            }
        }

        if (backupsMatch.size() == 0) {
            sender.sendMessage(backup.processMessage("NoBackupSearch").replaceAll("%input%", args[1]));

            return null;
        }

        Collections.sort(backupsMatch);
        return backupsMatch;
    }

    static void calcFileSize(CommandSender sender, int count, File file) {
        double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
        fileSize = Math.round(fileSize * 100.0) / 100.0;

        if (sender instanceof Player) {
            Player p = (Player) sender;

            TextComponent msg = new TextComponent("§7[" + count + "] §r"
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
}
