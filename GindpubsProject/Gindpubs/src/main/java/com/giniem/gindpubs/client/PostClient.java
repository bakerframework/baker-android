package com.giniem.gindpubs.client;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;

/**
 * Created by hollandsystem on 11-18-13.
 */
public class PostClient {

    private ArrayList<NameValuePair> parameters;

    private String url;

    public PostClient() {
        parameters = new ArrayList<NameValuePair>();
    }

    public void setUrl(final String _url) {
        url = _url;
    }

    public void addParameter(final String key, final String value) {
        this.parameters.add(new BasicNameValuePair(key, value));
    }

    public void addParameters(final ArrayList<NameValuePair> _parameters) {
        this.parameters = _parameters;
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public String doPost() {
        String result = "ERROR";

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            if (this.parameters != null) {
                httpPost.setEntity(new UrlEncodedFormEntity(this.parameters));
            }

            HttpResponse response = httpClient.execute(httpPost);

            if (null != response) {

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    result = EntityUtils.toString(response.getEntity());
                    Log.d(this.getClass().toString(), "POST REQUEST SUCCEEDED: " + result);
                } else {
                    Log.e(this.getClass().toString(), "BAD RESPONSE FROM SERVER: " + statusCode);
                }
            } else {
                Log.e(this.getClass().toString(), "RESPONSE IS NULL");
            }

        } catch (Exception ex) {
            Log.e(this.getClass().toString(), "EXCEPTION WHILE SENDING POST REQUEST: ", ex);
        }
        return result;
    }
}
