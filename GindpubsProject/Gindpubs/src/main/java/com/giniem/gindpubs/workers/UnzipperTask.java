package com.giniem.gindpubs.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.giniem.gindpubs.Configuration;
import com.giniem.gindpubs.client.GindMandator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipperTask extends AsyncTask<String, Long, String> {

    private Context context;

    private GindMandator mandator;

    private int taskId;

    private boolean resumed;

    public void setResumed(final boolean _resumed) {
        this.resumed = _resumed;
    }

	public UnzipperTask(Context context, GindMandator mandator, final int taskId) {
        this.context = context;
        this.mandator = mandator;
        this.taskId = taskId;
	}

    @Override
    protected String doInBackground(String... params) {
        InputStream input;
        ZipInputStream zipInput;
        try {
            String zipEntryName;

            Log.d(this.getClass().getName(),"Started unzip process for file " + params[0]);

            // First we create a directory to hold the unzipped files.
            String workingDir = params[0].substring(0, params[0].lastIndexOf("/")) + File.separator;
            File containerDir = new File(workingDir + params[1]);

            Log.d(this.getClass().getName(), "Magazine Directory: " + containerDir);

            if (containerDir.exists()) {
                Configuration.deleteDirectory(containerDir.getPath());
            }

            if (containerDir.mkdirs()) {
                input = new FileInputStream(params[0]);

                workingDir = workingDir + params[1] + File.separator;
                zipInput = new ZipInputStream(new BufferedInputStream(input));
                ZipEntry zipEntry;
                byte[] buffer = new byte[1024];
                int read;

                while ((zipEntry = zipInput.getNextEntry()) != null) {

                    zipEntryName = zipEntry.getName();
                    Log.d(this.getClass().toString(), "Unzipping entry " + zipEntryName);
                    if (zipEntry.isDirectory()) {
                        File innerDirectory = new File(workingDir + zipEntryName);
                        innerDirectory.mkdirs();
                        continue;
                    }

                    FileOutputStream output = new FileOutputStream(workingDir + zipEntryName);
                    while ((read = zipInput.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }

                    output.close();
                    zipInput.closeEntry();
                }

                zipInput.close();

                // Delete the zip file
                File zipFile = new File(params[0]);
                zipFile.delete();
            } else {
                Log.e(this.getClass().getName(), "Could not create the package directory");
                //TODO: Notify the user
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Log.d(this.getClass().getName(), "Unzip process finished successfully.");
		return "SUCCESS";
	}

    @Override
    protected void onProgressUpdate(Long... progress) {
    }

    @Override
    protected void onPostExecute(final String result) {
        mandator.postExecute(taskId, result);
    }

}
