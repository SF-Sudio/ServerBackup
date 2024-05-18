package net.server_backup.utils;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.apache.commons.io.FileUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.concurrent.Callable;

public class BStats {

    public static void initialize() {
        Metrics metrics = new Metrics(ServerBackup.getInstance(), 14673);

        metrics.addCustomChart(new SimplePie("player_per_server", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            }
        }));

        metrics.addCustomChart(new SimplePie("using_ftp_server", new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.UploadBackup")) {
                    return "yes";
                } else {
                    return "no";
                }
            }
        }));

        metrics.addCustomChart(new SimplePie("using_dropbox", new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Dropbox")) {
                    return "yes";
                } else {
                    return "no";
                }
            }
        }));

        metrics.addCustomChart(new SimplePie("using_gdrive", new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.GoogleDrive")) {
                    return "yes";
                } else {
                    return "no";
                }
            }
        }));

        metrics.addCustomChart(new SingleLineChart("total_backup_space", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                File file = new File(Configuration.backupDestination);

                double fileSize = (double) FileUtils.sizeOf(file) / 1000 / 1000;
                fileSize = Math.round(fileSize * 100.0) / 100.0;

                return (int) fileSize;
            }
        }));
    }

}
