package me.tatarka.timesync.lib;

import android.content.Context;
import android.content.SharedPreferences;

class TimeSyncPreferences {
    static final String NAME = "me.tatarka.timesync.SHARED_PREFS";
    private static final String SEED = "seed";
    private static final String POWER_CONNECTED = "power_connected";

    private SharedPreferences prefs;

    TimeSyncPreferences(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    long getSeed() {
        return prefs.getLong(SEED, 0);
    }

    void setSeed(long seed) {
        prefs.edit().putLong(SEED, seed).commit();
    }

    boolean isPowerConnected() {
        return prefs.getBoolean(POWER_CONNECTED, false);
    }

    void setPowerConnected(boolean value) {
        prefs.edit().putBoolean(POWER_CONNECTED, value).commit();
    }
}
