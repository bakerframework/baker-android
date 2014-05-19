package com.baker.abaker.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.baker.abaker.client.GindMandator;
import com.baker.abaker.GindActivity;
import com.baker.abaker.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

/**
 * Created by Holland on 10-23-13.
 */
public class GCMRegistrationWorker extends AsyncTask<Void, Long, String[]> {

    private GoogleCloudMessaging gcm;
    private Context context;
    private GindMandator mandator;
    private int taskId;

    public void setGcm(GoogleCloudMessaging _gcm) {
        this.gcm = _gcm;
    }

    public GCMRegistrationWorker(Context _context,
                                 GoogleCloudMessaging _gcm,
                                 int _taskId,
                                 GindMandator _mandator) {
        this.gcm = _gcm;
        this.context = _context;
        this.taskId = _taskId;
        this.mandator = _mandator;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        String msg = "ERROR";
        String regid = "";
        try {
            if (gcm == null) {
                gcm = GoogleCloudMessaging.getInstance(context);
            }
            regid = gcm.register(context.getString(R.string.sender_id));

            Log.d(this.getClass().toString(), "The registration ID is: " + regid);

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // The request to your server should be authenticated if your app
            // is using accounts.
            if (sendRegistrationIdToBackend(regid)) {
                Log.d(this.getClass().toString(), "The Registration ID process with backend was successful.");
                msg = "SUCCESS";
            }

            // For this demo: we don't need to send it because the device
            // will send upstream messages to a server that echo back the
            // message using the 'from' address in the message.

            // Persist the regID - no need to register again.
            //storeRegistrationId(context, regid);
        } catch (Exception ex) {
            msg = "ERROR";
            Log.e(this.getClass().toString(), "Error generating the registration ID: " + ex.getMessage());
            ex.printStackTrace();
            // If there is an error, don't just keep trying to register.
            // Require the user to click a button again, or perform
            // exponential back-off.
        }

        String results[] = {msg, regid};

        return results;
    }

    @Override
    protected void onPostExecute(String[] results) {
        this.mandator.postExecute(this.taskId, results);
    }

    private boolean sendRegistrationIdToBackend(String registrationId) {
        boolean result = false;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(context.getString(R.string.post_apns_token_url));

            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();

            postParameters.add(new BasicNameValuePair("app_id", context.getString(R.string.app_id)));
            postParameters.add(new BasicNameValuePair("user_id", GindActivity.userAccount));
            postParameters.add(new BasicNameValuePair("apns_token", registrationId));
            postParameters.add(new BasicNameValuePair("device", "ANDROID"));

            httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

            HttpResponse response = httpClient.execute(httpPost);

            if (null != response) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    result = true;
                } else {
                    Log.e(this.getClass().toString(), "Device Registration ID failed, response from server was " + response.getStatusLine());
                }
            } else {
                Log.e(this.getClass().toString(), "Device Registration ID failed, response is null");
            }

        } catch (Exception ex) {
            Log.e(this.getClass().toString(), "Fatal error when trying to send the registration ID: " + ex.toString());
        }

        return result;
    }
}