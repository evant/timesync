package me.tatarka.timesync.lib;

import android.os.Parcel;
import android.os.Parcelable;

public final class TimeSyncConfig implements Parcelable {
    public static final long SECONDS = 1000;
    public static final long MINUTES = SECONDS * 60;
    public static final long HOURS = MINUTES * 60;
    public static final long DAYS = HOURS * 24;
    public static final long WEEKS = DAYS * 7;

    /**
     * Inner class makes properties visible to the service but not the client.
     */
    class Properties {
        long timeSpan;
        long jitter = 5 * MINUTES;
    }

    protected final Properties p = new Properties();

    public TimeSyncConfig every(long timeSpanMills) {
        if (timeSpanMills < 5 * SECONDS) {
            throw new IllegalArgumentException("Illegal time span of " + timeSpanMills + " milliseconds, the minimum time span is 5 seconds");
        }
        p.timeSpan = timeSpanMills;
        return this;
    }

    public TimeSyncConfig every(long timeSpan, long timeSpanMult) {
        return every(timeSpan * timeSpanMult);
    }

    public TimeSyncConfig jitter(long jitterMills) {
        p.jitter = jitterMills;
        return this;
    }

    public TimeSyncConfig jitter(long jitter, long timeSpanMult) {
        return jitter(jitter * timeSpanMult);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(p.timeSpan);
        dest.writeLong(p.jitter);
    }

    public static final Creator<TimeSyncConfig> CREATOR = new Creator<TimeSyncConfig>() {
        @Override
        public TimeSyncConfig createFromParcel(Parcel source) {
            TimeSyncConfig c = new TimeSyncConfig();
            c.p.timeSpan = source.readLong();
            c.p.jitter = source.readLong();
            return c;
        }

        @Override
        public TimeSyncConfig[] newArray(int size) {
            return new TimeSyncConfig[0];
        }
    };
}
