package com.giniem.gindpubs.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class CustomWebViewPager extends ViewPager {

    public CustomWebViewPager(Context context) {
		super(context);
	}

	public CustomWebViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected boolean canScroll(View view, boolean checkV, int dx, int x, int y) {
		if (view instanceof CustomWebView) {
			return ((CustomWebView) view).canScrollHorizontal(-dx);
		} else {
			return super.canScroll(view, checkV, dx, x, y);
		}
	}
}
