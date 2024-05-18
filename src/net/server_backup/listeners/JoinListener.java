package net.server_backup.listeners;

import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (p.hasPermission("backup.update")) {
            if (ServerBackup.getInstance().getConfig().getBoolean("UpdateAvailableMessage")) {
                Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                    int resourceID = 79320;
                    try (InputStream inputStream = (new URL(
                            "https://api.spigotmc.org/legacy/update.php?resource=" + resourceID)).openStream();
                         Scanner scanner = new Scanner(inputStream)) {
                        if (scanner.hasNext()) {
                            String latest = scanner.next();
                            String current = ServerBackup.getInstance().getDescription().getVersion();

                            int late = Integer.parseInt(latest.replaceAll("\\.", ""));
                            int curr = Integer.parseInt(current.replaceAll("\\.", ""));

                            if (curr >= late) {
                            } else {
                                if (OperationHandler.isUpdated) {
                                    p.sendMessage("§8=====§fServerBackup§8=====");
                                    p.sendMessage("");
                                    p.sendMessage("§7There was a newer version available - §a" + latest
                                            + "§7, you are on - §c" + current);
                                    p.sendMessage(
                                            "\n§7The latest version has been downloaded automatically, please reload the server to complete the update.");
                                    p.sendMessage("");
                                    p.sendMessage("§8=====§9Plugin by Seblii§8=====");
                                } else {
                                    if (ServerBackup.getInstance().getConfig().getBoolean("AutomaticUpdates")) {
                                        if (p.hasPermission("backup.admin")) {
                                            p.sendMessage("§8=====§fServerBackup§8=====");
                                            p.sendMessage("");
                                            p.sendMessage("§7There is a newer version available - §a" + latest
                                                    + "§7, you are on - §c" + current);
                                            p.sendMessage("");
                                            p.sendMessage("§8=====§9Plugin by Seblii§8=====");
                                            p.sendMessage("");
                                            p.sendMessage("ServerBackup§7: Automatic update started...");

                                            URL url = new URL(
                                                    "https://server-backup.net/assets/downloads/ServerBackup.jar");

                                            int bVer = Integer.parseInt(
                                                    Bukkit.getVersion().split(" ")[Bukkit.getVersion().split(" ").length
                                                            - 1].replaceAll("\\)", "").replaceAll("\\.", ""));

                                            if (bVer < 118) {
                                                url = new URL(
                                                        "https://server-backup.net/assets/downloads/alt/ServerBackup.jar");
                                            }

                                            try (InputStream in = url.openStream();
                                                 ReadableByteChannel rbc = Channels.newChannel(in);
                                                 FileOutputStream fos = new FileOutputStream(
                                                         "plugins/ServerBackup.jar")) {
                                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                                                p.sendMessage(
                                                        "ServerBackup§7: Download finished. Please reload the server to complete the update.");

                                                OperationHandler.isUpdated = true;
                                            }
                                        }
                                    } else {
                                        p.sendMessage("§8=====§fServerBackup§8=====");
                                        p.sendMessage("");
                                        p.sendMessage("§7There is a newer version available - §a" + latest
                                                + "§7, you are on - §c" + current);
                                        p.sendMessage(
                                                "§7Please download the latest version - §4https://server-backup.net/");
                                        p.sendMessage("");
                                        p.sendMessage("§8=====§9Plugin by Seblii§8=====");
                                    }
                                }
                            }
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
            }
        }
    }

}
