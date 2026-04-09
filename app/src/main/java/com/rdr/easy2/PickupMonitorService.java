package com.rdr.easy2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PickupMonitorService extends Service implements SensorEventListener {
    public static final String ACTION_START = "com.rdr.easy2.action.START_PICKUP_MONITOR";
    public static final String ACTION_STOP = "com.rdr.easy2.action.STOP_PICKUP_MONITOR";

    private static final String CHANNEL_ID = "pickup_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long WAKE_COOLDOWN_MS = 8000L;
    private static final long WAKE_LOCK_TIMEOUT_MS = 3000L;
    private static final float MOTION_DELTA_THRESHOLD = 1.6f;
    private static final float TILT_DELTA_THRESHOLD = 2.4f;

    private SensorManager sensorManager;
    private PowerManager powerManager;
    private Sensor accelerometerSensor;
    private Sensor significantMotionSensor;
    private boolean accelerometerRegistered;
    private boolean significantMotionArmed;
    private float lastMagnitude;
    private float lastZ;
    private long lastWakeTimestamp;
    private boolean hasSensorSample;

    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMonitoringState();
        }
    };

    private final TriggerEventListener significantMotionListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            significantMotionArmed = false;
            handlePickupDetected();
            updateMonitoringState();
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, PickupMonitorService.class);
        intent.setAction(ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            significantMotionSensor =
                    sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        }

        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenStateReceiver, screenFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        updateMonitoringState();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenStateReceiver);
        unregisterSensors();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 3 || isScreenOn()) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt((x * x) + (y * y) + (z * z));

        if (!hasSensorSample) {
            hasSensorSample = true;
            lastMagnitude = magnitude;
            lastZ = z;
            return;
        }

        float motionDelta = Math.abs(magnitude - lastMagnitude);
        float tiltDelta = Math.abs(z - lastZ);
        lastMagnitude = magnitude;
        lastZ = z;

        if (motionDelta >= MOTION_DELTA_THRESHOLD && tiltDelta >= TILT_DELTA_THRESHOLD) {
            handlePickupDetected();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op.
    }

    private Notification buildNotification() {
        createNotificationChannelIfNeeded();

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.pickup_service_title))
                .setContentText(getString(R.string.pickup_service_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existingChannel != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pickup_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    private void updateMonitoringState() {
        if (sensorManager == null) {
            return;
        }

        if (isScreenOn()) {
            unregisterSensors();
            return;
        }

        if (significantMotionSensor != null) {
            armSignificantMotionSensor();
            unregisterAccelerometer();
            return;
        }

        registerAccelerometer();
    }

    private void armSignificantMotionSensor() {
        if (sensorManager == null || significantMotionSensor == null || significantMotionArmed) {
            return;
        }

        significantMotionArmed = sensorManager.requestTriggerSensor(
                significantMotionListener,
                significantMotionSensor
        );
    }

    private void registerAccelerometer() {
        if (sensorManager == null || accelerometerSensor == null || accelerometerRegistered) {
            return;
        }

        hasSensorSample = false;
        accelerometerRegistered = sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    private void unregisterAccelerometer() {
        if (!accelerometerRegistered || sensorManager == null) {
            return;
        }

        sensorManager.unregisterListener(this, accelerometerSensor);
        accelerometerRegistered = false;
        hasSensorSample = false;
    }

    private void unregisterSensors() {
        unregisterAccelerometer();

        if (sensorManager != null && significantMotionArmed && significantMotionSensor != null) {
            sensorManager.cancelTriggerSensor(significantMotionListener, significantMotionSensor);
        }
        significantMotionArmed = false;
    }

    private void handlePickupDetected() {
        long now = System.currentTimeMillis();
        if ((now - lastWakeTimestamp) < WAKE_COOLDOWN_MS) {
            return;
        }
        lastWakeTimestamp = now;

        wakeScreen();
        launchLauncher();
    }

    @SuppressWarnings("deprecation")
    private void wakeScreen() {
        if (powerManager == null) {
            return;
        }

        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE,
                "easy2:pickupWakeLock"
        );
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    private void launchLauncher() {
        Intent launcherIntent = new Intent(this, MainActivity.class);
        launcherIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );
        launcherIntent.putExtra(MainActivity.EXTRA_WAKE_FROM_PICKUP, true);
        startActivity(launcherIntent);
    }

    private boolean isScreenOn() {
        return powerManager != null && powerManager.isInteractive();
    }
}
