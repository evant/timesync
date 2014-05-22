package me.tatarka.timesync.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class TimeSyncNetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            disable(context);
            TimeSyncService.networkBack(context);
        }
    }

    static void enable(Context context) {
        ReceiverUtils.enable(context, TimeSyncNetworkReceiver.class);
    }

    static void disable(Context context) {
        ReceiverUtils.disable(context, TimeSyncNetworkReceiver.class);
    }
}
