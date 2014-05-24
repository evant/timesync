package me.tatarka.timesync.lib;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class TimeSync {
    public static final String META_DATA_NAME = "me.tatarka.timesync.TimeSync";
    private Config config;

    private boolean onCreateSuperFlag;
    private boolean onDestroySuperFlag;
    private boolean onStartSuperFlag;
    private boolean onStopSuperFlag;

    public static void start(Context context) {
        TimeSyncService.start(context);
    }

    public static TimeSyncListener get(Context context, Class<? extends TimeSync> listener) {
        return new TimeSyncListener(context, listener.getName());
    }

    protected void onCreate(Context context) {
        onCreateSuperFlag = true;
        config = new Config(context, getName());
    }

    protected void onDestroy(Context context) {
        onDestroySuperFlag = true;
    }

    protected void onStart(Context context) {
        onStartSuperFlag = true;
    }

    protected void onStop(Context context) {
        onDestroySuperFlag = true;
    }

    final void ensureOnCreate() {
        if (!onCreateSuperFlag) {
            throw new SuperNotCalledException("TimeSync {" + getClass().getName() + "} did not call through to super.onCreate()");
        }
        onCreateSuperFlag = false;
    }

    final void ensureOnDestroy() {
        if (!onDestroySuperFlag) {
            throw new SuperNotCalledException("TimeSync {" + getClass().getName() + "} did not call through to super.onDestroy()");
        }
        onDestroySuperFlag = false;
    }

    final void ensureOnStart() {
        if (!onStartSuperFlag) {
            throw new SuperNotCalledException("TimeSync {" + getClass().getName() + "} did not call through to super.onStart()");
        }
        onStartSuperFlag = false;
    }

    final void ensureOnStop() {
        if (!onStopSuperFlag) {
            throw new SuperNotCalledException("TimeSync {" + getClass().getName() + "} did not call through to super.onStop()");
        }
        onStopSuperFlag = false;
    }

    public abstract void onSync(Context context) throws Exception;

    public Config config() {
        if (config == null) {
            throw new IllegalStateException("Config has not been initialized. The config is initialized in super.onCreate().");
        }
        return config;
    }

    public final String getName() {
        return getClass().getName();
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

        /**
         * Modify the current configuration. This is persistent and will survive across restarts and
         * installs. Use this if, for example, you want the user to configure the syncing behavior.
         *
         * @return the editor
         * @see #editDefault()
         */
        public Editor edit() {
            return new PrefEditor(name, prefs);
        }

        /**
         * Modify the default configuration. This is not persisted, therefore the best place to call
         * this is in {@link #onCreate(android.content.Context)}. Any modifications to {@link
         * #edit()} will take precedence. This is equivalent to setting the configuration options in
         * xml.
         *
         * @return the editor
         * @see #edit()
         */
        public Editor editDefault() {
            return new DefaultEditor(this);
        }

        /**
         * Modifies the {@link TimeSync.Config} of a {@link TimeSync}, providing a fluent api. You
         * can acquire an editor with {@link #edit()} or {@link #editDefault()}. Make sure you call
         * {@link #save()} to actually apply the changes.
         */
        public interface Editor {
            /**
             * Sets how often to sync in milliseconds. The minimum allowed time span is 5 seconds
             * (5000). If you give a value less than this, an exception will be thrown.
             *
             * @param timeSpan the time span in milliseconds
             * @return the config for chaining
             */
            Editor every(long timeSpan);

            /**
             * Sets how often to sync. The minimum allowed time span is 5 seconds (5000). If you
             * give a value less than this, an exception will be thrown. This is a convince method
             * that allows you to specify a unit for easy readability.
             *
             * <p> Note that these units are not exact, for example, {@code config.every(2, DAYS)}
             * will sync every 2 * 86400000 milliseconds, which is not necessarily 2 days.</p>
             *
             * @param timeSpan     the time span, in a unit-dependent manner
             * @param timeSpanUnit the unit, can be one of {@link #SECONDS}, {@link #MINUTES},
             *                     {@link #HOURS}, {@link #DAYS}, {@link #WEEKS}
             * @return the config for chaining
             * @see #every(long)
             */
            Editor every(long timeSpan, long timeSpanUnit);

            /**
             * Sets a range in which to sync within in milliseconds. The default is 5 minutes
             * (300000).
             *
             * <p> In order not to overload a server with all clients trying to sync at exactly the
             * same time, TimeSync will pick a random offset from the sync interval that is within
             * this range for a given device. For example, if you set this value to 10 minutes, all
             * devices sync within a 10 minute interval, with some syncing up to 5 minutes before
             * and others up to 5 minutes after. </p>
             *
             * @param timeSpan the range to sync within in milliseconds
             * @return the config for chaining
             */
            Editor range(long timeSpan);

            /**
             * Sets a range in which to sync within in milliseconds. The default is 5 minutes
             * (300000). This is a convince method that allows you to specify a unit for easy
             * readability.
             *
             * @param timeSpan     the range to sync within in milliseconds
             * @param timeSpanUnit the unit, can be one of {@link #SECONDS}, {@link #MINUTES},
             *                     {@link #HOURS}, {@link #DAYS}, {@link #WEEKS}
             * @return the config for chaining
             * @see #range(long)
             */
            Editor range(long timeSpan, long timeSpanUnit);

            Editor enable(boolean value);

            Editor enable();

            Editor disable();

            /**
             * Applies all changes from the editor to the config. You must call this to ensure your
             * changes are applied.
             */
            void save();
        }

        private static class PrefEditor implements Editor {
            private String name;
            private SharedPreferences.Editor editor;

            private PrefEditor(String name, SharedPreferences prefs) {
                this.name = name;
                editor = prefs.edit();
            }

            @Override
            public Editor every(long timeSpan) {
                if (timeSpan < 5 * SECONDS) {
                    throw new IllegalArgumentException("Illegal time span of " + timeSpan + " milliseconds, the minimum time span is 5 seconds");
                }
                editor.putLong(name + CONFIG_EVERY, timeSpan);
                return this;
            }

            @Override
            public Editor every(long timeSpan, long timeSpanUnit) {
                return every(timeSpan * timeSpanUnit);
            }

            @Override
            public Editor range(long range) {
                editor.putLong(name + CONFIG_RANGE, range);
                return this;
            }

            @Override
            public Editor range(long range, long timeSpanUnit) {
                return range(range * timeSpanUnit);
            }

            @Override
            public Editor enable(boolean value) {
                editor.putBoolean(name + CONFIG_ENABLED, value);
                return this;
            }

            @Override
            public Editor enable() {
                return enable(true);
            }

            @Override
            public Editor disable() {
                return enable(false);
            }

            @Override
            public void save() {
                editor.commit();
            }
        }

        private static class DefaultEditor implements Editor {
            private Config config;

            private boolean enabled = DEFAULT_ENABLED;
            private long every = DEFAULT_EVERY;
            private long range = DEFAULT_RANGE;

            private DefaultEditor(Config config) {
                this.config = config;
            }

            @Override
            public Editor every(long timeSpan) {
                every = timeSpan;
                return this;
            }

            @Override
            public Editor every(long timeSpan, long timeSpanUnit) {
                every(timeSpan * timeSpanUnit);
                return this;
            }

            @Override
            public Editor range(long timeSpan) {
                range = timeSpan;
                return this;
            }

            @Override
            public Editor range(long timeSpan, long timeSpanUnit) {
                range(timeSpan * timeSpanUnit);
                return this;
            }

            @Override
            public Editor enable(boolean value) {
                enabled = value;
                return this;
            }

            @Override
            public Editor enable() {
                enable(true);
                return this;
            }

            @Override
            public Editor disable() {
                enable(false);
                return this;
            }

            @Override
            public void save() {
                config.defaultEnabled = enabled;
                config.defaultEvery = every;
                config.defaultRange = range;
            }
        }
    }
}
