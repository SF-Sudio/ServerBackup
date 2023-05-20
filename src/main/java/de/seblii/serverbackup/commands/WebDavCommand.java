package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import de.seblii.serverbackup.utils.WebDavManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class WebDavCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (!ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.WebDav")){
            sender.sendMessage("WebDav is disabled in the config.yml");
            return false;
        }

        if (args.length != 3) {
            sender.sendMessage("Invalid arguments provided. Correct usage: /backup dropbox upload <file>");
            return false;
        }

        try {
            if (args[1].equalsIgnoreCase("upload")){
                Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                    WebDavManager webDav = new WebDavManager(sender);

                    try {
                        webDav.uploadToWebDav(args[2]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
