package me.tatarka.timesync.lib;

import android.content.Context;

public class TimeSync {
    public static void start(Context context) {
        TimeSyncService.start(context);
    }

    public static void sync(Context context, Class<? extends TimeSyncListener> listener) {
        TimeSyncService.sync(context, listener.getName());
    }
}
