package me.tatarka.timesync.lib;

import android.content.Context;

public final class TimeSyncListener {
    private Context context;
    private String name;
    private TimeSync.Config config;

    TimeSyncListener(Context context, String name) {
        this.context = context;
        this.name = name;
        config = new TimeSync.Config(context, name);
    }

    public void sync() {
        TimeSyncService.sync(context, name);
    }

    public void setEnabled(boolean value) {
        config.edit().enable(value).save();
        if (value) {
            TimeSyncService.start(context, name);
        } else {
            TimeSyncService.stop(context, name);
        }
    }

    public boolean isEnabled() {
        return config.enabled();
    }
}
