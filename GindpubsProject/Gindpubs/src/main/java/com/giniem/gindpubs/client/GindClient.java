package com.giniem.gindpubs.client;

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
