package com.baker.abaker.workers;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.baker.abaker.GindActivity;
import com.baker.abaker.client.GindMandator;
import com.baker.abaker.settings.Configuration;

import java.io.File;

public class DownloaderTask extends AsyncTask<String, Long, String> {

    //The mandator that should be notified after the task is done
    private GindMandator mandator;
    //The task id
    private int taskId;

    private DownloadManager dm;
    private String downloadUrl;
    private String fileName;
    private String fileTitle;
    private String fileDescription;
    private String downloadPath;
    private int visibility;
    Uri downloadedFile;
    private long downloadId = -1L;
    private boolean overwrite = true;
    private Context context;

    public DownloaderTask(Context context,
                          GindMandator mandator,
                          final int taskId,
                          final String downloadUrl,
                          final String fileName,
                          final String fileTitle,
                          final String fileDesc,
                          final String downloadDirectoryPath,
                          final int visibility) {
        this.context = context;
        this.mandator = mandator;
        this.taskId = taskId;
        this.dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.fileTitle = fileTitle;
        this.fileDescription = fileDesc;
        this.downloadPath = downloadDirectoryPath;
        this.visibility = visibility;
        this.downloadId = this.restoreDownloadId();
    }

    public long getDownloadId() {
        return this.downloadId;
    }

    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isOverwrite() {
        return this.overwrite;
    }

    public boolean isDownloading() {
        boolean result = false;
        if (null != this.dm) {
            Query query = new Query();
            query.setFilterById(downloadId);
            Cursor c = this.dm.query(query);
            try {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    result = (status == DownloadManager.STATUS_RUNNING);
                }
            } catch (NullPointerException ex) {
                // Do nothing
            }
        }

        return result;
    }

    public void cancelDownload() {
        if (null != this.dm) {
            this.dm.remove(downloadId);
            this.cancel(true);
            this.removeDownloadId();
        }
    }

    private boolean fileExists(final String filepath) {
        File file = new File(filepath);
        return file.exists();
    }

    private boolean deleteFile(final String filepath) {
        File file = new File(filepath);
        return file.delete();
    }

    public SharedPreferences getDownloadPreferences() {
        return this.context.getSharedPreferences(GindActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private void storeDownloadId() {
        SharedPreferences preferences = context.getSharedPreferences(GindActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Configuration.DOWNLOAD_IN_PROGRESS + this.fileName, this.downloadId);
        editor.commit();
    }

    private long restoreDownloadId() {
        SharedPreferences preferences = context.getSharedPreferences(GindActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        long id = preferences.getLong(Configuration.DOWNLOAD_IN_PROGRESS + this.fileName, -1L);

        return id;
    }

    private void removeDownloadId() {
        this.downloadId = -1L;
        SharedPreferences preferences = context.getSharedPreferences(GindActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Configuration.DOWNLOAD_IN_PROGRESS + this.fileName);
        editor.commit();
    }

    @Override
    protected String doInBackground(String... params) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setDescription(fileDescription);
        request.setTitle(fileTitle);

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(visibility);
        }

        if (this.isOverwrite() && !this.isDownloading()) {

            String filepath = downloadPath + File.separator + fileName;
            Log.d(this.getClass().toString(), "DownloaderTask will overwrite the file " + filepath);

            boolean result = this.fileExists(filepath);

            if (result) {
                this.deleteFile(filepath);
            }
        }

        String result = "";
        try {
            Log.d(this.getClass().toString(), "USING RELATIVE PATH FOR DOWNLOAD: " + downloadPath);

            File downloadDirectory = new File(this.downloadPath);
            // If the download directory (or parent directories) does not exist, we create it.
            if (!downloadDirectory.exists()) {
                downloadDirectory.mkdirs();
            }

            request.setDestinationUri(Uri.parse("file://".concat(downloadPath.concat(File.separator).concat(fileName))));

            if (downloadId == -1L) {
                downloadId = dm.enqueue(request);

                this.storeDownloadId();
            }

            Query query = new Query();
            query.setFilterById(downloadId);

            boolean downloading = true;
            while (downloading) {
                Cursor c = this.dm.query(query);
                c.moveToFirst();
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                        if (!Configuration.hasNetworkConnection(this.context)) {
                            result = "ERROR";
                            dm.remove(downloadId);
                            downloading = false;
                        }
                        break;
                    case DownloadManager.STATUS_PENDING:
                        //Do nothing
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        long totalBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long bytesSoFar = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long progress = (bytesSoFar * 100 / totalBytes);
                        Log.d(this.getClass().getName(), "RUNNING Download of " + this.fileName + " progress: " + progress + "%");
                        publishProgress(progress, bytesSoFar, totalBytes);
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        publishProgress(100L,
                                c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                                c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
                        downloading = false;
                        Log.d(this.getClass().getName(), "SUCCESSFULLY Downloaded " + this.fileName);
                        result = "SUCCESS";
                        downloadedFile = dm.getUriForDownloadedFile(downloadId);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        result = "ERROR";
                        Log.e(this.getClass().getName(), "ERROR Downloading " + this.fileName);
                        dm.remove(downloadId);
                        downloading = false;
                        break;
                }
                c.close();
            }
        } catch (IllegalStateException ex) {
            result = "DIRECTORY_NOT_FOUND";
            ex.printStackTrace();
        } catch (android.database.CursorIndexOutOfBoundsException ex) {
            // This exception might be thrown when the user presses the back
            // button and cancels the current downloads, so the index
            // does not exist anymore.
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        mandator.updateProgress(taskId, progress[0], progress[1], progress[2]);
    }

    @Override
    protected void onPostExecute(String result) {
        this.removeDownloadId();
        String path = (null == downloadedFile) ? "" : downloadedFile.getPath();
        mandator.postExecute(taskId, result, path);
    }

}
