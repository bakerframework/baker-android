package com.baker.abaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;

public class ABakerAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(this.getClass().getName(), "Received RTC_WAKEUP alarm!");
            DownloaderClientMarshaller
                    .startDownloadServiceIfRequired(context, intent, ABakerDownloaderService.class);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
