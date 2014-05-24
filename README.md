TimeSync
========

Android library for periodicly syncing data to and from a server.

This library is an alternative to a SyncAdapter. It is easier to set up, and doesn't require you to worry about accounts or content providers.

#### Features
- Periodicly sync on a given interval in a battery-conscience way.
- Turn off syncing when their is no network connection.
- Randomly offset syncs so that they don't all hit the server at once.
- Sync accross apps using this library at the same time to reduce wakeups.

## Usage
First create `MySync.java`. This will hold the logic of how your are syncing.

```java
public class MySync extends TimeSync {
  @Override
  public void onCreate(Context context) {
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

Then create `timesync.xml` in `res/raw`. This is where you put configuation for how and when syncs happen. You can define as many TimeSync classes as you want in here.

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

Finnaly, make sure you start TimeSync in your Application subclass.

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

Your sync class will automatically run based on it's configuration. If you want to start it manualy, you can do so as well. Note that this will be ignored if your sync class is disabled.

```java
  TimeSyncProxy mySync = TimeSync.get(context, MySync.class);
  mySync.sync();
```
