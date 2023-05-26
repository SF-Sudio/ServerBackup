package de.seblii.serverbackup.commands;

import de.seblii.serverbackup.BackupManager;
import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

public class TasksCommand implements ServerCommand{
    @Override
    public boolean execute(CommandSender sender, String[] args, ServerBackup backup) {
        if (BackupManager.tasks.size() > 0) {
            sender.sendMessage(backup.processMessage("Command.Tasks.Header"));

            for (String task : BackupManager.tasks) {
                sender.sendMessage(task);
            }

            sender.sendMessage(backup.processMessage("Command.Tasks.Footer"));
        } else {
            sender.sendMessage(backup.processMessage("Error.NoTasks"));
        }
        return false;
    }
}
