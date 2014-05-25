package me.tatarka.timesync.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimeSyncBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TimeSyncService.start(context);
    }

    public static void enable(Context context) {
        ReceiverUtils.enable(context, TimeSyncBootReceiver.class);
    }

    public static void disable(Context context) {
        ReceiverUtils.disable(context, TimeSyncBootReceiver.class);
    }
}
