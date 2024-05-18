package net.server_backup.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import net.server_backup.utils.FtpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandFtp {

    public static void execute(CommandSender sender, String[] args) {
        if (args[1].equalsIgnoreCase("list")) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                FtpManager ftpm = new FtpManager(sender);

                List<String> backups = ftpm.getFtpBackupList(false);

                if (backups.size() == 0) {
                    sender.sendMessage(OperationHandler.processMessage("Error.NoFtpBackups"));

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
                    sender.sendMessage(OperationHandler.processMessage("Error.NotANumber").replaceAll("%input%", args[1]));
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

                ftpm.uploadFileToFtp(args[2], !ServerBackup.getInstance().getConfig().getBoolean("Ftp.CompressBeforeUpload"));
            });
        }
    }

}
