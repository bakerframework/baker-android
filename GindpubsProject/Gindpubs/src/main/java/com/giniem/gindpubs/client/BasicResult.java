package com.giniem.gindpubs.client;

import org.json.JSONException;
import org.json.JSONObject;

public class BasicResult {

	private String type;
	
	private String value;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public JSONObject errorJson(final String value) throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("type", "error");
		json.put("value", value);
		
		return json;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("type", this.type);
		json.put("value", this.value);
		
		return json;
	}
	
}
