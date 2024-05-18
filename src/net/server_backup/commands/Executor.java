package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.core.OperationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Executor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender.hasPermission("backup.admin")) {
            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("create")) {
                    CommandCreate.execute(sender, args);

                    return true;
                }
            }

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("shutdown")) {
                    CommandShutdown.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("list")) {
                    Bukkit.dispatchCommand(sender, "backup list 1");
                } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
                    Configuration.reloadConfig(sender);
                } else if (args[0].equalsIgnoreCase("tasks") || args[0].equalsIgnoreCase("task")) {
                    CommandTasks.execute(sender, args);
                } else {
                    sendHelp(sender);
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("list")) {
                    CommandList.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("ftp")) {
                    if (args[1].equalsIgnoreCase("list")) {
                        Bukkit.dispatchCommand(sender, "backup ftp list 1");
                    }
                } else if (args[0].equalsIgnoreCase("zip")) {
                    CommandZip.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("unzip")) {
                    CommandUnzip.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
                    CommandRemove.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("search")) {
                    Bukkit.dispatchCommand(sender, "backup search " + args[1] + " 1");
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("search")) {
                    CommandSearch.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("ftp")) {
                    CommandFtp.execute(sender, args);
                } else if (args[0].equalsIgnoreCase("dropbox")) {
                    CommandDropbox.execute(sender, args);
                }
            } else {
                sendHelp(sender);
            }
        } else {
            sender.sendMessage(OperationHandler.processMessage("Error.NoPermission"));
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
        sender.sendMessage("/backup ftp <download/upload/list> - download, upload or list ftp backup files");
        sender.sendMessage("");
        sender.sendMessage("/backup dropbox upload <file> - upload a backup to dropbox");
        sender.sendMessage("");
        sender.sendMessage("/backup shutdown - shut downs the server after backup tasks are finished");
    }

}
