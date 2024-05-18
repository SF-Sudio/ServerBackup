package net.server_backup;

import net.server_backup.commands.Executor;
import net.server_backup.commands.TabCompleter;
import net.server_backup.core.DynamicBackup;
import net.server_backup.core.OperationHandler;
import net.server_backup.listeners.JoinListener;
import net.server_backup.utils.BStats;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class ServerBackup extends JavaPlugin {

    private static ServerBackup serverBackup;

    public static ServerBackup getInstance() {
        return serverBackup;
    }

    @Override
    public void onDisable() {
        OperationHandler.stopTimer();

        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            task.cancel();

            this.getLogger().log(Level.WARNING, "WARNING - ServerBackup: Task [" + task.getTaskId()
                    + "] cancelled due to server shutdown. There might be some unfinished Backups.");
        }

        Bukkit.getLogger().log(Level.INFO, "ServerBackup: Plugin disabled.");
    }

    @Override
    public void onEnable() {
        serverBackup = this;

        Configuration.loadUp();

        OperationHandler.startTimer();

        getCommand("backup").setExecutor(new Executor());
        getCommand("backup").setTabCompleter(new TabCompleter());

        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new DynamicBackup(), this);

        Bukkit.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");

        if (getConfig().getBoolean("UpdateAvailableMessage")) {
            OperationHandler.checkVersion();
        }

        BStats.initialize();
    }

}
