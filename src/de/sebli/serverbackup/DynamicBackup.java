package de.sebli.serverbackup;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class DynamicBackup implements Listener {

	List<Chunk> chunks = new ArrayList<>();

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (ServerBackup.getInstance().getConfig().getBoolean("DynamicBackup")) {
			if (e.getFrom().getChunk() != e.getTo().getChunk()) {
				int regX = e.getTo().getChunk().getX() >> 5;
				int regZ = e.getTo().getChunk().getZ() >> 5;

				Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), new Runnable() {

					@Override
					public void run() {
						String chunkInf = "Data." + Bukkit.getWorldContainer() + "\\" + e.getTo().getWorld().getName()
								+ ".Chunk." + "r." + regX + "." + regZ + ".mca";

						if (!chunks.contains(e.getTo().getChunk())) {
							if (!ServerBackup.getInstance().bpInf.contains(chunkInf)) {
								chunks.add(e.getTo().getChunk());
								ServerBackup.getInstance().bpInf.set(chunkInf + ".X", e.getTo().getChunk().getX());
								ServerBackup.getInstance().bpInf.set(chunkInf + ".Z", e.getTo().getChunk().getZ());

								ServerBackup.getInstance().saveBpInf();
								chunks.remove(e.getTo().getChunk());
							} else {
								chunks.add(e.getTo().getChunk());
								ServerBackup.getInstance().bpInf.set(chunkInf, e.getTo().getChunk().getInhabitedTime());

								ServerBackup.getInstance().saveBpInf();
								chunks.remove(e.getTo().getChunk());
							}
						}
					}

				});
			}
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
					String chunkInf = "Data." + Bukkit.getWorldContainer() + "\\" + p.getLocation().getWorld().getName()
							+ ".Chunk." + "r." + regX + "." + regZ + ".mca";

					if (!chunks.contains(p.getLocation().getChunk())) {
						if (!ServerBackup.getInstance().bpInf.contains(chunkInf)) {
							chunks.add(p.getLocation().getChunk());
							ServerBackup.getInstance().bpInf.set(chunkInf + ".X", p.getLocation().getChunk().getX());
							ServerBackup.getInstance().bpInf.set(chunkInf + ".Z", p.getLocation().getChunk().getZ());

							ServerBackup.getInstance().saveBpInf();
							chunks.remove(p.getLocation().getChunk());
						} else {
							chunks.add(p.getLocation().getChunk());
							ServerBackup.getInstance().bpInf.set(chunkInf,
									p.getLocation().getChunk().getInhabitedTime());

							ServerBackup.getInstance().saveBpInf();
							chunks.remove(p.getLocation().getChunk());
						}
					}
				}

			});
		}
	}

}
