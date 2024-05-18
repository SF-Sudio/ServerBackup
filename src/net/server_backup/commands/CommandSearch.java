package net.server_backup.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandSearch {

    public static void execute(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
            File[] backups = new File(Configuration.backupDestination + "").listFiles();

            if (backups.length == 0
                    || backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
                sender.sendMessage(OperationHandler.processMessage("Error.NoBackups"));

                return;
            }

            List<File> backupsMatch = new ArrayList<>();

            for (int i = 0; i < backups.length; i++) {
                if (backups[i].getName().contains(args[1])) {
                    backupsMatch.add(backups[i]);
                }
            }

            if (backupsMatch.size() == 0) {
                sender.sendMessage(OperationHandler.processMessage("NoBackupSearch").replaceAll("%input%", args[1]));

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

                            TextComponent msg = new TextComponent("§7[" + Integer.valueOf(count)
                                    + "] §r" + file.getName() + " §7[" + fileSize + "MB]");
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
                sender.sendMessage(OperationHandler.processMessage("Error.NotANumber").replaceAll("%input%", args[2]));
            }
        });
    }

}
