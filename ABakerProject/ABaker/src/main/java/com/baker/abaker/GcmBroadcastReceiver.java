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
package com.baker.abaker;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.baker.abaker.settings.Configuration;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONObject;

public class GcmBroadcastReceiver extends BroadcastReceiver {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;

    /**
     * 0 = unknown error.
     * 1 = notifications not enabled.
     * 2 = downloads not enabled.
     * 3 = no mobile network enabled and no Wi-Fi available.
     * 4 = can download on either a mobile or Wi-Fi network.
     */
    public int processFinishedCode = 0;

    public boolean receiveNotifications;
    public boolean receiveNotificationsDownload;
    public boolean receiveNotificationsDownloadOnlyWifi;
    public boolean canDownload;

    public String messageType;

    public boolean testing = false;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (testing == false) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            receiveNotifications = sharedPreferences.getBoolean(Configuration.PREF_RECEIVE_NOTIFICATIONS, true);
            receiveNotificationsDownload = sharedPreferences.getBoolean(Configuration.PREF_RECEIVE_NOTIFICATIONS_DOWNLOAD, true);
            receiveNotificationsDownloadOnlyWifi = sharedPreferences.getBoolean(Configuration.PREF_RECEIVE_NOTIFICATIONS_DOWNLOAD_ONLY_WIFI, true);

            canDownload = !receiveNotificationsDownloadOnlyWifi | Configuration.connectionIsWiFi(context);
        }

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        messageType = gcm.getMessageType(intent);

        if (!receiveNotifications) {
            processFinishedCode = 1;
            return;
        }

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            Log.i(this.getClass().toString(), "Received: " + extras.toString());
            /**
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             **/
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification(context, "Error notification", "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification(context, "Deleted messages", "Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                try {
                    JSONObject json = new JSONObject(extras.getString("notificationData"));

                    // Values can be either "standard-notification" or "background-download". This value is required.
                    String type = json.getString("type");

                    if ("standard-notification".equals(type)) {

                        // A title to show at the notification bar in android. This value is optional.
                        String title = json.has("title") ? json.getString("title") : "";

                        // The message description for the notification to show at the notifications bar in android. This value is optional.
                        String message = json.has("message") ? json.getString("message") : "";

                        this.sendNotification(context, title, message);
                    } else if ("background-download".equals(type)) {
                        if (receiveNotificationsDownload) {
                            if (canDownload) {
                                processFinishedCode = 4;
                                if (json.has("issueName")) {
                                    // Values can be "latest" or the name of the issue, for example "magazine-12". This value is required.
                                    String issueName = json.getString("issueName");
                                    Intent gindIntent = new Intent(context, GindActivity.class);
                                    gindIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    gindIntent.putExtra("START_DOWNLOAD", issueName);
                                    context.startActivity(gindIntent);
                                }
                            } else {
                                processFinishedCode = 3;
                                return;
                            }
                        } else {
                            processFinishedCode = 2;
                            return;
                        }

                    }
                } catch (Exception ex) {
                    // Do nothing, if it fails we simply do not process the notification.
                    Log.e(this.getClass().toString(), ex.getMessage());
                }

                // Post notification of received message.
                //sendNotification(context, extras.getString("collapse_key"), extras.getString("message"));
            }
        }
    }

    private boolean appIsRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo app : manager.getRunningAppProcesses()) {
            if (context.getApplicationContext().getPackageName().trim().equals(app.processName)) {
                return true;
            }
        }
        return false;
    }

    private void sendNotification(Context context, String title, String message) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, GindActivity.class);
        intent.putExtra("START_DOWNLOAD", message);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
