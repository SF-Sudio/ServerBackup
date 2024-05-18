package net.server_backup.utils;

import com.dropbox.core.*;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Level;

public class DropboxManager {

    private CommandSender sender;
    public DropboxManager(CommandSender sender) {
        this.sender = sender;
    }

    private static final String ACCESS_TOKEN = Configuration.cloudInfo.getString("Cloud.Dropbox.AccessToken");

    public void uploadToDropbox(String filePath) {
        File file = new File(filePath);

        if (!file.getPath().contains(Configuration.backupDestination.replaceAll("/", ""))) {
            file = new File(Configuration.backupDestination + "//" + filePath);
            filePath = file.getPath();
        }

        if (!file.exists()) {
            sender.sendMessage("Dropbox: Backup '" + file.getName() + "' not found.");

            return;
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/ServerBackupG").build();

        String appKey = Configuration.cloudInfo.getString("Cloud.Dropbox.AppKey");
        String secretKey = Configuration.cloudInfo.getString("Cloud.Dropbox.AppSecret");

        DbxClientV2 client = null;
        DbxCredential credential = null;

        if(!Configuration.cloudInfo.contains("Cloud.Dropbox.RT")) {
            DbxAppInfo appInfo = new DbxAppInfo(appKey, secretKey);
            DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
            DbxAuthFinish authFinish = null;
            try {
                authFinish = webAuth.finishFromCode(ACCESS_TOKEN);
            } catch (DbxException e) {
                throw new RuntimeException(e);
            }
            credential = new DbxCredential(authFinish.getAccessToken(), 60L, authFinish.getRefreshToken(), appKey, secretKey);

            Configuration.cloudInfo.set("Cloud.Dropbox.RT", authFinish.getRefreshToken());
            Configuration.saveCloud();
        } else {
            credential = new DbxCredential("isly", 0L, Configuration.cloudInfo.getString("Cloud.Dropbox.RT"), appKey, secretKey);
        }

        client = new DbxClientV2(config, credential);

        sender.sendMessage("Dropbox: Uploading backup [" + file.getName() + "] ...");

        String des = ServerBackup.getInstance().getConfig().getString("CloudBackup.Options.Destination").replaceAll("/", "");
        des = "/" + (des.equals("") ? "" : des + "/");

        if(file.length() > (100 * 1000 * 1000)) {
            chunkedUploadFile(sender, client, file, des + file.getName());
        } else {
            uploadFile(sender, client, file, des + file.getName());
        }
    }

    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 16L << 20;
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    static String lastProgress = "";

    private static void chunkedUploadFile(CommandSender sender, DbxClientV2 dbxClient, File file, String dbxPath) {
        long size = file.length();

        long uploaded = 0L;
        DbxException thrown = null;

        IOUtil.ProgressListener progressListener = new IOUtil.ProgressListener() {
            long uploadedBytes = 0;

            @Override
            public void onProgress(long l) {
                getProgress(file.getName(), l + uploadedBytes, size, false);
                if (l == CHUNKED_UPLOAD_CHUNK_SIZE) uploadedBytes += CHUNKED_UPLOAD_CHUNK_SIZE;
            }
        };

        String sessionId = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
            if (i > 0) {
                Bukkit.getLogger().log(Level.INFO, "Dropbox: Chunk upload failed. Retrying (" + Integer.valueOf(i + 1) + "/" + CHUNKED_UPLOAD_MAX_ATTEMPTS + ")...");
            }

            try (InputStream in = new FileInputStream(file)) {
                in.skip(uploaded);

                // Start
                if (sessionId == null) {
                    sessionId = dbxClient.files().uploadSessionStart()
                            .uploadAndFinish(in)
                            .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    getProgress(file.getName(), uploaded, size, false);
                }

                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                // Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    dbxClient.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(in);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    getProgress(file.getName(), uploaded, size, false);
                    cursor = new UploadSessionCursor(sessionId, uploaded);
                }

                // Finish
                long remaining = size - uploaded;
                CommitInfo commitInfo = CommitInfo.newBuilder(dbxPath)
                        .withMode(WriteMode.ADD)
                        .withClientModified(new Date(file.lastModified()))
                        .build();
                dbxClient.files().uploadSessionFinish(cursor, commitInfo).uploadAndFinish(in, remaining, progressListener);

                sender.sendMessage("Dropbox: Upload successfully. Backup stored on your dropbox account.");
                getProgress(file.getName(), uploaded, size, true);

                if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Options.DeleteLocalBackup")) {
                    file.delete();
                    System.out.println("File [" + file.getPath() + "] deleted.");
                }
                return;
            } catch (RetryException e) {
                thrown = e;
            } catch (NetworkIOException e) {
                thrown = e;
            } catch (UploadSessionFinishErrorException e) {
                if (e.errorValue.isLookupFailed() && e.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    thrown = e;
                    uploaded = e.errorValue
                            .getLookupFailedValue()
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                } else {
                    e.printStackTrace();
                    return;
                }
            } catch (DbxException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Bukkit.getLogger().log(Level.WARNING, "Dropbox: Too many upload attempts. Check your server's connection and try again." + "\n" + thrown.getMessage());
    }

    private static void uploadFile(CommandSender sender, DbxClientV2 client, File file, String dbxPath) {
        OperationHandler.tasks.add("DROPBOX UPLOAD {" + file.getName() + "}");

        try (InputStream in = new FileInputStream(file.getPath())) {
            client.files().uploadBuilder(dbxPath + file.getName()).uploadAndFinish(in);
        } catch (UploadErrorException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DbxException e) {
            throw new RuntimeException(e);
        } finally {
            sender.sendMessage("Dropbox: Upload successfully. Backup stored on your dropbox account.");
            OperationHandler.tasks.remove("DROPBOX UPLOAD {" + file.getName() + "}");

            if (ServerBackup.getInstance().getConfig().getBoolean("CloudBackup.Options.DeleteLocalBackup")) {
                file.delete();
                System.out.println("File [" + file.getPath() + "] deleted.");
            }
        }
    }

    private static String getProgress(String fileName, long uploaded, long size, boolean finished) {
        if(lastProgress != "") {
            OperationHandler.tasks.remove(lastProgress);
        }

        if(!finished) {
            String progress = "DROPBOX UPLOAD {" + fileName + ", Progress: " + Math.round((uploaded / (double) size) * 100) + "%}";
            OperationHandler.tasks.add(progress);
            lastProgress = progress;

            return progress;
        }

        return "DROPBOX UPLOAD {" + fileName + ", Progress: finished}";
    }

}
