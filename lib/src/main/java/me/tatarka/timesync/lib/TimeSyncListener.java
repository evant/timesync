package me.tatarka.timesync.lib;

import android.content.Context;

public final class TimeSyncListener {
    private Context context;
    private String name;
    private TimeSync listener;

    TimeSyncListener(Context context, String name) {
        this.context = context;
        this.name = name;
        listener = TimeSyncParser.parseListeners(context).get(name);
    }

    public void sync() {
        TimeSyncService.sync(context, name);
    }

    public void setEnabled(boolean value) {
        listener.config().edit().enable(value).save();
        if (value) {
            TimeSyncService.start(context, name);
        } else {
            TimeSyncService.stop(context, name);
        }
    }

    public boolean isEnabled() {
        return listener.config().enabled();
    }
}
