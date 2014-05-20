package me.tatarka.timesync.lib;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

public class TimeSyncNetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            ComponentName receiver = new ComponentName(context, TimeSyncNetworkReceiver.class);
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL_APP);
            TimeSyncService.networkBack(context);
        }
    }
}
