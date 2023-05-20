package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import de.seblii.serverbackup.utils.DropboxManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class DropboxCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if(args[1].equalsIgnoreCase("upload")) {
            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                DropboxManager dm = new DropboxManager(sender);

                dm.uploadToDropbox(args[2]);
            });
        } else {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup dropbox upload <file>");
        }
        return false;
    }
}
