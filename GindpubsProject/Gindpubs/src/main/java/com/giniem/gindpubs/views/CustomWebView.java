package com.giniem.gindpubs.views;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class CustomWebView extends WebView {
	
	public CustomWebView(Context context) {
        super(context);
    }
	
	public CustomWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/**
	 * API 14 already has a function canScrollHorizontally defined, we just added this
	 * to support devices before API 14.
	 */
	public boolean canScrollHorizontal(int direction) {
        final int offset = computeHorizontalScrollOffset();
        final int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (range == 0) 
        	return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }
}
