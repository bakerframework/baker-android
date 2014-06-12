package com.baker.abaker;

public interface AnalyticsEvents {

    public void sendEvent(final String category, final String action, final String label);

    public void sendTimingEvent(final String category, final long value, final String name, final String label);
}
