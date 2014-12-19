package com.baker.abaker.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.baker.abaker.client.GindMandator;
import com.baker.abaker.settings.Configuration;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UnzipperTask extends AsyncTask<String, Long, String> {

    private Context context;

    private GindMandator mandator;

    private int taskId;

    private boolean resumed;

    private boolean absoluteOutputDirectory = false;

    private boolean deleteZipFile = true;

    public void setResumed(final boolean _resumed) {
        this.resumed = _resumed;
    }

    public boolean isAbsoluteOutputDirectory() {
        return absoluteOutputDirectory;
    }

    public boolean isDeleteZipFile() {
        return deleteZipFile;
    }

    public void setDeleteZipFile(boolean deleteZipFile) {
        this.deleteZipFile = deleteZipFile;
    }

    public void setAbsoluteOutputDirectory(boolean absoluteOutputDirectory) {
        this.absoluteOutputDirectory = absoluteOutputDirectory;
    }

    public UnzipperTask(Context context, GindMandator mandator, final int taskId) {
        this.context = context;
        this.mandator = mandator;
        this.taskId = taskId;
    }

    @Override
    protected String doInBackground(String... params) {
        String result;
        String workingDir = "";
        try {
            Log.d(this.getClass().getName(), "Started unzip process for file " + params[0]);

            // First we create a directory to hold the unzipped files.
            if (this.isAbsoluteOutputDirectory()) {
                workingDir = "";
            } else {
                workingDir = params[0].substring(0, params[0].lastIndexOf(File.separator)) + File.separator;
            }
            File containerDir = new File(workingDir + params[1]);

            Log.d(this.getClass().getName(), "Magazine Directory: " + containerDir);

            if (containerDir.exists()) {
                Configuration.deleteDirectory(containerDir.getPath());
            }

            if (containerDir.mkdirs()) {
                workingDir = workingDir + params[1] + File.separator;
                this.extract(params[0], workingDir);
                result = "SUCCESS";
            } else {
                Log.e(this.getClass().getName(), "Could not create the package directory");
                //TODO: Notify the user
                result = "ERROR";
            }
        } catch (IOException ex) {
            Log.e(this.getClass().getName(), "Error unzipping the issue.", ex);
            result = "ERROR";
        }

        if (result.equals("SUCCESS")) {
            Log.d(this.getClass().getName(), "Unzip process finished successfully.");
        } else {
            Log.d(this.getClass().getName(), "There was a problem extracting the issue.");
            if (!workingDir.isEmpty()) {
                Configuration.deleteDirectory(workingDir);
            }
        }

        return result;
    }

    private void extract(final String inputFile, final String outputDir) throws IOException  {
        FileInputStream fileInputStream = null;
        ZipArchiveInputStream zipArchiveInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {

            Log.d(this.getClass().getName(), "Will extract " + inputFile + " to " + outputDir);

            byte[] buffer = new byte[8192];
            fileInputStream = new FileInputStream(inputFile);

            // We use null as encoding.
            zipArchiveInputStream = new ZipArchiveInputStream(fileInputStream, null, true);
            ArchiveEntry entry;
            while ((entry = zipArchiveInputStream.getNextEntry()) != null) {
                Log.d(this.getClass().getName(), "Extracting entry " + entry.getName());
                File file = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    fileOutputStream = new FileOutputStream(file);
                    int bytesRead;
                    while ((bytesRead = zipArchiveInputStream.read(buffer, 0, buffer.length)) != -1)
                        fileOutputStream.write(buffer, 0, bytesRead);
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
            }
            if (this.isDeleteZipFile()) {
                // Delete the zip file
                File zipFile = new File(inputFile);
                zipFile.delete();
            }

        } finally {
            try {
                zipArchiveInputStream.close();
                fileInputStream.close();
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (NullPointerException ex) {
                Log.e(this.getClass().getName(), "Error closing the file streams.", ex);
            } catch (IOException ex) {
                Log.e(this.getClass().getName(), "Error closing the file streams.", ex);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
    }

    @Override
    protected void onPostExecute(final String result) {
        mandator.postExecute(taskId, result);
    }

}
