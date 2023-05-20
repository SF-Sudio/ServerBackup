package de.seblii.serverbackup.utils;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class WebDavManager {

    private final CommandSender sender;
    private final String URL;
    private final String USERNAME;
    private final String PASSWORD;

    public WebDavManager(CommandSender sender) {
        this.sender = sender;
        URL = ServerBackup.getInstance().cloud.getString("Cloud.WebDav.Url");
        USERNAME = ServerBackup.getInstance().cloud.getString("Cloud.WebDav.Username");
        PASSWORD = ServerBackup.getInstance().cloud.getString("Cloud.WebDav.Password");
    }

    public void uploadToWebDav(String filePath) throws IOException {

        File file = new File(filePath);

        if (!file.getPath().contains(ServerBackup.getInstance().backupDestination.replaceAll("/", ""))) {
            file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
            filePath = file.getPath();
        }

        if (!file.exists()) {
            sender.sendMessage("WebDav: Backup '" + file.getName() + "' not found.");

            return;
        }

        Sardine sardine = SardineFactory.begin(USERNAME, PASSWORD);
        sardine.enablePreemptiveAuthentication(URL);

        String des = Objects.requireNonNull(ServerBackup.getInstance().getConfig().getString("CloudBackup.Options.Destination")).replaceAll("/", "");

        String folderWebDav = URL + (des.equals("") || des.equals("/") ? "" : des + "/");

        if (!des.equals("") && !des.equals("/") && !des.equals(".")) {

            List<DavResource> resources = sardine.list(URL);
            boolean containsFolder = resources.stream()
                    .anyMatch(resource -> resource.getName().equals(des));

            if (!containsFolder) {
                sardine.createDirectory(folderWebDav);
            }
        }

        boolean success = false;
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            sardine.put(folderWebDav + file.getName(), in);
            success = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (success){
            sender.sendMessage("WebDav: Upload successfully. Backup stored in your WebDav storage.");

            if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Options.DeleteLocalBackup")) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("File [" + file.getPath() + "] deleted.");
            }
        }
    }
}
