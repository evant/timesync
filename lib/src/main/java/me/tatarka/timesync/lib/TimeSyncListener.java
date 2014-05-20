package me.tatarka.timesync.lib;

import android.content.Context;

public interface TimeSyncListener {
    TimeSyncConfig getConfig();
    void onSync(Context context) throws Exception;
}
