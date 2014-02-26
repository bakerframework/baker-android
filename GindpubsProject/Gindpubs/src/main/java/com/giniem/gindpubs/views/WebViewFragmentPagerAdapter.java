package com.giniem.gindpubs.views;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.giniem.gindpubs.model.BookJson;

import java.io.File;

public class WebViewFragmentPagerAdapter extends FragmentStatePagerAdapter {

	private BookJson book;

	private String magazinePath;

    private Context context;
	
	public WebViewFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	public WebViewFragmentPagerAdapter(FragmentManager fm, BookJson book,
			final String magazinePath, Context context) {
		super(fm);
        this.context = context;
		if (null == book) {
			this.book = new BookJson();
		} else {
			this.book = book;
		}
		if (null == magazinePath) {
			this.magazinePath = "";
		} else {
			this.magazinePath = magazinePath;
		}
	}

	@Override
	public Fragment getItem(int i) {
        Bundle args = new Bundle();

        String page = this.magazinePath + book.getMagazineName() + File.separator
                + book.getContents().get(i);
        Log.d(this.getClass().getName(), "Loading page " + page);
        args.putString(WebViewFragment.ARG_OBJECT, page);

        return Fragment.instantiate(context, WebViewFragment.class.getName(), args);
	}

	@Override
	public int getCount() {
		return book.getContents().size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return "OBJECT " + (position + 1);
	}
}
