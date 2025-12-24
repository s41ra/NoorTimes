package com.example.islamic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * Ensures the PrayerTimeService is restarted after key system events:
 * 1. Device boot (fully unlocked).
 * 2. App update (package replaced).
 *
 * NOTE: This receiver requires the RECEIVE_BOOT_COMPLETED permission in the manifest.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Log.w(TAG, "Received null action intent.");
            return;
        }

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                // This fires when the system has fully booted AND is ready for app operations.
                // It is generally the safest and most reliable point to start a service.
                Log.i(TAG, "Event: Device booted (ACTION_BOOT_COMPLETED). Starting PrayerTimeService.");
                startPrayerTimeService(context);
                break;

            case Intent.ACTION_MY_PACKAGE_REPLACED:
                // CRITICAL for persistence: This fires after the app has been updated.
                // An update often terminates foreground services, requiring a mandatory restart.
                Log.i(TAG, "Event: App updated (ACTION_MY_PACKAGE_REPLACED). Restarting PrayerTimeService.");
                startPrayerTimeService(context);
                break;

            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                // Starting the service here is often unnecessary and potentially complex (since
                // it's before user unlock). For most services, ACTION_BOOT_COMPLETED is sufficient.
                // If you NEED the service to run immediately upon power on, keep this:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    // For pre-Nougat devices, this action might be used if ACTION_BOOT_COMPLETED isn't enough.
                    Log.i(TAG, "Event: Device booted locked (ACTION_LOCKED_BOOT_COMPLETED). Starting PrayerTimeService.");
                    startPrayerTimeService(context);
                }
                break;

            default:
                Log.d(TAG, "Ignoring intent action: " + action);
                break;
        }
    }

    /**
     * Helper to safely start the PrayerTimeService as a foreground service.
     */
    private void startPrayerTimeService(Context context) {
        Intent serviceIntent = new Intent(context, PrayerTimeService.class);

        // Use the modern, robust way to start the service
        try {
            // ContextCompat.startForegroundService ensures compliance with Android 8.0 (Oreo) limits.
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (IllegalStateException e) {
            // Thrown if the app is in a state where it cannot start a foreground service
            // (e.g., after the user force-stopped the app). The service cannot be started in this case.
            Log.e(TAG, "Could not start PrayerTimeService: App is restricted. " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting PrayerTimeService: " + e.getMessage());
        }
    }
}