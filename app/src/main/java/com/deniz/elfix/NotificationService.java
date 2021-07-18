package com.deniz.elfix;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class NotificationService extends NotificationListenerService {

    private ArrayList<StatusBarNotification> notificationBatch;

    public NotificationService() {
        notificationBatch = new ArrayList<>();
    }

    private boolean OREO_FIX_BETA = false;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();
        if (!sbn.isOngoing() && sbn.isClearable()) {
            if (AppLock.isLocked()) {
                if (pack.equals(AppLock.LOCK_PACKAGE_NAME)) {
                    process(sbn);
                }
            } else {
                if (isPackAvailable(pack))
                    process(sbn);
            }
        }
    }

    private StatusBarNotification prev;
    private void process(StatusBarNotification sbn) {
        try {
            if (OREO_FIX_BETA && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (prev != null && sbn.getKey().equals(prev.getKey())) {
                    prev = null;
                    return;
                }
                if (sbn.getPackageName().equals("com.whatsapp")) {
                    if (sbn.getTag() == null) {
                        if (notificationBatch.size() > 0) {
                            prev = notificationBatch.get(notificationBatch.size() - 1);
                        }
                    } else if (sbn.getTag().contains("@")) {
                        notificationBatch.add(sbn);
                    }
                } else {
                    prev = sbn;
                }

                if (prev != null) {
                    this.snoozeNotification(prev.getKey(), 1000);
                    System.out.println("SNOOZED");
                }
            } else {
                process(sbn.getPackageName(), sbn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (OREO_FIX_BETA && !ScreenReceiver.wasScreenOn && sbn.getKey().equals(prev.getKey())) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl;
            if (pm != null) {
                wl = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "elfix:wakelock");
                wl.acquire(2000);
            }
        }
    }

    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    private int getColorFromPackage(String pack) throws PackageManager.NameNotFoundException {
        PackageManager pm = getPackageManager();
        Drawable applicationIcon = pm.getApplicationIcon(pack);
        Bitmap newBitmap = Bitmap.createScaledBitmap(getBitmapFromDrawable(applicationIcon), 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    PendingIntent contentIntent;
    private void process(String pack, StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE);
            String text = extras.getCharSequence(Notification.EXTRA_TEXT).toString();

            if (pack.equals("com.whatsapp")) {
                if (sbn.getTag() == null) {
                    if (notificationBatch.size() > 0) post();
                } else {
                    contentIntent = sbn.getNotification().contentIntent;
                    notificationBatch.add(sbn);
                }
            } else {
                contentIntent = sbn.getNotification().contentIntent;
                post(pack, title, text, getColorFromPackage(pack));
            }
        } catch (NullPointerException | PackageManager.NameNotFoundException ignored) {
        }
    }

    private void post() throws PackageManager.NameNotFoundException {
        StatusBarNotification sbn = notificationBatch.get(notificationBatch.size() - 1);
        Bundle extras = sbn.getNotification().extras;
        String pack = sbn.getPackageName();
        String title = extras.getString(Notification.EXTRA_TITLE);
        String text = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        post(pack, title, text, getColorFromPackage(pack));
        notificationBatch.clear();
    }

    private void post(String pack, String title, String text, int color) {
        Intent notiRCV = new Intent(ForegroundService.NOTIFICATION_ACTION);
        notiRCV.putExtra("package", pack);
        notiRCV.putExtra("title", title);
        notiRCV.putExtra("text", text);
        notiRCV.putExtra("color", color);
        notiRCV.putExtra("contentIntent", contentIntent);

        LocalBroadcastManager.getInstance(this).sendBroadcast(notiRCV);
    }

    private boolean isPackAvailable(String pack) {
        try {
            for (String p : AppLock.AVAILABLE_PACKAGE_NAMES) {
                if (p.equals(pack)) return true;
            }
        } catch (NullPointerException ignored) {
        }
        return false;
    }

    public static boolean checkNotificationEnabled(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners")
                .contains(context.getPackageName());
    }
}

