package com.baker.abaker.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.baker.abaker.client.GindMandator;
import com.baker.abaker.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Created by Holland on 02-18-14.
 */
public class CheckInternetTask extends AsyncTask<String, Long, String> {

    private Context context;
    private GindMandator mandator;
    private int taskId;

    public CheckInternetTask(Context _context,
                             GindMandator _mandator,
                             int _taskId) {

        this.context = _context;
        this.mandator = _mandator;
        this.taskId = _taskId;

    }

    @Override
    protected String doInBackground(String... strings) {
        String result;
        if (this.hasInternetConnection()) {
            result = "TRUE";
        } else {
            result = "FALSE";
        }

        Log.d(this.getClass().toString(), "HAS INTERNET CONNECTION: " + result);

        return result;
    }

    public boolean hasInternetConnection() {
        boolean result;
        try {
            final int timeout = Integer.parseInt(this.context.getString(R.integer.check_internet_timeout));
            final String host = this.context.getString(R.string.check_internet_with_host);

            Log.d(this.getClass().toString(), "TESTING INTERNET CONNECTION WITH HOST " + host + " AND TIMEOUT " + timeout);

            //result = InetAddress.getByName(host).isReachable(timeout);

            HttpGet httpGet = new HttpGet(host);
            HttpParams httpParameters = new BasicHttpParams();

            // Set the timeout in milliseconds until a connection is established.
            // The default value is zero, that means the timeout is not used.
            //int timeoutConnection = 3000;
            //HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            HttpConnectionParams.setSoTimeout(httpParameters, timeout);

            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
            HttpResponse response = httpClient.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();
            Log.d(this.getClass().toString(), "THE TESTING INTERNET CONNECTION STATUS CODE WAS " + statusCode);
            if (statusCode == HttpStatus.SC_OK) {
                result = true;
            } else {
                result = false;
            }
        } catch (Exception ioe) {
            Log.e(this.getClass().toString(), ioe.getMessage());
            result = false;
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        this.mandator.postExecute(this.taskId, result);
    }
}
