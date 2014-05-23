package me.tatarka.timesync.lib;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.Map;
import java.util.Random;

import static me.tatarka.timesync.lib.MathUtil.randomInRange;

public class TimeSyncService extends IntentService {
    private static final String TYPE = "type";
    private static final int TYPE_START = 0;
    private static final int TYPE_STOP = 1;
    private static final int TYPE_SYNC = 2;
    private static final int TYPE_NETWORK_BACK = 3;
    private static final int TYPE_POWER_CHANGED = 4;
    private static final int TYPE_START_ONE = 5;
    private static final int TYPE_STOP_ONE = 6;

    private static final String NAME = "name";
    private static final String POWER_CONNECTED = "power_connected";

    private TimeSyncPreferences prefs;
    private long seed;
    private boolean powerConnected;

    private Map<String, TimeSync> listeners;

    public TimeSyncService() {
        super(TimeSyncService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = new TimeSyncPreferences(this);
        seed = findOrCreateSeed(prefs);
        powerConnected = prefs.isPowerConnected();

        listeners = TimeSyncParser.parseListeners(this);
        for (TimeSync listener : listeners.values()) {
            listener.onCreate(this);
            listener.ensureOnCreate();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (TimeSync listener : listeners.values()) {
            listener.onDestroy(this);
            listener.ensureOnDestroy();
        }
    }

    private int getResource() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            int res = appInfo.metaData.getInt(TimeSync.META_DATA_NAME);
            if (res == 0) {
                throw new IllegalArgumentException("You must declare <meta-data android:name=\"" + TimeSync.META_DATA_NAME + "\" android:resource=\"@xml/[RESOURCE_NAME]\"/> in your AndroidManifest.xml");
            }
            return res;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static void start(Context context) {
        context.startService(getStartIntent(context));
    }

    static void start(Context context, String name) {
        context.startService(getStartIntent(context, name));
    }

    static void stop(Context context) {
        context.startService(getStopIntent(context));
    }

    static void stop(Context context, String name) {
        context.startService(getStopIntent(context, name));
    }

    static void sync(Context context, String name) {
        context.startService(getSyncIntent(context, name));
    }

    static void networkBack(Context context) {
        context.startService(getNetworkBackIntent(context));
    }

    static void powerChanged(Context context, boolean connected) {
        context.startService(getPowerChangedIntent(context, connected));
    }

    static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_START);
        return intent;
    }

    static Intent getStartIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_START_ONE);
        intent.putExtra(NAME, name);
        return intent;
    }

    static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_STOP);
        return intent;
    }

    static Intent getStopIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_STOP_ONE);
        intent.putExtra(NAME, name);
        return intent;
    }

    static Intent getSyncIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.setData(Uri.parse("timesync://" + name));
        intent.putExtra(TYPE, TYPE_SYNC);
        intent.putExtra(NAME, name);
        return intent;
    }

    static Intent getNetworkBackIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_NETWORK_BACK);
        return intent;
    }

    static Intent getPowerChangedIntent(Context context, boolean connected) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_POWER_CHANGED);
        intent.putExtra(POWER_CONNECTED, connected);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getIntExtra(TYPE, 0)) {
            case TYPE_START: {
                onHandleStart();
                break;
            }
            case TYPE_START_ONE: {
                String name = intent.getStringExtra(NAME);
                TimeSync listener = listeners.get(name);
                if (listener != null) {
                    add(listener);
                }
                break;
            }
            case TYPE_STOP: {
                onHandleStop();
                break;
            }
            case TYPE_STOP_ONE: {
                String name = intent.getStringExtra(NAME);
                TimeSync listener = listeners.get(name);
                if (listener != null) {
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    remove(alarmManager, listener);
                }
                break;
            }
            case TYPE_SYNC: {
                String name = intent.getStringExtra(NAME);
                TimeSync listener = listeners.get(name);
                if (listener != null) {
                    onHandleSync(listener);
                }
                break;
            }
            case TYPE_NETWORK_BACK: {
                for (TimeSync listener : listeners.values()) {
                    add(listener);
                }
                break;
            }
            case TYPE_POWER_CHANGED: {
                boolean connected = intent.getBooleanExtra(POWER_CONNECTED, false);
                onHandlePowerChanged(connected);
                // TimeSyncPowerReceiver is a WakefulBroadcastReceiver, so make sure to release the lock.
                TimeSyncPowerReceiver.completeWakefulIntent(intent);
                break;
            }
        }
    }

    private void onHandleStart() {
        removeAll();
        for (TimeSync listener : listeners.values()) {
            listener.onStart(this);
            listener.ensureOnStart();
            add(listener);
        }
        TimeSyncPowerReceiver.enable(this);
    }

    private void onHandleStop() {
        removeAll();
        for (TimeSync listener : listeners.values()) {
            listener.onStop(this);
            listener.ensureOnStop();
        }
        TimeSyncNetworkReceiver.disable(this);
        TimeSyncPowerReceiver.disable(this);
    }

    private void add(TimeSync listener) {
        if (!listener.config().enabled()) return;

        long time = calculateTime(listener);

        if (time > 0) {
            int alarmType = powerConnected ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = getSyncIntent(this, listener.getClass().getName());
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager.cancel(pendingIntent);
            alarmManager.set(alarmType, time, pendingIntent);
        }
    }

    private void onHandleSync(TimeSync listener) {
        if (!listener.config().enabled()) return;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            try {
                listener.onSync(this);
                add(listener);
            } catch (Exception e) {
                onHandleFailureSyncError(listener);
            }
        } else {
            onHandleFailureNoNetwork(listener);
        }
    }

    private void onHandleFailureNoNetwork(TimeSync listener) {
        removeAll();
        TimeSyncNetworkReceiver.enable(this);
    }

    private void removeAll() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (TimeSync listener : listeners.values()) {
            remove(alarmManager, listener);
        }
    }

    private void remove(AlarmManager alarmManager, TimeSync listener) {
        Intent intent = getSyncIntent(this, listener.getClass().getName());
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private long calculateTime(TimeSync listener) {
        TimeSync.Config config = listener.config();
        long exactTime = EventCalculator.getNextEvent(System.currentTimeMillis(), config.every());
        long range = config.range();
        long rangeOffset = randomInRange(seed, -range / 2, range / 2);
        return exactTime + rangeOffset;
    }

    private void onHandleFailureSyncError(TimeSync listener) {
        //TODO
    }

    private void onHandlePowerChanged(boolean connected) {
        powerConnected = connected;
        prefs.setPowerConnected(connected);
        // Remove and re-add alarms to take into account the state change.
        removeAll();
        for (TimeSync listener : listeners.values()) {
            add(listener);
        }
    }

    private long findOrCreateSeed(TimeSyncPreferences prefs) {
        long seed = prefs.getSeed();
        if (seed != 0) return seed;

        String id = Build.SERIAL + Settings.Secure.ANDROID_ID;

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            String deviceId = telephonyManager.getDeviceId();
            if (deviceId != null) id += deviceId;
        }

        // Use Random to evenly distribute values
        seed = new Random(id.hashCode()).nextLong();
        prefs.setSeed(seed);

        return seed;
    }

}
