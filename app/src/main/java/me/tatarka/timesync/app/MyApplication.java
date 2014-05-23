package me.tatarka.timesync.app;

import android.app.Application;

import me.tatarka.timesync.lib.TimeSync;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TimeSync.start(this);
    }
}
