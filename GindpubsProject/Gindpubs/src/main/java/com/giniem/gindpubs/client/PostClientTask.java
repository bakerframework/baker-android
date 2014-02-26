package com.giniem.gindpubs.client;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

/**
 * Created by hollandsystem on 11-02-13.
 */
public class PostClientTask extends AsyncTask<Void, Void, String> {

    private int asyncTaskId;

    private GindMandator mandator;

    private String url;

    private PostClient postClient;

    private ArrayList<NameValuePair> parameters;

    public PostClientTask(final int taskId, GindMandator _mandator) {
        asyncTaskId = taskId;
        mandator = _mandator;
        postClient = new PostClient();
    }

    public void setUrl(final String _url) {
        url = _url;
    }

    public void addParameter(final String key, final String value) {
        this.parameters.add(new BasicNameValuePair(key, value));
    }

    @Override
    protected String doInBackground(Void... params) {

        String result;

        postClient.setUrl(url);
        postClient.clearParameters();
        if (parameters != null) {
            postClient.addParameters(parameters);
        }

        result = postClient.doPost();

        Log.d(this.getClass().toString(), "RAW POST RESPONSE:" + result);

        if ("ERROR".equals(result)) {
            Log.e(this.getClass().toString(), "ERROR RESPONSE FROM POST CLIENT");
        }

        return result;
    }

    @Override
    protected void onPostExecute(final String results) {
        mandator.postExecute(asyncTaskId, results);
    }
}