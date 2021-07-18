package com.deniz.elfix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent arg) {
        new AppLock(context);

        Intent intent = new Intent(context, ForegroundService.class);
        context.startService(intent);
    }
}
