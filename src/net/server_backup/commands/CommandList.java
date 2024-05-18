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
import java.util.Arrays;

public class CommandList {

    public static void execute(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
            File[] backups = new File(Configuration.backupDestination + "").listFiles();

            if (backups.length == 0
                    || backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
                sender.sendMessage(OperationHandler.processMessage("Error.NoBackups"));

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
                sender.sendMessage(OperationHandler.processMessage("Error.NotANumber").replaceAll("%input%", args[1]));
            }
        });
    }
}