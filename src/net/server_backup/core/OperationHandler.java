package net.server_backup.core;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

public class OperationHandler {

    public static boolean shutdownProgress = false;
    public static boolean isUpdated = false;

    public static List<String> tasks = new ArrayList<>();

    public static String processMessage(String msgCode) {
        return (Configuration.prefix + Configuration.messages.getString(msgCode)).replace("&nl", "\n").replace("&", "§");
    }

    public static void startTimer() {
        if (ServerBackup.getInstance().getConfig().getBoolean("AutomaticBackups")) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(ServerBackup.getInstance(), new Timer(), 20 * 20, 20 * 20);
        }
    }

    public static void stopTimer() {
        Bukkit.getScheduler().cancelTasks(ServerBackup.getInstance());
    }

    public static void checkVersion() {
        ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup: Searching for updates...");

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
                        ServerBackup.getInstance().getLogger().log(Level.INFO,
                                "ServerBackup: No updates found. The server is running the latest version.");
                    } else {
                        ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup: There is a newer version available - " + latest
                                + ", you are on - " + current);

                        if (ServerBackup.getInstance().getConfig().getBoolean("AutomaticUpdates")) {
                            ServerBackup.getInstance().getLogger().log(Level.INFO, "ServerBackup: Downloading newest version...");

                            URL url = new URL("https://server-backup.net/assets/downloads/alt/ServerBackup.jar");

                            if (Bukkit.getVersion().contains("1.18") || Bukkit.getVersion().contains("1.19")) {
                                url = new URL("https://server-backup.net/assets/downloads/ServerBackup.jar");
                            }

                            try (InputStream in = url.openStream();
                                 ReadableByteChannel rbc = Channels.newChannel(in);
                                 FileOutputStream fos = new FileOutputStream("plugins/ServerBackup.jar")) {
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                                ServerBackup.getInstance().getLogger().log(Level.INFO,
                                        "ServerBackup: Download finished. Please reload the server to complete the update.");

                                isUpdated = true;
                            }
                        } else {
                            ServerBackup.getInstance().getLogger().log(Level.INFO,
                                    "ServerBackup: Please download the latest version - https://server-backup.net/");
                        }
                    }
                }
            } catch (IOException exception) {
                ServerBackup.getInstance().getLogger().log(Level.WARNING,
                        "ServerBackup: Cannot search for updates - " + exception.getMessage());
            }
        });
    }
}
