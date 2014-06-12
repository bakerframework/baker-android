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
package com.baker.abaker.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.baker.abaker.MagazineActivity;
import com.baker.abaker.R;
import com.baker.abaker.settings.Configuration;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewFragment extends Fragment {

	public static final String ARG_OBJECT = "object";

    private FrameLayout progressBarContainer;
	private CustomWebView webView;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;
    public CustomChromeClient chromeClient = new CustomChromeClient();
    private MagazineActivity activity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

        this.activity = (MagazineActivity) this.getActivity();

		// The last two arguments ensure LayoutParams are inflated
		// properly.
		View rootView = inflater.inflate(R.layout.fragment_collection_object,
				container, false);
		Bundle args = getArguments();

		customViewContainer = (FrameLayout) this.getActivity().findViewById(R.id.customViewContainer);
		
		webView = (CustomWebView) rootView.findViewById(R.id.webpage1);
        progressBarContainer = (FrameLayout) rootView.findViewById(R.id.progressBarContainer);

        //Enable javascript
        webView.getSettings().setJavaScriptEnabled(true);
        //Set zoom enabled/disabled
        webView.getSettings().setSupportZoom(true);
        //Support zoom like normal browsers
        webView.getSettings().setUseWideViewPort(true);
        //Disable zoom buttons
        webView.getSettings().setDisplayZoomControls(false);
        //Add zoom controls
        webView.getSettings().setBuiltInZoomControls(true);
        //Load the page on the maximum zoom out available.
        webView.getSettings().setLoadWithOverviewMode(true);

        webView.setWebChromeClient(chromeClient);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String stringUrl) {

                // mailto links will be handled by the OS.
                if (stringUrl.startsWith("mailto:")) {
                    Uri uri = Uri.parse(stringUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } else {
                    try {
                        URL url = new URL(stringUrl);

                        // We try to remove the referrer string to avoid passing it to the server in case the URL is an external link.
                        String referrer = "";
                        if (url.getQuery() != null) {
                            Map<String, String> variables = Configuration.splitUrlQueryString(url);
                            String finalQueryString = "";
                            for (Map.Entry<String, String> entry : variables.entrySet()) {
                                if (entry.getKey().equals("referrer")) {
                                    referrer = entry.getValue();
                                } else {
                                    finalQueryString += entry.getKey() + "=" + entry.getValue() + "&";
                                }
                            }
                            if (!finalQueryString.isEmpty()) {
                                finalQueryString = "?" + finalQueryString.substring(0, finalQueryString.length() - 1);
                            }
                            stringUrl = stringUrl.replace("?" + url.getQuery(), finalQueryString);
                        }
                        // Aaaaand that was the process of removing the referrer from the query string.

                        if (!url.getProtocol().equals("file")) {
                            Log.d("REFERRER>>>", "THE REFERRER IS: " + referrer);
                            if (referrer.equals(WebViewFragment.this.activity.getString(R.string.url_external_referrer))) {
                                Uri uri = Uri.parse(stringUrl);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            } else if (referrer.equals(WebViewFragment.this.activity.getString(R.string.url_gindpubs_referrer))) {
                                WebViewFragment.this.activity.openLinkInModal(stringUrl);
                                return true;
                            } else {
                                // We return false to tell the webview that we are not going to handle the URL override.
                                return false;
                            }
                        } else {
                            stringUrl = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
                            Log.d(">>>URL_DATA", "FINAL INTERNAL HTML FILENAME = " + stringUrl);

                            int index = activity.getJsonBook().getContents().indexOf(stringUrl);

                            if (index != -1) {
                                Log.d(this.getClass().toString(), "Index to load: " + index
                                        + ", page: " + stringUrl);

                                activity.getPager().setCurrentItem(index);
                                view.setVisibility(View.GONE);
                            } else {

                                // If the file DOES NOT exist, we won't load it.

                                File htmlFile = new File(url.getPath());
                                if (htmlFile.exists()) {
                                    return false;
                                }
                            }
                        }
                    } catch (MalformedURLException ex) {
                        Log.d(">>>URL_DATA", ex.getMessage());
                    } catch (UnsupportedEncodingException ex) {
                    }
                }

                return true;
            }
        });

        webView.loadUrl(args.getString(ARG_OBJECT));

		return rootView;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.getWebView().destroy();
    }
	
	public String getUrl() {
		return this.webView.getUrl();
	}
	
	public boolean inCustomView() {
        return (customView != null);
    }

    public void hideCustomView() {
    	chromeClient.onHideCustomView();
    }

    public CustomWebView getWebView() {
        return this.webView;
    }

	class CustomChromeClient extends WebChromeClient {

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
           onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {

            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            webView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (customView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            customView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(customView);
            customViewCallback.onCustomViewHidden();

            activity.resetOrientation();

            customView = null;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {

            if (100 == newProgress) {
                progressBarContainer.setVisibility(View.GONE);
            }
        }
    }
}