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
import java.util.Arrays;

public class ListCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args.length > 2) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup list [page]");
            return false;
        } else if (args.length == 1) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                File[] backups = getFiles(sender, backup);
                if (backups == null) return;

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

                    calcFileSize(sender, backups, i);
                }

                int maxPages = (backups.length - 1) / 10;

                if ((backups.length - 1) % 10 != 0) {
                    maxPages++;
                }

                sender.sendMessage("");
                sender.sendMessage("-------- Page 1/" + maxPages + " --------");
            });
        } else if (args.length == 2) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                File[] backups = getFiles(sender, backup);
                if (backups == null) return;

                try {
                    int page = Integer.parseInt(args[1]);

                    if ((backups.length - 1) < page * 10 - 9) {
                        sender.sendMessage("Try a lower value.");

                        return;
                    }

                    if ((backups.length - 1) <= page * 10 && (backups.length - 1) >= page * 10 - 10) {
                        sender.sendMessage("----- Backup " + (page * 10 - 9) + "-"
                                + (backups.length - 1) + "/" + (backups.length - 1) + " -----");
                    } else {
                        sender.sendMessage("----- Backup " + (page * 10 - 9) + "-"
                                + page * 10 + "/" + (backups.length - 1) + " -----");
                    }
                    sender.sendMessage("");

                    for (int i = page * 10 - 10; i < (backups.length - 1) && i < page * 10; i++) {
                        if (backups[0].getName().equalsIgnoreCase("Files")) {
                            i--;
                            continue;
                        }

                        calcFileSize(sender, backups, i);
                    }

                    int maxPages = (backups.length - 1) / 10;

                    if ((backups.length - 1) % 10 != 0) {
                        maxPages++;
                    }

                    sender.sendMessage("");
                    sender.sendMessage("-------- Page " + page + "/" + maxPages + " --------");
                } catch (Exception e) {
                    sender.sendMessage(backup.processMessage("Error.NotANumber").replaceAll("%input%", args[1]));
                }
            });
        }
        return false;
    }

    private static File[] getFiles(CommandSender sender, ServerBackup backup) {
        File[] backups = new File(ServerBackup.getInstance().backupDestination).listFiles();
        assert backups != null;

        if (backups.length == 0
                || backups.length == 1 && backups[0].getName().equalsIgnoreCase("Files")) {
            sender.sendMessage(backup.processMessage("Error.NoBackups"));

            return null;
        }

        Arrays.sort(backups);
        return backups;
    }

    private void calcFileSize(CommandSender sender, File[] backups, int i) {
        double fileSize = (double) FileUtils.sizeOf(backups[i]) / 1000 / 1000;
        fileSize = Math.round(fileSize * 100.0) / 100.0;

        if (sender instanceof Player) {
            Player p = (Player) sender;

            TextComponent msg = new TextComponent("§7[" + (i + 1) + "] §r"
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
}
