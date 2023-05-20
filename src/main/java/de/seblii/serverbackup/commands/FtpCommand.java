package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import de.seblii.serverbackup.utils.FtpManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class FtpCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (args[1].equalsIgnoreCase("list")) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                FtpManager ftpm = new FtpManager(sender);

                List<String> backups = ftpm.getFtpBackupList();

                if (backups.size() == 0) {
                    sender.sendMessage(backup.processMessage("Error.NoFtpBackups"));

                    return;
                }

                if (args[2]!=null) {
                    try {
                        int page = Integer.parseInt(args[2]);

                        if (backups.size() < page * 10 - 9) {
                            sender.sendMessage("Try a lower value.");

                            return;
                        }

                        if (backups.size() <= page * 10 && backups.size() >= page * 10 - 10) {
                            sender.sendMessage("----- Ftp-Backup " + (page * 10 - 9) + "-"
                                    + backups.size() + "/" + backups.size() + " -----");
                        } else {
                            sender.sendMessage("----- Ftp-Backup " + (page * 10 - 9) + "-"
                                    + page * 10 + "/" + backups.size() + " -----");
                        }
                        sender.sendMessage("");

                        for (int i = page * 10 - 10; i < backups.size() && i < page * 10; i++) {
                            checkPlayer(sender, backups, i);
                        }

                        int maxPages = backups.size() / 10;

                        if (backups.size() % 10 != 0) {
                            maxPages++;
                        }

                        sender.sendMessage("");
                        sender.sendMessage("--------- Page " + page + "/" + maxPages + " ---------");
                    } catch (Exception e) {
                        sender.sendMessage(backup.processMessage("Error.NotANumber").replaceAll("%input%", args[1]));
                    }
                } else {
                    Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                        if (backups.size() == 0) {
                            sender.sendMessage(backup.processMessage("Error.NoFtpBackups"));
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
                            checkPlayer(sender, backups, i);
                        }

                        int maxPages = backups.size() / 10;

                        if (backups.size() % 10 != 0) {
                            maxPages++;
                        }

                        sender.sendMessage("");
                        sender.sendMessage("--------- Page 1/" + maxPages + " ---------");
                    });
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

                ftpm.uploadFileToFtp(args[2], false);
            });
        } else {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup ftp <list/download/upload>");
        }
        return false;
    }

    private void checkPlayer(CommandSender sender, List<String> backups, int i) {
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
}
