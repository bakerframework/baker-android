/**
 * Copyright (c) 2013-2014. Francisco Contreras, Holland Salazar.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the Baker Framework nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
package com.baker.abaker.client;

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
