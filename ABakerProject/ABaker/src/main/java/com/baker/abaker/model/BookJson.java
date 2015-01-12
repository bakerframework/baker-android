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
package com.baker.abaker.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookJson {

	private String hpub;

	private String magazineName;
	
	private String title;

	private List<String> authors;

	private List<String> creators;

	private Date date;

	private String url;

	private String cover;

	private String orientation;

	private boolean zoomable;

    /**
     * We set it empty by default (not null), because we will use it anyways.
     */
    private String liveUrl;

	private List<String> contents;
	
	public BookJson() {
		this.hpub = "1";
		this.date = new Date();
		this.authors = new ArrayList<String>();
		this.creators = new ArrayList<String>();
		this.contents = new ArrayList<String>();
		this.title = "";
		this.url = "";
		this.cover = "";
		this.orientation = "";
		this.zoomable = false;
	}

	public String getHpub() {
		return hpub;
	}

	public void setHpub(String hpub) {
		this.hpub = hpub;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public boolean isZoomable() {
		return zoomable;
	}

	public void setZoomable(boolean zoomable) {
		this.zoomable = zoomable;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

	public List<String> getCreators() {
		return creators;
	}

	public void setCreators(List<String> creators) {
		this.creators = creators;
	}

	public List<String> getContents() {
		return contents;
	}

	public void setContents(List<String> contents) {
		this.contents = contents;
	}

	public String getMagazineName() {
		return magazineName;
	}

	public void setMagazineName(String magazineName) {
		this.magazineName = magazineName;
	}

    public String getLiveUrl() {
        return liveUrl;
    }

    public void setLiveUrl(String liveUrl) {
        this.liveUrl = liveUrl;
    }

    public void fromJson(final String jsonString) throws JSONException,
			ParseException {
		JSONObject json = new JSONObject(jsonString);
        if (json.has("liveUrl")) {
            this.liveUrl = json.getString("liveUrl");
        }
//		SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd",
//				Locale.US);

		// The other properties are commented by now, as we are not gonna use them yet.
		
		this.hpub = json.optString("hpub", "1");
		this.title = json.optString("title", "");
//		this.date = sdfInput.parse(json.getString("date"));
		this.url = json.optString("url", "");
		this.cover = json.optString("cover", "");
        this.orientation = json.optString("orientation", "PORTRAIT");
//		this.zoomable = json.getBoolean("zoomable");

//		JSONArray authors = new JSONArray(json.getString("author"));
//		JSONArray creators = new JSONArray(json.getString("creator"));
		JSONArray contents = new JSONArray(json.getString("contents"));
//		this.authors = new ArrayList<String>();
//		this.creators = new ArrayList<String>();
		this.contents = new ArrayList<String>();

//		for (int i = 0; i < authors.length(); i++) {
//			this.authors.add(authors.getString(i));
//		}
//
//		for (int i = 0; i < creators.length(); i++) {
//			this.creators.add(creators.getString(i));
//		}

		for (int i = 0; i < contents.length(); i++) {
			this.contents.add(contents.getString(i));
		}
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject result = new JSONObject();
		SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd",
				Locale.US);
		
		result.put("hpub", this.hpub);
		result.put("title", this.title);
		result.put("date", sdfInput.format(this.date));
		result.put("url", this.url);
		result.put("cover", this.cover);
		result.put("orientation", this.orientation);
		result.put("zoomable", this.zoomable);
        result.put("liveUrl", this.liveUrl);
		
		JSONArray authors = new JSONArray();
		JSONArray creators = new JSONArray();
		JSONArray contents = new JSONArray();
		
		for (String author : this.authors) {
			authors.put(author);
		}
		result.put("author", authors);
		
		for (String creator : this.creators) {
			creators.put(creator);
		}
		result.put("creator", creators);
		
		for (String content : this.contents) {
			contents.put(content);
		}
		result.put("contents", contents);
		
		return result;
	}
}
