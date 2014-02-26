package com.giniem.gindpubs.client;

/**
 * Created by francisco contreras on 9/12/13.
 */
public interface GindMandator {

    public void updateProgress(final int taskId, Long... progress);
    public void postExecute(final int taskId, String... results);
}
