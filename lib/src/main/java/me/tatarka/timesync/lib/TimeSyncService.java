package me.tatarka.timesync.lib;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
    private static final int TYPE_UPDATE = 3;
    private static final int TYPE_SYNC_INEXACT = 4;
    private static final int TYPE_NETWORK_BACK = 5;
    private static final int TYPE_POWER_CHANGED = 6;

    private static final String NAME = "name";
    private static final String POWER_CONNECTED = "power_connected";

    private static final long BASE_RETRY_SPAN = 500;
    private static final long MIN_RETRY_CAP = 5 * TimeSync.Config.SECONDS;

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
    }

    static void start(Context context) {
        context.startService(getStartIntent(context));
    }

    static void stop(Context context) {
        context.startService(getStopIntent(context));
    }

    static void sync(Context context, String name) {
        context.startService(getSyncIntent(context, name));
    }

    static void syncInexact(Context context, String name) {
        context.startService(getSyncInexactIntent(context, name));
    }

    static void update(Context context, String name) {
        context.startService(getUpdateIntent(context, name));
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

    static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_STOP);
        return intent;
    }

    static Intent getSyncIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.setData(Uri.parse("timesync://" + name));
        intent.putExtra(TYPE, TYPE_SYNC);
        intent.putExtra(NAME, name);
        return intent;
    }

    static Intent getSyncInexactIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.setData(Uri.parse("timesync://" + name));
        intent.putExtra(TYPE, TYPE_SYNC_INEXACT);
        intent.putExtra(NAME, name);
        return intent;
    }

    static Intent getUpdateIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_UPDATE);
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
            case TYPE_STOP: {
                onHandleStop();
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
            case TYPE_SYNC_INEXACT: {
                String name = intent.getStringExtra(NAME);
                TimeSync listener = listeners.get(name);
                if (listener != null) {
                    onHandleSyncInexact(listener);
                }
                break;
            }
            case TYPE_UPDATE: {
                String name = intent.getStringExtra(NAME);
                TimeSync listener = listeners.get(name);
                if (listener != null) {
                    onHandleUpdate(listener);
                }
                break;
            }
            case TYPE_NETWORK_BACK: {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                for (TimeSync listener : listeners.values()) {
                    add(alarmManager, listener);
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
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        removeAll(alarmManager);
        for (TimeSync listener : listeners.values()) {
            add(alarmManager, listener);
        }
        TimeSyncPowerReceiver.enable(this);
        TimeSyncBootReceiver.enable(this);
    }

    private void onHandleStop() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        removeAll(alarmManager);
        TimeSyncNetworkReceiver.disable(this);
        TimeSyncPowerReceiver.disable(this);
        TimeSyncBootReceiver.disable(this);
    }

    private void add(AlarmManager alarmManager, TimeSync listener) {
        if (!listener.config().enabled()) return;

        TimeSync.Config config = listener.config();
        long span = config.every();
        if (span > 0) {
            long time = calculateTime(span, config.range());
            setAlarm(alarmManager, listener.getName(), time);
        }
    }

    private void setAlarm(AlarmManager alarmManager, String name, long time) {
        if (time > 0) {
            int alarmType = powerConnected ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
            Intent intent = getSyncIntent(this, name);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager.cancel(pendingIntent);
            alarmManager.set(alarmType, time, pendingIntent);
        }
    }

    private void onHandleSync(TimeSync listener) {
        if (!listener.config().enabled()) return;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            try {
                listener.onSync(this);
                prefs.setLastFailedTimeSpan(listener.getName(), 0);
                add(alarmManager, listener);
            } catch (Exception e) {
                onHandleFailureSyncError(alarmManager, listener);
            }
        } else {
            onHandleFailureNoNetwork(alarmManager, listener);
        }
    }

    private void onHandleSyncInexact(TimeSync listener) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long range = listener.config().range();
        long time = calculateTime(0, range);
        remove(alarmManager, listener);
        setAlarm(alarmManager, listener.getName(), time);
    }

    private void onHandleUpdate(TimeSync listener) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        remove(alarmManager, listener);
        add(alarmManager, listener);
    }

    private void onHandleFailureNoNetwork(AlarmManager alarmManager, TimeSync listener) {
        removeAll(alarmManager);
        TimeSyncNetworkReceiver.enable(this);
    }

    private void onHandleFailureSyncError(AlarmManager alarmManager, TimeSync listener) {
        TimeSync.Config config = listener.config();
        long span = config.every();
        if (span < MIN_RETRY_CAP) span = MIN_RETRY_CAP;
        long lastRetrySpan = prefs.getLastFailedTimeSpan(listener.getName());
        long retrySpan = lastRetrySpan == 0 ? BASE_RETRY_SPAN : lastRetrySpan * 2;
        if (retrySpan > span) retrySpan = span;

        prefs.setLastFailedTimeSpan(listener.getName(), retrySpan);
        long time = calculateTime(retrySpan, config.range());
        setAlarm(alarmManager, listener.getName(), time);
    }

    private void removeAll(AlarmManager alarmManager) {
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

    private long calculateTime(long timeSpan, long range) {
        long currentTime = System.currentTimeMillis();
        long exactTime = EventCalculator.getNextEvent(currentTime, timeSpan);
        long rangeOffset = randomInRange(seed, 0, range);
        return exactTime + rangeOffset;
    }

    private void onHandlePowerChanged(boolean connected) {
        powerConnected = connected;
        prefs.setPowerConnected(connected);
        // Remove and re-add alarms to take into account the state change.
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        removeAll(alarmManager);
        for (TimeSync listener : listeners.values()) {
            add(alarmManager, listener);
        }
    }

    private long findOrCreateSeed(TimeSyncPreferences prefs) {
        long seed = prefs.getSeed();
        if (seed != 0) return seed;

        String id = Build.SERIAL + Settings.Secure.ANDROID_ID;

        // TelephonyManger requires permission READ_PHONE_STATE, is this a good idea?
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
