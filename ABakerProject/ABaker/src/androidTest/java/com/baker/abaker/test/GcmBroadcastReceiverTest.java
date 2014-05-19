package com.baker.abaker.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

import com.baker.abaker.GcmBroadcastReceiver;

public class GcmBroadcastReceiverTest extends AndroidTestCase {

    public GcmBroadcastReceiver broadcastReceiver;
    public Context testContext;
    public Intent intent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        broadcastReceiver = new GcmBroadcastReceiver();

        // Set environment to testing to avoid loading the shared preferences
        // values from the app.
        broadcastReceiver.testing = true;

        // We use the AndroidTestCase's context, but a mock context should be used as well.
        testContext = getContext();

        // We are going to use this intent in the tests.
        intent = new Intent();

        // GCM action.
        intent.setAction("com.google.android.c2dm.intent.RECEIVE");

        // Setting notification data to simulate a notification from GCM.
        Bundle extras = new Bundle();
        extras.putString("notificationData", "{\"issueName\":\"latest\",\"type\":\"background-download\"}");
        extras.putString("from", "356573692838");
        extras.putString("collapse_key", "New Magazine Available");
        intent.putExtras(extras);
    }

    public void testNotificationsDisabled() {
        // Setting variables
        broadcastReceiver.receiveNotifications = false;
        broadcastReceiver.receiveNotificationsDownload = false;
        broadcastReceiver.receiveNotificationsDownloadOnlyWifi = true;
        broadcastReceiver.canDownload = false;

        broadcastReceiver.onReceive(testContext, intent);

        // Should be null, as no other receiver has set another value.
        assertNull(broadcastReceiver.getResultData());

        // Message type should be "gcm" as the testIntent's action is from google cloud messaging.
        assertEquals("gcm", broadcastReceiver.messageType);

        // Process should end with 1 as the notifications are disabled.
        assertEquals(1, broadcastReceiver.processFinishedCode);
    }

    public void testDownloadNotificationsDisabled() {
        // Setting variables
        broadcastReceiver.receiveNotifications = true;
        broadcastReceiver.receiveNotificationsDownload = false;
        broadcastReceiver.receiveNotificationsDownloadOnlyWifi = true;
        broadcastReceiver.canDownload = false;

        broadcastReceiver.onReceive(testContext, intent);

        // Should be null, as no other receiver has set another value.
        assertNull(broadcastReceiver.getResultData());

        // Message type should be "gcm" as the testIntent's action is from google cloud messaging.
        assertEquals("gcm", broadcastReceiver.messageType);

        // Process should end with 1 as the notifications are disabled.
        assertEquals(2, broadcastReceiver.processFinishedCode);
    }

    public void testNoMobileNoWiFi() {
        // Setting variables
        broadcastReceiver.receiveNotifications = true;
        broadcastReceiver.receiveNotificationsDownload = true;
        broadcastReceiver.receiveNotificationsDownloadOnlyWifi = true;
        broadcastReceiver.canDownload = false;

        broadcastReceiver.onReceive(testContext, intent);

        // Should be null, as no other receiver has set another value.
        assertNull(broadcastReceiver.getResultData());

        // Message type should be "gcm" as the testIntent's action is from google cloud messaging.
        assertEquals("gcm", broadcastReceiver.messageType);

        // Process should end with 1 as the notifications are disabled.
        assertEquals(3, broadcastReceiver.processFinishedCode);
    }

    public void testProcessDownloadNotification() {
        // Setting variables
        broadcastReceiver.receiveNotifications = true;
        broadcastReceiver.receiveNotificationsDownload = true;
        broadcastReceiver.receiveNotificationsDownloadOnlyWifi = false;
        broadcastReceiver.canDownload = true;

        broadcastReceiver.onReceive(testContext, intent);

        // Should be null, as no other receiver has set another value.
        assertNull(broadcastReceiver.getResultData());

        // Message type should be "gcm" as the testIntent's action is from google cloud messaging.
        assertEquals("gcm", broadcastReceiver.messageType);

        // Process should end with 1 as the notifications are disabled.
        assertEquals(4, broadcastReceiver.processFinishedCode);
    }
}
