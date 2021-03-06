package com.deniz.elfix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ForegroundService extends Service {

    public final int NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_ACTION = "NOT_ACTION";
    public final String CHANNEL_ID = "ELFIX";
    public static String PACKAGE_NAME = "com.deniz.elfix";

    private ScreenReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock service_wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "elfix:wakelock:service_persistent");
            service_wl.acquire();
        }

        createNotificationChannel();
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter(NOTIFICATION_ACTION));

        return START_STICKY;
    }

    private BroadcastReceiver onNotice = new BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isDND = false;
            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    isDND = notificationManager.getCurrentInterruptionFilter()
                            != NotificationManager.INTERRUPTION_FILTER_ALL;
                }
            }
            SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, MODE_PRIVATE);
            boolean screen = prefs.getBoolean("SCREEN_ON", false);
            boolean wake_screen = prefs.getBoolean("WAKE_SCREEN", false);
            if ((!ScreenReceiver.wasScreenOn || screen) && !isDND) {
                String pack = intent.getStringExtra("package");
                String title = intent.getStringExtra("title");
                String text = intent.getStringExtra("text");
                int color = intent.getIntExtra("color", 0);
                PendingIntent contentIntent = intent.getParcelableExtra("contentIntent");

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl;
                if (pm != null && wake_screen) {
                    wl = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "elfix:wakelock");
                    wl.acquire(2000);
                }

                String mDrawableName = null;
                if (pack != null) {
                    mDrawableName = "ic_stat_" + pack.replace(".", "_");
                }
                int resID;
                try {
                    resID = getResources().getIdentifier(mDrawableName, "drawable", getPackageName());
                } catch (Exception e) {
                    resID = R.drawable.dot;
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(resID)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(contentIntent);

                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

                new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                notificationManager.cancel(NOTIFICATION_ID);
                            }
                        }, 100);
            }
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "Edge Lighting Helper";
        String description = "Helper channel with Edge Lighting support for selected app(s) in EL Fix app.";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //When remove app from background then start it again
        super.onTaskRemoved(rootIntent);

        PendingIntent service = PendingIntent.getService(
                getApplicationContext(),
                1,
                new Intent(getApplicationContext(), ForegroundService.class),
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, service);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}