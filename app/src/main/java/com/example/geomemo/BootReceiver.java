package com.example.geomemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("geomemo_prefs", Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
            if (notificationsEnabled) {
                try {
                    Intent serviceIntent = new Intent(context, LocationService.class);
                    context.startForegroundService(serviceIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
