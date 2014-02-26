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
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class GindClient {
	
	private String shelfJson;
	
	public String getShelfJson() {
		return shelfJson;
	}

	public JSONArray shelfJsonGet(final String url) throws JSONException,
			ParseException, IOException {
		JSONArray json;
		BasicResult basicResult = new BasicResult();

		Log.d(this.getClass().getName(),
				"Sending request to get the shelf JSON data to URL " + url);

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		HttpResponse response = httpClient.execute(httpGet);
		if (null != response) {
			if (response.getStatusLine().getStatusCode() == 200) {
				
				String value = EntityUtils.toString(response.getEntity());
				
				json = new JSONArray(value);
				this.shelfJson = value;
			} else {
				json = new JSONArray();
				json.put(basicResult.errorJson(response.getStatusLine()
						.getStatusCode()
						+ ": "
						+ response.getStatusLine().getReasonPhrase()));
			}
		} else {
			json = new JSONArray();
			json.put(basicResult.errorJson("Error in HTTP response."));
		}

		return json;
	}
	
	public HttpResponse get(final String url) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = null;
		try {
			HttpGet httpGet = new HttpGet(url);

			response = httpClient.execute(httpGet);

		} catch (IOException ex) {
			ex.printStackTrace();
			response = null;
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return response;
	}
}
