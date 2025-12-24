package com.example.islamic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * Service Watchdog: Ensures the PrayerTimeService is running after critical system events.
 * It uses startForegroundService for compliance with modern Android OS restrictions.
 */
public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerWatchdog";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (action == null) {
            Log.w(TAG, "Null action received. Ensuring PrayerTimeService is running as a fallback.");
            startPrayerTimeService(context);
            return;
        }

        // Use strict checks for system actions that require the service to restart
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_REBOOT:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                Log.i(TAG, "System event detected (" + action + "). Starting PrayerTimeService.");
                startPrayerTimeService(context);
                break;

            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                // Handle devices that boot with storage encryption (Android Nougat and later)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.i(TAG, "Device-protected storage boot detected (" + action + "). Starting PrayerTimeService.");
                    startPrayerTimeService(context);
                }
                break;

            default:
                // If it's one of our custom alarm actions, ensure the service is running.
                if (action.startsWith("com.example.islamic.")) {
                    Log.d(TAG, "Custom application intent detected (" + action + "). Ensuring PrayerTimeService is running.");
                    startPrayerTimeService(context);
                } else {
                    Log.d(TAG, "Ignoring unrelated system intent: " + action);
                }
                break;
        }
    }

    /**
     * Helper to start the PrayerTimeService as a foreground service.
     */
    private void startPrayerTimeService(Context context) {
        Intent serviceIntent = new Intent(context, PrayerTimeService.class);

        try {
            // Use startForegroundService for compliance on Android Oreo (API 26) and later.
            // This is crucial for persistence.
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Could not start PrayerTimeService as foreground: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting PrayerTimeService: " + e.getMessage());
        }
    }
}