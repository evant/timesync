package me.tatarka.timesync.lib;

import android.content.Context;

import java.util.Arrays;

/**
 * A class for interacting with a {@link TimeSync}. You can get and set it's configuration, and
 * force it to sync immediately. Ta get an instance of the class for a given {@link TimeSync}, use
 * {@link TimeSync#get(android.content.Context, Class)}.
 */
public final class TimeSyncProxy {
    private Context context;
    private String name;
    private TimeSync listener;

    TimeSyncProxy(Context context, String name) {
        this.context = context;
        this.name = name;
        listener = TimeSyncParser.parseListeners(context).get(name);
    }

    /**
     * Syncs immediately. This is useful for a response to a user action. Use this sparingly, as
     * frequent syncs defeat the purpose of using this library.
     */
    public void sync() {
        TimeSyncService.sync(context, name);
    }

    /**
     * Syncs sometime in the near future, randomizing per device. This is useful in response to a
     * server message, using GCM for example, so that the server is not overwhelmed with all devices
     * trying to sync at once.
     */
    public void syncInexact() {
        TimeSyncService.syncInexact(context, name);
    }

    /**
     * Gets the current configuration for the {@link TimeSync}.
     *
     * @return the configuration
     * @see TimeSync.Config
     */
    public TimeSync.Config config() {
        return listener.config();
    }

    /**
     * Modifies the current configuration for the {@link TimeSync}.
     *
     * @param edits the edits
     * @see TimeSync#edit(TimeSync.Edit...)
     */
    public void edit(Iterable<TimeSync.Edit> edits) {
        listener.edit(edits);
        TimeSyncService.update(context, name);
    }

    /**
     * Modifies the current configuration for the {@link TimeSync}.
     *
     * @param edits the edits
     * @see TimeSync#edit(TimeSync.Edit...)
     */
    public void edit(TimeSync.Edit... edits) {
        edit(Arrays.asList(edits));
    }
}
