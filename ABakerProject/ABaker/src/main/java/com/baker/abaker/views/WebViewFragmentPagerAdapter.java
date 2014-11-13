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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.baker.abaker.MagazineActivity;
import com.baker.abaker.model.BookJson;

import java.io.File;

public class WebViewFragmentPagerAdapter extends FragmentStatePagerAdapter {

	private BookJson book;

	private String magazinePath;

    private MagazineActivity magazineActivity;
	
	public WebViewFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	public WebViewFragmentPagerAdapter(FragmentManager fm, BookJson book,
			final String magazinePath, MagazineActivity _magazineActivity) {
		super(fm);
        this.magazineActivity = _magazineActivity;
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

//        String page = this.magazinePath + book.getMagazineName() + File.separator
//                + book.getContents().get(i);

        // Magazine path should contain the magazine name and not end with "/".
        String page = this.magazinePath + File.separator + book.getContents().get(i);
        Log.d(this.getClass().getName(), "Loading page " + page);
        args.putString(WebViewFragment.ARG_OBJECT, page);

        Fragment fragment = Fragment.instantiate(magazineActivity, WebViewFragment.class.getName(), args);

        return fragment;
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
