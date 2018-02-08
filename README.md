TimeSync
========

Android library for periodically syncing data to and from a server.

This library is an alternative to a SyncAdapter. It is easier to set up, and doesn't require you to worry about accounts or content providers.

Note: this library uses permissions `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, and `RECEIVE_BOOT_COMPLETED`.

#### Features
- Periodically sync on a given interval in a battery-conscious way.
- Turn off syncing when there is no network connection.
- Randomly offset syncs so that they don't all hit the server at once.
- Sync across apps using this library at the same time to reduce wakeups.
- Retry on sync failure.

## Usage
First create `MySync.java`. This will hold the logic of how your are syncing.

```java
public class MySync extends TimeSync {
  @Override
  protected void onCreate(Context context) {
    super.onCreate(context);
    // This is called whenever MySync is created.
  }
  
  @Override
  public void onSync(Context context) throws Exception {
    // Implement you sync logic here. This will run on a
    // seperate thread and network is guaranteed to be available.
    // An exception represents a sync failure.
  }
}
```

Then create `timesync.xml` in `res/xml`. This is where you put configuration for how and when syncs happen. You can define as many TimeSync classes as you want in here.

```xml
<?xml version="1.0" encoding="utf-8"?>

<timesync>
    <listener name=".MySync" every="1 day"/>
</timesync>
```

Then add a `meta-data` attribute to your `AndroidManifest`.

```xml
<application>
  ...
  <meta-data android:name="me.tatarka.timesync.TimeSync" android:resource="@xml/timesync"/>
</application>
```

Finally, make sure you start TimeSync in your Application subclass.

```java
public class MyApplication extends Application {
  @Override
  public void onCreate(Context context) {
    super.onCreate(context);
    TimeSync.start(context);
  }
}
```

## Running

Your sync class will automatically run based on it's configuration. If you want to start it manually, you can do so as well. Note that this will be ignored if your sync class is disabled.

```java
  TimeSyncProxy mySync = TimeSync.get(context, MySync.class);
  mySync.sync();
```

When running as a response to some event, a GCM message for example, you may want to ensure all devices don't hit your server at exactly the same time. In this case use

```java
  mySync.syncInexact();
```

## Configuration

Configuration can either take place in xml, or at runtime, the second useful if you want to provide some user control.

For xml use

```xml
  <listener name=".MySync" config_option="value"/>
```

- **enabled="true|false"** If the TimeSync is even enabled. If not, both periodic and explicit syncs will not be run.
- **every="10 [second(s)|minute(s)|hour(s)|day(s)|year(s)]"** How often to sync periodically. If no unit is provided, it will be assumed milliseconds. The default is 0, which disables periodic syncing.
- **range="5 [second(s)|minute(s)|hour(s)|day(s)|year(s)]"** The range of the random offset added to syncs so that they don't hit the server at exactly the same time. A sync will occur up to the given value after regularly scheduled. The default is 5 minutes. This is also used for `TimeSync.syncInexact()`.

In code, use `TimeSyncProxy.edit(...)`. Setting values this way will override the xml config and be persisted across updates.

### Proguard

```
  -keep class * extends me.tatarka.timesync.lib.TimeSync { *; }
```











