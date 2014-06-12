/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baker.abaker;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

public class ABakerApp extends Application implements AnalyticsEvents {

    public enum TrackerName {
        GLOBAL_TRACKER
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public ABakerApp() {
        super();
    }

    @Override
    public void sendEvent(String category, String action, String label) {
        Tracker tracker = this.getTracker(TrackerName.GLOBAL_TRACKER);

        Log.d(this.getClass().getName(), "Sending event to Google Analytics with Category: "
                + category
                + ", Action: " + action
                + ", Label: " + label);

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    @Override
    public void sendTimingEvent(String category, long value, String name, String label) {
        Tracker tracker = this.getTracker(TrackerName.GLOBAL_TRACKER);

        Log.d(this.getClass().getName(), "Sending user timing event to Google Analytics with Category: "
                + category
                + ", Value: " + value
                + ", Name: " + name
                + ", Label: " + label);

        // Build and send timing.
        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setValue(value)
                .setVariable(name)
                .setLabel(label)
                .build());
    }

    /**
     * We return our only configured tracker.
     *
     * @param trackerId the name of tracker, in case others are added.
     * @return Tracker the Tracker.
     */
    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker tracker = (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
                    : null;
            mTrackers.put(trackerId, tracker);

        }
        return mTrackers.get(trackerId);
    }
}
