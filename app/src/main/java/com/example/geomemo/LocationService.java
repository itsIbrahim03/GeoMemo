package com.example.geomemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationService extends Service implements LocationListener {

    private static final String CHANNEL_ID_SERVICE = "location_service";
    private static final String CHANNEL_ID_PROXIMITY = "proximity_alerts";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final long MIN_TIME_MS = 10000;
    private static final float MIN_DISTANCE_M = 10f;

    private LocationManager locationManager;
    private MemoDBHelper dbHelper;
    private final Set<Long> notifiedMemoIds = new HashSet<>();
    private Thread proximityThread;
    private volatile Location latestLocation;
    private volatile boolean running;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        dbHelper = new MemoDBHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildForegroundNotification();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
            }
        } catch (SecurityException e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        running = true;
        proximityThread = new Thread(this::proximityCheckLoop);
        proximityThread.start();

        return START_STICKY;
    }

    private void proximityCheckLoop() {
        while (running) {
            Location location = latestLocation;
            if (location != null) {
                checkProximity(location);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void checkProximity(Location currentLocation) {
        SharedPreferences prefs = getSharedPreferences("geomemo_prefs", MODE_PRIVATE);
        int detectionRadius = prefs.getInt("detection_radius", 100);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

        if (!notificationsEnabled) {
            return;
        }

        List<Memo> memos = dbHelper.getAllMemos();
        Set<Long> currentlyNearby = new HashSet<>();
        float[] results = new float[1];

        for (Memo memo : memos) {
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    memo.getLatitude(),
                    memo.getLongitude(),
                    results
            );
            float distance = results[0];

            if (distance < detectionRadius) {
                currentlyNearby.add(memo.getId());
                if (!notifiedMemoIds.contains(memo.getId())) {
                    sendProximityNotification(memo);
                    notifiedMemoIds.add(memo.getId());
                }
            }
        }

        // Reset notified IDs for memos the user has moved away from
        notifiedMemoIds.retainAll(currentlyNearby);
    }

    private void sendProximityNotification(Memo memo) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) memo.getId(), intent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_PROXIMITY)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_proximity_title))
                .setContentText("You are near: " + memo.getTitle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify((int) (1000 + memo.getId()), notification);
    }

    private Notification buildForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.service_notification_title))
                .setContentText(getString(R.string.service_notification_text))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(serviceChannel);

        NotificationChannel proximityChannel = new NotificationChannel(
                CHANNEL_ID_PROXIMITY,
                getString(R.string.notification_channel_proximity),
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(proximityChannel);
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isBetterLocation(location, latestLocation)) {
            latestLocation = location;
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // No action needed
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // No action needed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (proximityThread != null) {
            proximityThread.interrupt();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
