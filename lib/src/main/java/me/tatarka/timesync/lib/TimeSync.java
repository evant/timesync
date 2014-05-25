package me.tatarka.timesync.lib;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;

public abstract class TimeSync {
    public static final String META_DATA_NAME = "me.tatarka.timesync.TimeSync";
    private Config config;

    private boolean onCreateSuperFlag;

    /**
     * Starts the {@code TimeSync} service. This is necessary for all periodic syncing to occur. The
     * best place to call this is in your {@link android.app.Application} subclass's {@link
     * android.app.Application#onCreate()} method.
     *
     * @param context the context
     */
    public static void start(Context context) {
        TimeSyncService.start(context);
    }

    /**
     * Returns a proxy to a {@code TimeSync} to be able to query and modify it outside it's own
     * context. You should always use this instead of constructing the {@code TimeSync} directly.
     *
     * @param context  the context
     * @param listener the class of the {@code TimeSync} to proxy
     * @return the proxy
     */
    public static TimeSyncProxy get(Context context, Class<? extends TimeSync> listener) {
        return new TimeSyncProxy(context, listener.getName());
    }

    /**
     * Called when the {@code TimeSync} is created. This may be only called once across several
     * syncs depending on how long your app stays in memory.
     *
     * @param context the context
     */
    protected void onCreate(Context context) {
        onCreateSuperFlag = true;
        config = new Config(context, getName());
    }

    final void ensureOnCreate() {
        if (!onCreateSuperFlag) {
            throw new SuperNotCalledException("TimeSync {" + getClass().getName() + "} did not call through to super.onCreate()");
        }
        onCreateSuperFlag = false;
    }

    /**
     * Called every time the system decides to sync. This is always called on a separate thread. You
     * are also guaranteed to have a network connection at this point. If sync fails, you should
     * throw an exception to notify {@code TimeSync} to retry properly.
     *
     * @param context the context
     * @throws Exception throw to notify of a sync failure
     */
    public abstract void onSync(Context context) throws Exception;

    /**
     * Returns the current configuration of the {@code TimeSync}. This is not valid before {@link
     * #onCreate(android.content.Context)}. The returned configuration is read-only. To edit, use
     * {@link #edit(TimeSync.Edit...)} instead.
     *
     * @return the configuration
     * @see TimeSync.Config
     */
    public Config config() {
        if (config == null) {
            throw new IllegalStateException("Config has not been initialized. The config is initialized in super.onCreate().");
        }
        return config;
    }

    /**
     * Returns the name of the {@code TimeSync}. This is currently {@code getClass().getName()}.
     *
     * @return the name
     */
    public final String getName() {
        return getClass().getName();
    }

    /**
     * Modify the current configuration. This is persistent and will survive across restarts and
     * installs. Use this if, for example, you want the user to configure the syncing behavior.
     *
     * @see #editDefault(TimeSync.Edit...)
     */
    public void edit(Edit... edits) {
        edit(Arrays.asList(edits));
    }

    /**
     * Modify the current configuration. This is persistent and will survive across restarts and
     * installs. Use this if, for example, you want the user to configure the syncing behavior.
     *
     * @see #editDefault(TimeSync.Edit...)
     */
    public void edit(Iterable<Edit> edits) {
        if (config == null) {
            throw new IllegalStateException("Config has not been initialized. The config is initialized in super.onCreate().");
        }

        SharedPreferences.Editor editor = config.prefs.edit();
        for (Edit edit : edits) {
            switch (edit.type) {
                case ENABLED:
                    editor.putBoolean(config.name + Config.CONFIG_ENABLED, (boolean) edit.value);
                    break;
                case EVERY:
                    editor.putLong(config.name + Config.CONFIG_EVERY, (long) edit.value);
                    break;
                case RANGE:
                    editor.putLong(config.name + Config.CONFIG_RANGE, (long) edit.value);
                    break;
            }
        }
        editor.commit();
    }

    /**
     * Modify the default configuration. This is not persisted, therefore the best place to call
     * this is in {@link #onCreate(android.content.Context)}. Any modifications to {@link
     * #edit(TimeSync.Edit...)} will take precedence. This is equivalent to setting the
     * configuration options in xml.
     *
     * @see #edit(TimeSync.Edit...)
     */
    public void editDefault(Edit... edits) {
        editDefault(Arrays.asList(edits));
    }

    /**
     * Modify the default configuration. This is not persisted, therefore the best place to call
     * this is in {@link #onCreate(android.content.Context)}. Any modifications to {@link
     * #edit(TimeSync.Edit...)} will take precedence. This is equivalent to setting the
     * configuration options in xml.
     *
     * @see #edit(TimeSync.Edit...)
     */
    public void editDefault(Iterable<Edit> edits) {
        for (Edit edit : edits) {
            switch (edit.type) {
                case ENABLED:
                    config.defaultEnabled = (boolean) edit.value;
                    break;
                case EVERY:
                    config.defaultEvery = (long) edit.value;
                    break;
                case RANGE:
                    config.defaultRange = (long) edit.value;
                    break;
            }
        }
    }

