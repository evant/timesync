package me.tatarka.timesync.lib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class TimeSyncParser {
    private static Map<String, TimeSync> sListeners;

    static Map<String, TimeSync> parseListeners(Context context) {
        if (sListeners != null) {
            return sListeners;
        }

        sListeners = new HashMap<>();

        try {
            XmlPullParser parser = context.getResources().getXml(getResource(context));
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("listener")) {
                        String className = parser.getAttributeValue(null, "name");
                        if (className == null) {
                            throw new IllegalArgumentException("Your <listener/> must have the attribute android:name=\"[CLASS_NAME]\"");
                        }
                        if (className.startsWith(".")) {
                            className = context.getPackageName() + className;
                        }

                        TimeSync listener = createListener(className);
                        sListeners.put(listener.getName(), listener);
                    }
                }
                parser.next();
            }
            return sListeners;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (XmlPullParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static TimeSync createListener(String name) throws IllegalArgumentException {
        try {
            return (TimeSync) Class.forName(name).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid TimeSync {" + name + "}", e);
        }
    }

    private static int getResource(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            int res = appInfo.metaData.getInt(TimeSync.META_DATA_NAME);
            if (res == 0) {
                throw new IllegalArgumentException("You must declare <meta-data android:name=\"" + TimeSync.META_DATA_NAME + "\" android:resource=\"@xml/[RESOURCE_NAME]\"/> in your AndroidManifest.xml");
            }
            return res;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
