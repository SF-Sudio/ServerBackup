package net.server_backup.core;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class DynamicBackup  implements Listener {

    List<Chunk> chunks = Collections.synchronizedList(new ArrayList<>());
    public boolean isSaving = false;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
            if (e.getFrom().getChunk() != e.getTo().getChunk()) {
                int regX = e.getTo().getChunk().getX() >> 5;
                int regZ = e.getTo().getChunk().getZ() >> 5;

                Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), new Runnable() {

                    @Override
                    public void run() {
                        String chunkInf = "Data." + e.getTo().getWorld().getName() + ".Chunk." + "r." + regX + "."
                                + regZ + ".mca";

                        if (!Bukkit.getWorldContainer().toString().equalsIgnoreCase(".")) {
                            chunkInf = "Data." + Bukkit.getWorldContainer() + "\\" + e.getTo().getWorld().getName()
                                    + ".Chunk." + "r." + regX + "." + regZ + ".mca";
                        }

                        if (!chunks.contains(e.getTo().getChunk())) {
                            if (!Configuration.backupInfo.contains(chunkInf)) {
                                chunks.add(e.getTo().getChunk());
                                Configuration.backupInfo.set(chunkInf, chunks.get(chunks.size() - 1).getX());
                                Configuration.backupInfo.set(chunkInf, chunks.get(chunks.size() - 1).getZ());

                                saveChanges();
                                try {
                                    chunks.remove(e.getTo().getChunk());
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                }
                            } else {
                                chunks.add(e.getTo().getChunk());
                                Configuration.backupInfo.set(chunkInf, chunks.get(chunks.size() - 1).getWorld());

                                saveChanges();
                                try {
                                    chunks.remove(e.getTo().getChunk());
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                }
                            }
                        }
                    }

                });
            }
        }
    }

    public void saveChanges() {
        if (!isSaving) {
            isSaving = true;

            Bukkit.getScheduler().runTaskLaterAsynchronously(ServerBackup.getInstance(), new Runnable() {

                @Override
                public void run() {
                    Configuration.saveBackupInfo();;
                    if (ServerBackup.getInstance().getConfig().getBoolean("SendLogMessages")) {
                        Bukkit.getLogger().log(Level.INFO, "DynamicBP: file saved.");
                    }

                    isSaving = false;
                }

            }, 20 * 5);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
            Player p = e.getPlayer();

            int regX = p.getLocation().getChunk().getX() >> 5;
            int regZ = p.getLocation().getChunk().getZ() >> 5;

            Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), new Runnable() {

                @Override
                public void run() {
                    String chunkInf = "Data." + p.getLocation().getWorld().getName() + ".Chunk." + "r." + regX + "."
                            + regZ + ".mca";

                    if (!Bukkit.getWorldContainer().toString().equalsIgnoreCase(".")) {
                        chunkInf = "Data." + Bukkit.getWorldContainer() + "\\" + p.getLocation().getWorld().getName()
                                + ".Chunk." + "r." + regX + "." + regZ + ".mca";
                    }

                    if (!chunks.contains(p.getLocation().getChunk())) {
                        if (!Configuration.backupInfo.contains(chunkInf)) {
                            chunks.add(p.getLocation().getChunk());
                            Configuration.backupInfo.set(chunkInf + ".X", p.getLocation().getChunk().getX());
                            Configuration.backupInfo.set(chunkInf + ".Z", p.getLocation().getChunk().getZ());

                            Configuration.saveBackupInfo();
                            chunks.remove(p.getLocation().getChunk());
                        } else {
                            chunks.add(p.getLocation().getChunk());
                            Configuration.backupInfo.set(chunkInf, p.getLocation().getChunk().getWorld());

                            Configuration.saveBackupInfo();
                            chunks.remove(p.getLocation().getChunk());
                        }
                    }
                }

            });
        }
    }
}