    /**
     * Class for configuring how and when a {@link TimeSync} will sync.
     */
    public static final class Config {
        public static final long SECONDS = 1000;
        public static final long MINUTES = SECONDS * 60;
        public static final long HOURS = MINUTES * 60;
        public static final long DAYS = HOURS * 24;
        public static final long WEEKS = DAYS * 7;

        public static final boolean DEFAULT_ENABLED = true;
        public static final long DEFAULT_EVERY = 0;
        public static final long DEFAULT_RANGE = 5 * MINUTES;

        private static final String CONFIG_ENABLED = "config_enabled";
        private static final String CONFIG_EVERY = "config_every";
        private static final String CONFIG_RANGE = "config_range";

        private String name;
        private boolean defaultEnabled = DEFAULT_ENABLED;
        private long defaultEvery = DEFAULT_EVERY;
        private long defaultRange = DEFAULT_RANGE;
        private SharedPreferences prefs;

        private Config(Context context, String name) {
            this.name = name;
            prefs = context.getSharedPreferences(TimeSyncPreferences.NAME, Context.MODE_PRIVATE);
        }

        public boolean enabled() {
            return prefs.getBoolean(name + CONFIG_ENABLED, defaultEnabled);
        }

        public long every() {
            return prefs.getLong(name + CONFIG_EVERY, defaultEvery);
        }

        public long range() {
            return prefs.getLong(name + CONFIG_RANGE, defaultRange);
        }
    }

    /**
     * Class for modifying the {@link TimeSync} configuration.
     */
    public static class Edit {
        private static enum Type {
            ENABLED, EVERY, RANGE
        }

        private Type type;
        private Object value;

        private Edit(Type type, Object value) {
            this.type = type;
            this.value = value;
        }

        /**
         * Sets if the {@link TimeSync} is enabled. If not, it will not sync, even when called
         * explicitly.
         *
         * @param value true to enable, false to disable
         * @return the edit for chaining
         */
        public static Edit enable(boolean value) {
            return new Edit(Type.ENABLED, value);
        }

        /**
         * Enables the {@link TimeSync}.
         *
         * @return the edit for chaining
         * @see #enable(boolean)
         */
        public static Edit enable() {
            return enable(true);
        }

        /**
         * Disables the {@link TimeSync}.
         *
         * @return the edit for chaining
         * @see #enable(boolean)
         */
        public static Edit disable() {
            return enable(false);
        }

        /**
         * Sets how often to sync in milliseconds. The minimum allowed time span is 5 seconds
         * (5000). If you give a value less than this, an exception will be thrown.
         *
         * @param timeSpan the time span in milliseconds
         * @return the edit for chaining
         */
        public static Edit every(long timeSpan) {
            return new Edit(Type.EVERY, timeSpan);
        }

        /**
         * Sets how often to sync. The minimum allowed time span is 5 seconds (5000). If you give a
         * value less than this, an exception will be thrown. This is a convince method that allows
         * you to specify a unit for easy readability.
         *
         * <p> Note that these units are not exact, for example, {@code config.every(2, DAYS)} will
         * sync every 2 * 86400000 milliseconds, which is not necessarily 2 days.</p>
         *
         * @param timeSpan     the time span, in a unit-dependent manner
         * @param timeSpanUnit the unit, can be one of {@link Config#SECONDS}, {@link
         *                     Config#MINUTES}, {@link Config#HOURS}, {@link Config#DAYS}, {@link
         *                     Config#WEEKS}
         * @return the edit for chaining
         * @see #every(long)
         */
        public static Edit every(long timeSpan, long timeSpanUnit) {
            return every(timeSpan * timeSpanUnit);
        }

        /**
         * Sets a range in which to sync within in milliseconds. The default is 5 minutes (300000).
         *
         * <p> In order not to overload a server with all clients trying to sync at exactly the same
         * time, TimeSync will pick a random offset from the sync interval that is within this range
         * for a given device. For example, if you set this value to 10 minutes, some devices may
         * sync up to 10 minutes after the regularly scheduled time.
         * minutes after. </p>
         *
         * @param timeSpan the range to sync within in milliseconds
         * @return the edit for chaining
         */
        public static Edit range(long timeSpan) {
            return new Edit(Type.RANGE, timeSpan);
        }

        /**
         * Sets a range in which to sync within in milliseconds. The default is 5 minutes (300000).
         * This is a convince method that allows you to specify a unit for easy readability.
         *
         * @param timeSpan     the range to sync within in milliseconds
         * @param timeSpanUnit the unit, can be one of {@link Config#SECONDS}, {@link
         *                     Config#MINUTES}, {@link Config#HOURS}, {@link Config#DAYS}, {@link
         *                     Config#WEEKS}
         * @return the edit for chaining
         * @see #range(long)
         */
        public static Edit range(long timeSpan, long timeSpanUnit) {
            return new Edit(Type.RANGE, timeSpan * timeSpanUnit);
        }
    }
}
