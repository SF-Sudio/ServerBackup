package de.seblii.serverbackup;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;
import java.util.logging.Level;

public class DynamicBackup implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup") && (e.getFrom().getChunk() != Objects.requireNonNull(e.getTo()).getChunk())) {
				// region X & Y (mca file)
				Chunk chunk = e.getTo().getChunk();
				int regX = chunk.getX() >> 5;
				int regZ = chunk.getZ() >> 5;


				Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
                    String chunkInf = getChunkInfoPath(Objects.requireNonNull(e.getTo().getWorld()).getName(), regX, regZ);
					addChunkToBackupInfo(chunk, chunkInf, e.getPlayer().getName(), chunk.getWorld().getName());
				});

		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
			Player p = e.getPlayer();
			Chunk chunk = p.getLocation().getChunk();
			int regX = chunk.getX() >> 5;
			int regZ = chunk.getZ() >> 5;

			Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
				String chunkInf = getChunkInfoPath(Objects.requireNonNull(p.getLocation().getWorld()).getName(), regX, regZ);

				addChunkToBackupInfo(chunk, chunkInf, p.getName(), chunk.getWorld().getName());
			});
		}
	}

	private static String getChunkInfoPath(String worldName, int regX, int regZ) {
		String chunkInf = "Data." + worldName + ".Chunk." + "r." + regX + "."
				+ regZ + ".mca";

		if (!Bukkit.getWorldContainer().toString().equalsIgnoreCase(".")) {
			chunkInf = "Data." + Bukkit.getWorldContainer() + "\\" + worldName
					+ ".Chunk." + "r." + regX + "." + regZ + ".mca";
		}
		return chunkInf;
	}

	private static void addChunkToBackupInfo(Chunk chunk, String chunkInf, String playerName, String worldName) {
		if (!ServerBackup.getInstance().bpInf.contains(chunkInf)) {
			ServerBackup.getInstance().bpInf.set(chunkInf + ".chunkX", chunk.getX());
			ServerBackup.getInstance().bpInf.set(chunkInf + ".chunkZ", chunk.getZ());
			ServerBackup.getInstance().bpInf.set(chunkInf + ".triggeredBy", playerName);
			ServerBackup.getInstance().saveBackupInfoChanges();
			Bukkit.getLogger().log(Level.INFO, "Region {0}, {1} added to backup set for {2}.",
					new Object[]{chunk.getX() >> 5, chunk.getZ() >> 5, worldName});
		}
	}
}