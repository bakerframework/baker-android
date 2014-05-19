package com.baker.abaker.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.baker.abaker.settings.Configuration;
import com.baker.abaker.client.GindMandator;

import java.io.File;

public class MagazineDeleteTask extends AsyncTask<String, Long, String> {

    private Context context;

    private GindMandator mandator;

    private int taskId;

    public MagazineDeleteTask(Context context, GindMandator mandator, final int taskId) {
        this.context = context;
        this.mandator = mandator;
        this.taskId = taskId;
    }

    @Override
    protected String doInBackground(String... params) {
        String result = "ERROR";

        String filepath = Configuration.getMagazinesDirectory(this.context)
                + File.separator + params[0];

        if (Configuration.deleteDirectory(filepath)) {
            result = "SUCCESS";
            Log.d(this.getClass().toString(), "Delete process finished successfully.");
        } else {
            Log.e(this.getClass().toString(), "Could not delete directory: " + params[0]);
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
    }

    @Override
    protected void onPostExecute(final String result) {
        mandator.postExecute(taskId, result);
    }

}
