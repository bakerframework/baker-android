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

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

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