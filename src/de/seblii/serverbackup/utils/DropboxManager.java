package de.seblii.serverbackup.utils;

import com.dropbox.core.*;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import de.seblii.serverbackup.ServerBackup;
import org.apache.commons.net.ftp.FTPFile;
import org.bukkit.command.CommandSender;

import java.io.*;

public class DropboxManager {

    private CommandSender sender;
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

        DbxClientV2 client = null;
        DbxCredential credential = null;

        if(!ServerBackup.getInstance().cloud.contains("Cloud.Dropbox.RT")) {
            DbxAppInfo appInfo = new DbxAppInfo(appKey, secretKey);
            DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
            DbxAuthFinish authFinish = null;
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

        String des = ServerBackup.getInstance().getConfig().getString("CloudBackup.Options.Destination").replaceAll("/", "");
        try (InputStream in = new FileInputStream(filePath)) {
            // Step 1: Upload session start
            UploadSessionStartUploader uploadSessionStart = client.files().uploadSessionStart();
            UploadSessionStartResult uploadSessionStartResult = uploadSessionStart.uploadAndFinish(in);
            String sessionId = uploadSessionStartResult.getSessionId();
            
            // Step 2: Upload large file in chunks
            long fileSize = file.length();
            byte[] buffer = new byte[4 * 1000 * 1000]; // 4 MB chunk size
            long uploaded = 0;
            
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
                
                // Append data to the session
                client.files().uploadSessionAppendV2(cursor).uploadAndFinish(byteArrayInputStream, bytesRead);
                uploaded += bytesRead;
            }
            
            // Step 3: Upload session finish
            UploadSessionCursor cursor = new UploadSessionCursor(sessionId, fileSize);
            CommitInfo commitInfo = CommitInfo.newBuilder("/" + (des.equals("") ? "" : des + "/") + file.getName())
                    .withClientModified(new Date())
                    .withMode(WriteMode.ADD)
                    .build();
            FileMetadata metadata = client.files().uploadSessionFinish(cursor, commitInfo).finish();
        } catch (UploadErrorException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DbxException e) {
            throw new RuntimeException(e);
        } finally {
            sender.sendMessage("Dropbox: Upload successfully. Backup stored on your dropbox account.");

            if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Options.DeleteLocalBackup")) {
                file.delete();
                System.out.println("File [" + file.getPath() + "] deleted.");
            }
        }
    }
}