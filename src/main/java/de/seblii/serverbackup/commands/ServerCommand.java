package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

public interface ServerCommand {
    boolean execute(CommandSender sender, String[] args, ServerBackup backup);
}
