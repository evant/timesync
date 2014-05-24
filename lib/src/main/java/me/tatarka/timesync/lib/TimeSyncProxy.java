package me.tatarka.timesync.lib;

import android.content.Context;

import java.util.Arrays;

public final class TimeSyncProxy {
    private Context context;
    private String name;
    private TimeSync listener;

    TimeSyncProxy(Context context, String name) {
        this.context = context;
        this.name = name;
        listener = TimeSyncParser.parseListeners(context).get(name);
    }

    public void sync() {
        TimeSyncService.sync(context, name);
    }

    public TimeSync.Config config() {
        return listener.config();
    }

    public void edit(Iterable<TimeSync.Edit> edits) {
        listener.edit(edits);
        TimeSyncService.update(context, name);
    }

    public void edit(TimeSync.Edit... edits) {
        edit(Arrays.asList(edits));
    }
}
