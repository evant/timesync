package me.tatarka.timesync.app;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Random;

import me.tatarka.timesync.lib.TimeSyncConfig;
import me.tatarka.timesync.lib.TimeSyncListener;

import static me.tatarka.timesync.lib.TimeSyncConfig.SECONDS;

public class RandomSync implements TimeSyncListener {
    public static final String BROADCAST = RandomSync.class.getName();
    public static final String EXTRA_RESULT = "result";

    private Random random = new Random();

    @Override
    public TimeSyncConfig getConfig() {
        return new TimeSyncConfig().every(5, SECONDS).range(1, SECONDS);
    }

    @Override
    public void onSync(Context context) throws Exception {
        Log.d("TimeSync", "sync: " + System.currentTimeMillis());

        // Do (no) work for one second.
        Thread.sleep(1000);
        long result = random.nextLong();

        // Normally you would save to a database or file. For this example, it's just easier to
        // broadcast the result.
        Intent intent = new Intent(BROADCAST);
        intent.putExtra(EXTRA_RESULT, result);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
