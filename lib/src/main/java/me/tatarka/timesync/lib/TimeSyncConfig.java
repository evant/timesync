package me.tatarka.timesync.lib;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class for configuring how and when a {@link me.tatarka.timesync.lib.TimeSyncListener} will sync.
 */
public final class TimeSyncConfig implements Parcelable {
    public static final long SECONDS = 1000;
    public static final long MINUTES = SECONDS * 60;
    public static final long HOURS = MINUTES * 60;
    public static final long DAYS = HOURS * 24;
    public static final long WEEKS = DAYS * 7;

    protected long timeSpan;
    protected long range = 5 * MINUTES;

    /**
     * Sets how often to sync in milliseconds. The minimum allowed time span is 5 seconds (5000). If
     * you give a value less than this, an exception will be thrown.
     *
     * @param timeSpan the time span in milliseconds
     * @return the config for chaining
     */
    public TimeSyncConfig every(long timeSpan) {
        if (timeSpan < 5 * SECONDS) {
            throw new IllegalArgumentException("Illegal time span of " + timeSpan + " milliseconds, the minimum time span is 5 seconds");
        }
        this.timeSpan = timeSpan;
        return this;
    }

    /**
     * Sets how often to sync. The minimum allowed time span is 5 seconds (5000). If you give a
     * value less than this, an exception will be thrown. This is a convince method that allows you
     * to specify a unit for easy readability.
     *
     * <p> Note that these units are not exact, for example, {@code config.every(2, DAYS)} will sync
     * every 2 * 86400000 milliseconds, which is not necessarily 2 days.</p>
     *
     * @param timeSpan     the time span, in a unit-dependent manner
     * @param timeSpanUnit the unit, can be one of {@link #SECONDS}, {@link #MINUTES}, {@link
     *                     #HOURS}, {@link #DAYS}, {@link #WEEKS}
     * @return the config for chaining
     * @see #every(long)
     */
    public TimeSyncConfig every(long timeSpan, long timeSpanUnit) {
        return every(timeSpan * timeSpanUnit);
    }

    /**
     * Sets a range in which to sync within in milliseconds. The default is 5 minutes (300000).
     *
     * <p> In order not to overload a server with all clients trying to sync at exactly the same
     * time, TimeSync will pick a random offset from the sync interval that is within this range for
     * a given device. For example, if you set this value to 10 minutes, all devices sync within a
     * 10 minute interval, with some syncing up to 5 minutes before and others up to 5 minutes
     * after. </p>
     *
     * @param range the range to sync within in milliseconds
     * @return the config for chaining
     */
    public TimeSyncConfig range(long range) {
        this.range = range;
        return this;
    }

    /**
     * Sets a range in which to sync within in milliseconds. The default is 5 minutes (300000). This
     * is a convince method that allows you to specify a unit for easy readability.
     *
     * @param range
     * @param timeSpanUnit
     * @return
     * @see #range(long)
     */
    public TimeSyncConfig range(long range, long timeSpanUnit) {
        return range(range * timeSpanUnit);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timeSpan);
        dest.writeLong(range);
    }

    public static final Creator<TimeSyncConfig> CREATOR = new Creator<TimeSyncConfig>() {
        @Override
        public TimeSyncConfig createFromParcel(Parcel source) {
            TimeSyncConfig c = new TimeSyncConfig();
            c.timeSpan = source.readLong();
            c.range = source.readLong();
            return c;
        }

        @Override
        public TimeSyncConfig[] newArray(int size) {
            return new TimeSyncConfig[0];
        }
    };
}
