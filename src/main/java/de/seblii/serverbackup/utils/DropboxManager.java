package de.seblii.serverbackup.utils;

import com.dropbox.core.*;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import de.seblii.serverbackup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class DropboxManager {

    private final CommandSender sender;
    public DropboxManager(CommandSender sender) {
        this.sender = sender;
    }

    private static final String ACCESS_TOKEN = ServerBackup.getInstance().cloud.getString("Cloud.Dropbox.AccessToken");

    public void uploadToDropbox(String filePath) {
        File file = new File(filePath);

        if (!file.getPath().contains(ServerBackup.getInstance().backupDestination.replaceAll("/", ""))) {
            file = new File(ServerBackup.getInstance().backupDestination + "//" + filePath);
            filePath = file.getPath();
        }

        if (!file.exists()) {
            sender.sendMessage("Dropbox: Backup '" + file.getName() + "' not found.");

            return;
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/ServerBackupG").build();

        String appKey = ServerBackup.getInstance().cloud.getString("Cloud.Dropbox.AppKey");
        String secretKey = ServerBackup.getInstance().cloud.getString("Cloud.Dropbox.AppSecret");

        DbxClientV2 client;
        DbxCredential credential;

        if(!ServerBackup.getInstance().cloud.contains("Cloud.Dropbox.RT")) {
            DbxAppInfo appInfo = new DbxAppInfo(appKey, secretKey);
            DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
            DbxAuthFinish authFinish ;
            try {
                authFinish = webAuth.finishFromCode(ACCESS_TOKEN);
            } catch (DbxException e) {
                throw new RuntimeException(e);
            }
            credential = new DbxCredential(authFinish.getAccessToken(), 60L, authFinish.getRefreshToken(), appKey, secretKey);

            ServerBackup.getInstance().cloud.set("Cloud.Dropbox.RT", authFinish.getRefreshToken());
            ServerBackup.getInstance().saveCloud();
        } else {
             credential = new DbxCredential("isly", 0L, ServerBackup.getInstance().cloud.getString("Cloud.Dropbox.RT"), appKey, secretKey);
        }

        client = new DbxClientV2(config, credential);

        sender.sendMessage("Dropbox: Uploading backup [" + file.getName() + "] ...");

        String des = Objects.requireNonNull(ServerBackup.getInstance().cloud.getString("Cloud.Dropbox.Destination")).replaceAll("/", "");

        boolean success = false;
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            FileMetadata metadata = client.files().uploadBuilder("/" + (des.equals("") ? "" : des + "/") + file.getName()).uploadAndFinish(in);
            success = true;
        } catch (IOException | DbxException e) {
            throw new RuntimeException(e);
        }
        if (success){
            sender.sendMessage("Dropbox: Upload successfully. Backup stored on your dropbox account.");

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
