package me.tatarka.timesync.lib;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

public class TimeSyncService extends IntentService {
    public static final String META_DATA_NAME = "me.tatarka.timesync.TimeSync";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final String SHARED_PREFS = "me.tatarka.timesync.SHARED_PREFS";
    private static final String PREFS_SEED = "seed";

    private static final String TYPE = "type";
    private static final int TYPE_START = 0;
    private static final int TYPE_SYNC = 1;
    private static final int TYPE_NETWORK_BACK = 2;

    public static final String NAME = "name";

    private int seed;
    private Random random;
    private Map<String, TimeSyncListener> listeners = new HashMap<>();

    public TimeSyncService() {
        super(TimeSyncService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        seed = findOrCreateSeed();
        random = new Random(seed);

        List<String> names = readXmlResource();
        for (String name : names) {
            try {
                listeners.put(name, (TimeSyncListener) Class.forName(name).newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalArgumentException("Listener " + name + " must be public and have an empty constructor");
            }
        }
    }

    private List<String> readXmlResource() {
        List<String> names = new ArrayList<>();

        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            int res = appInfo.metaData.getInt(META_DATA_NAME);
            if (res == 0) {
                throw new IllegalArgumentException("You must declare <meta-data android:name=\"" + META_DATA_NAME + "\" android:resource=\"@xml/[RESOURCE_NAME]\"/> in your AndroidManifest.xml");
            }
            XmlPullParser parser = getResources().getXml(res);
            while (parser.getEventType()!=XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType()==XmlPullParser.START_TAG) {
                    if (parser.getName().equals("listener")) {
                        String name = parser.getAttributeValue(ANDROID_NS, "name");
                        if (name == null) {
                            throw new IllegalArgumentException("Your <listener/> must have the attribute android:name=\"[CLASS_NAME]\"");
                        }
                        if (name.startsWith(".")) {
                            name = getPackageName() + name;
                        }
                        names.add(name);
                    }
                }
                parser.next();
            }
            return names;
        } catch (PackageManager.NameNotFoundException | IOException e) {
            throw new IllegalStateException(e);
        } catch (XmlPullParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void start(Context context) {
        context.startService(getStartIntent(context));
    }

    public static void sync(Context context, String name) {
        context.startService(getSyncIntent(context, name));
    }

    public static void networkBack(Context context) {
        context.startService(getNetworkBackIntent(context));
    }

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_START);
        return intent;
    }

    public static Intent getSyncIntent(Context context, String name) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.setData(Uri.parse("timesync://" + name));
        intent.putExtra(TYPE, TYPE_SYNC);
        intent.putExtra(NAME, name);
        return intent;
    }

    public static Intent getNetworkBackIntent(Context context) {
        Intent intent = new Intent(context, TimeSyncService.class);
        intent.putExtra(TYPE, TYPE_NETWORK_BACK);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getIntExtra(TYPE, 0)) {
            case TYPE_START: {
                for (TimeSyncListener listener : listeners.values()) {
                    onHandleAdd(listener);
                }
            }
            case TYPE_SYNC: {
                String name = intent.getStringExtra(NAME);
                TimeSyncListener listener = listeners.get(name);
                if (listener != null) {
                    onHandleSync(listener);
                }
            }
            case TYPE_NETWORK_BACK: {
                for (TimeSyncListener listener : listeners.values()) {
                    onHandleAdd(listener);
                }
            }
        }
    }

    private void onHandleAdd(TimeSyncListener listener) {
        long time = calculateTime(listener);

        if (time > 0) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = getSyncIntent(this, listener.getClass().getName());
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager.cancel(pendingIntent);
            alarmManager.set(AlarmManager.RTC, time, pendingIntent);
        }
    }

    private void onHandleSync(TimeSyncListener listener) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            try {
                listener.onSync(this);
                onHandleAdd(listener);
            } catch (Exception e) {
                onHandleFailureSyncError(listener);
            }
        } else {
            onHandleFailureNoNetwork(listener);
        }
    }

    private void onHandleFailureNoNetwork(TimeSyncListener listener) {
        // Stop all alarms
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (TimeSyncListener l : listeners.values()) {
            Intent intent = getSyncIntent(this, l.getClass().getName());
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager.cancel(pendingIntent);
        }

        // Start listening for when network is back up.
        ComponentName receiver = new ComponentName(this, TimeSyncNetworkReceiver.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(receiver, COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL_APP);
    }

    private void onHandleFailureSyncError(TimeSyncListener listener) {

    }

    private long calculateTime(TimeSyncListener listener) {
        TimeSyncConfig config = listener.getConfig();
        return System.currentTimeMillis() + config.p.timeSpan + jitter(config);
    }

    private long jitter(TimeSyncConfig config) {
        return nextLong(random, 2 * config.p.jitter) - config.p.jitter;
    }

    /**
     * source: http://stackoverflow.com/a/2546186
     */
    private static long nextLong(Random rng, long n) {
        long bits, val;
        do {
            bits = (rng.nextLong() << 1) >>> 1;
            val = bits % n;
        } while (bits-val+(n-1) < 0L);
        return val;
    }

    private int findOrCreateSeed() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int seed = prefs.getInt(PREFS_SEED, 0);
        if (seed != 0) return seed;

        String id = Build.SERIAL + Settings.Secure.ANDROID_ID;

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            String deviceId = telephonyManager.getDeviceId();
            if (deviceId != null) id += deviceId;
        }

        seed = id.hashCode();
        prefs.edit().putInt(PREFS_SEED, seed).commit();

        return seed;
    }
}
