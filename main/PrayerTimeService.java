package com.example.islamic;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class PrayerTimeService extends Service {

    // --- CONSTANTS ---
    private static final String CHANNEL_ID_MAIN = "PrayerTimeChannelMain";
    private static final String CHANNEL_ID_COUNTDOWN = "PrayerTimeChannelCountdown";
    private static final int NOTIFICATION_ID = 101;
    private static final int NOTIFICATION_ID_COUNTDOWN = 102;
    private static final String TAG = "PrayerTimeService";
    private static final long DATA_CHECK_INTERVAL_HOURS = 6;
    private static final long ALERT_BEFORE_MINUTES = 5;
    private static final long ALERT_BEFORE_TWO_MINUTES = 2;
    private static final long NOTIFICATION_UPDATE_INTERVAL_MS = 20000;

    // ACTION CONSTANTS FOR ALARM MANAGER
    private static final String ACTION_PRAYER_ALERT = "com.example.islamic.PRAYER_ALERT";
    private static final String ACTION_TWO_MIN_ALERT = "com.example.islamic.TWO_MIN_ALERT";
    private static final String ACTION_POST_PRAYER = "com.example.islamic.POST_PRAYER";

    // --- VARIABLES ---
    private Timer notificationUpdateTimer;
    private TimerTask notificationUpdateTask;

    // Data structures
    // NOTE: This assumes a class named 'clock' with a nested public class 'PrayerTime' exists
    private List<clock.PrayerTime> todayPrayerTimes = new ArrayList<>();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    // !! IMPORTANT: REPLACE WITH YOUR ACTUAL URL !!
    private final String DATABASE_URL = "https://clothsuggest-default-rtdb.firebaseio.com/";

    private ValueEventListener prayerTimeListener;
    private FirebaseDatabase database;
    private AlarmManager alarmManager;

    // Alarm Pending Intents
    private PendingIntent alarmPendingIntent;
    private PendingIntent twoMinAlarmPendingIntent;
    private PendingIntent postPrayerPendingIntent;

    // Wake Lock variable
    private WakeLock partialWakeLock;
    private WakeLock screenWakeLock; // Initialized in triggerAlertAndWakeScreen

    @Override
    public void onCreate() {
        super.onCreate();

        database = FirebaseDatabase.getInstance(DATABASE_URL);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Create both notification channels
        createNotificationChannels();

        // Initialize the Partial Wake Lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":PartialWakeLock");

        // Initialize Alarm Pending Intents (Using unique request codes: 0, 1, 2)
        Intent mainAlarmIntent = new Intent(this, PrayerTimeService.class);
        mainAlarmIntent.setAction(ACTION_PRAYER_ALERT);
        alarmPendingIntent = PendingIntent.getService(this, 0, mainAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent twoMinAlarmIntent = new Intent(this, PrayerTimeService.class);
        twoMinAlarmIntent.setAction(ACTION_TWO_MIN_ALERT);
        twoMinAlarmPendingIntent = PendingIntent.getService(this, 1, twoMinAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent postPrayerIntent = new Intent(this, PrayerTimeService.class);
        postPrayerIntent.setAction(ACTION_POST_PRAYER);
        postPrayerPendingIntent = PendingIntent.getService(this, 2, postPrayerIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // R.drawable.logo must exist
        Notification notification = buildBaseNotification("Loading prayer times...");

        // Start as Foreground Service
        // IMPORTANT: R.drawable.logo must exist
        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        );

        // Acquire the Partial Wake Lock to keep the CPU running until job is done
        if (partialWakeLock != null && !partialWakeLock.isHeld()) {
            partialWakeLock.acquire();
            Log.d(TAG, "Partial Wake Lock acquired.");
        }

        // Anti-Termination Improvement: Check for system restart flag
        if (flags == START_FLAG_REDELIVERY) {
            Log.w(TAG, "Service restarted by system (START_FLAG_REDELIVERY).");
        }

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PRAYER_ALERT.equals(action)) {
                // This is triggered by our 5-Min or Prayer Time alarm
                handleAlarmAlert(ACTION_PRAYER_ALERT);
            } else if (ACTION_TWO_MIN_ALERT.equals(action)) {
                handleAlarmAlert(ACTION_TWO_MIN_ALERT);
            } else if (ACTION_POST_PRAYER.equals(action)) {
                handleAlarmAlert(ACTION_POST_PRAYER);
            } else {
                // Initial service start or data reload
                setupRealTimeUpdates();
            }
        } else {
            // Service restart by system or null intent from system.
            Log.w(TAG, "onStartCommand called with null intent (System Restart likely). Running setup.");
            setupRealTimeUpdates();
        }

        // IMPORTANT: Ensure the lock is released quickly after the task is done
        if (partialWakeLock != null && partialWakeLock.isHeld()) {
            partialWakeLock.release();
            Log.d(TAG, "Partial Wake Lock released.");
        }

        // CRITICAL for anti-termination: START_STICKY tells the system to try and recreate the service
        // after it has sufficient resources.
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopNotificationUpdateTimer();

        // Cancel all future alarms
        if (alarmManager != null) {
            if (alarmPendingIntent != null) alarmManager.cancel(alarmPendingIntent);
            if (twoMinAlarmPendingIntent != null) alarmManager.cancel(twoMinAlarmPendingIntent);
            if (postPrayerPendingIntent != null) alarmManager.cancel(postPrayerPendingIntent);
            Log.d(TAG, "All AlarmManager schedules canceled.");
        }

        // Release the Partial Wake Lock if held
        if (partialWakeLock != null && partialWakeLock.isHeld()) {
            partialWakeLock.release();
            Log.d(TAG, "Partial Wake Lock released on destroy.");
        }

        // Release the Screen Wake Lock if held
        if (screenWakeLock != null && screenWakeLock.isHeld()) {
            screenWakeLock.release();
            Log.d(TAG, "Screen Wake Lock released on destroy.");
        }


        removeFirebaseListener();
        Log.d(TAG, "Service destroyed.");
    }

    // -----------------------------------------------------------------------
    // NOTIFICATION, ALARM, AND WAKELOCK LOGIC
    // -----------------------------------------------------------------------

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // Use R.raw.islamic2 for ALL alarm channels
            Uri islamic2SoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.islamic2);
            AudioAttributes alarmAudioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            // 1. MAIN PRAYER CHANNEL (for 5-min alert and Prayer Time - now uses R.raw.islamic2)
            NotificationChannel mainChannel = new NotificationChannel(CHANNEL_ID_MAIN, "Prayer Time Main Alerts", NotificationManager.IMPORTANCE_HIGH);
            mainChannel.setDescription("Alerts for 5 minutes before and at the prayer time.");
            mainChannel.setSound(islamic2SoundUri, alarmAudioAttributes);
            mainChannel.setVibrationPattern(new long[]{500, 500, 500});
            mainChannel.enableVibration(true);
            notificationManager.createNotificationChannel(mainChannel);

            // 2. COUNTDOWN CHANNEL (for 2-min countdown - also uses R.raw.islamic2)
            NotificationChannel countdownChannel = new NotificationChannel(CHANNEL_ID_COUNTDOWN, "Prayer Time Countdown", NotificationManager.IMPORTANCE_HIGH);
            countdownChannel.setDescription("Alerts for 2 minutes before prayer.");
            countdownChannel.setSound(islamic2SoundUri, alarmAudioAttributes);
            countdownChannel.setVibrationPattern(new long[]{0, 200, 200, 200});
            countdownChannel.enableVibration(true);
            notificationManager.createNotificationChannel(countdownChannel);
        }
    }

    // -----------------------------------------------------------------------
    // NOTIFICATION DISPLAY METHODS
    // -----------------------------------------------------------------------

    private Notification buildBaseNotification(String contentText) {
        // NOTE: Replace `clock.class` with the main activity class name (e.g., `MainActivity.class`)
        Intent notificationIntent = new Intent(this, clock.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // Uses the main channel but is set to LOW priority and ONGOING
        return new NotificationCompat.Builder(this, CHANNEL_ID_MAIN)
                .setContentTitle("Next Prayer")
                .setContentText(contentText)
                // IMPORTANT: Replace R.drawable.logo with the actual resource ID
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    /**
     * Standard method to update the ongoing notification (silent, low priority).
     */
    private void updateNotification(String title, String contentText) {
        // Reusing the base notification ID (101) for the ongoing display
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_MAIN)
                .setContentTitle(title)
                .setContentText(contentText)
                // IMPORTANT: Replace R.drawable.logo with the actual resource ID
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(buildBaseNotification("").contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true) // Ensure it's silent for the periodic update
                .build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Triggers the custom tone/vibration set in the Channel AND attempts to turn the screen on.
     * @param channelId The channel to use (MAIN or COUNTDOWN).
     */
    private void triggerAlertAndWakeScreen(String title, String contentText, String channelId, int notificationId) {
        // 1. **Screen Wake Lock** (Best Effort)
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (screenWakeLock == null) {
            screenWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    TAG + ":ScreenWakeLock"
            );
        }

        final long WAKE_TIME_MS = 5000;
        if (!screenWakeLock.isHeld()) {
            // Note: Acquire with a timeout to prevent battery drain if not explicitly released
            screenWakeLock.acquire(WAKE_TIME_MS);
            Log.d(TAG, "Screen Wake Lock acquired for " + WAKE_TIME_MS + "ms.");
        }

        // 2. Update notification (HIGH priority, triggers tone/vibration)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.logo)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // Set to HIGH priority for both alert types to ensure they break through DND and ring loudly
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // Allow high priority notifications to be dismissed

        // Use the proper ID for the alert (101 for main, 102 for countdown)
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, builder.build());
    }


    // -----------------------------------------------------------------------
    // ALARM HANDLER AND SCHEDULING LOGIC
    // -----------------------------------------------------------------------

    /**
     * Schedules all three potential next single, Doze-safe alarms.
     */
    private void scheduleNextEvent() {
        // Cancel all existing non-persistent alarms first for a clean slate
        alarmManager.cancel(alarmPendingIntent);
        alarmManager.cancel(twoMinAlarmPendingIntent);
        alarmManager.cancel(postPrayerPendingIntent);

        if (todayPrayerTimes.isEmpty()) {
            // Schedule a recurring check every X hours to look for new data
            scheduleAlarm(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(DATA_CHECK_INTERVAL_HOURS), "Data Check", alarmPendingIntent);
            return;
        }

        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();
        int nowMinutesTotal = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        // 1. Find the next prayer (including tomorrow's Fajr)
        clock.PrayerTime nextPrayer = null;
        // Sort the list based on time to ensure correct 'next' finding
        todayPrayerTimes.sort(Comparator.comparingInt(pt -> timeToMinutes(pt.time)));

        for (clock.PrayerTime pt : todayPrayerTimes) {
            if (timeToMinutes(pt.time) > nowMinutesTotal) {
                nextPrayer = pt;
                break;
            }
        }
        if (nextPrayer == null) {
            // All today's prayers are over. Next is tomorrow's Fajr.
            // NOTE: getPrayerCalendar handles advancing the day for the next event.
            nextPrayer = todayPrayerTimes.get(0);
        }

        // 2. Determine all three possible next trigger times
        try {
            Calendar prayerCal = getPrayerCalendar(nextPrayer, now);
            long prayerTimeMs = prayerCal.getTimeInMillis();

            // All times must be in the future to be scheduled
            long fiveMinutesBefore = prayerTimeMs - TimeUnit.MINUTES.toMillis(ALERT_BEFORE_MINUTES);
            long twoMinutesBefore = prayerTimeMs - TimeUnit.MINUTES.toMillis(ALERT_BEFORE_TWO_MINUTES);
            long oneMinuteAfter = prayerTimeMs + TimeUnit.MINUTES.toMillis(1);

            // --- 5-Minute Alert (Main Alarm) or Prayer Time Alert ---
            if (fiveMinutesBefore > nowMillis) {
                scheduleAlarm(fiveMinutesBefore, nextPrayer.name + " (5-Min Alert)", alarmPendingIntent);
            } else {
                // If 5-min alert is in the past, schedule the actual prayer time
                scheduleAlarm(prayerTimeMs, nextPrayer.name + " (Prayer Time)", alarmPendingIntent);
            }

            // --- 2-Minute Alert ---
            if (twoMinutesBefore > nowMillis) {
                scheduleAlarm(twoMinutesBefore, nextPrayer.name + " (2-Min Alert)", twoMinAlarmPendingIntent);
            }

            // --- 1-Minute Post-Prayer Tone ---
            // Only schedule if the actual prayer time is still in the future or very recent
            if (prayerTimeMs > nowMillis) {
                scheduleAlarm(oneMinuteAfter, nextPrayer.name + " (Post-Prayer)", postPrayerPendingIntent);
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error scheduling next event.", e);
            scheduleAlarm(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1), "Error Recovery Check", alarmPendingIntent);
        }
    }

    /**
     * Executes the Doze-safe alarm using the best available API, handling permission requirements.
     */
    private void scheduleAlarm(long triggerAtMillis, String eventName, PendingIntent pendingIntent) {
        // No need to cancel here, it's canceled in scheduleNextEvent

        if (alarmManager == null) {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null. Cannot schedule alarm.");
                return;
            }
        }

        Log.d(TAG, "Attempting to schedule: " + eventName + " at " + timeFormat.format(new Date(triggerAtMillis)));

        // API 31+ (Android 12): Requires SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "Scheduled: " + eventName + " (API 31+ EXACT)");
            } else {
                // Fallback if exact alarm permission is denied
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 60000, pendingIntent);
                Log.w(TAG, "Scheduled: " + eventName + " (API 31+ WINDOW). Exact permission denied. Alerts may be delayed.");
            }
        }
        // API 23 to 30 (Marshmallow to Android 11): Uses setExactAndAllowWhileIdle
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d(TAG, "Scheduled: " + eventName + " (API 23-30 EXACT_IDLE)");
        }
        // API < 23 (Pre-Marshmallow): Uses setExact
        else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d(TAG, "Scheduled: " + eventName + " (API < 23 EXACT)");
        }
    }


    /**
     * Logic that runs when any of the Doze-safe alarms fire.
     */
    private void handleAlarmAlert(String action) {
        if (todayPrayerTimes.isEmpty()) {
            scheduleNextEvent();
            return;
        }

        // We use the same logic to find the CURRENTLY relevant prayer
        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();
        int nowMinutesTotal = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        // Find the next prayer
        clock.PrayerTime nextPrayer = null;
        // Re-sort the list just in case it wasn't done before finding next
        todayPrayerTimes.sort(Comparator.comparingInt(pt -> timeToMinutes(pt.time)));

        for (clock.PrayerTime pt : todayPrayerTimes) {
            if (timeToMinutes(pt.time) > nowMinutesTotal) {
                nextPrayer = pt;
                break;
            }
        }
        if (nextPrayer == null) {
            nextPrayer = todayPrayerTimes.get(0);
        }

        if (nextPrayer == null) {
            scheduleNextEvent();
            return;
        }

        try {
            Calendar prayerCal = getPrayerCalendar(nextPrayer, now);
            long timeUntilPrayerMs = prayerCal.getTimeInMillis() - nowMillis;

            // Add small buffer to check to ensure the alert fires reliably
            long timeBufferMs = TimeUnit.SECONDS.toMillis(30);

            if (ACTION_PRAYER_ALERT.equals(action)) {
                // This handles the 5-Minute Alert and the Prayer Time Alert (Uses MAIN tone)
                long fiveMinAlertWindowMs = TimeUnit.MINUTES.toMillis(ALERT_BEFORE_MINUTES) + timeBufferMs;

                if (timeUntilPrayerMs > timeBufferMs && timeUntilPrayerMs <= fiveMinAlertWindowMs) {
                    // **5-MINUTE WARNING**
                    String timeDisplay = formatTimeRemaining(timeUntilPrayerMs);
                    triggerAlertAndWakeScreen(
                            "5 MINUTE ALERT: " + nextPrayer.name,
                            nextPrayer.name + " starts in approx. " + timeDisplay + " at " + nextPrayer.time,
                            CHANNEL_ID_MAIN,
                            NOTIFICATION_ID
                    );
                } else if (timeUntilPrayerMs <= timeBufferMs && timeUntilPrayerMs >= -TimeUnit.MINUTES.toMillis(2)) {
                    // **PRAYER TIME HAS STARTED**
                    triggerAlertAndWakeScreen(
                            "PRAYER TIME: " + nextPrayer.name + " NOW",
                            "Time for " + nextPrayer.name + " has officially started.",
                            CHANNEL_ID_MAIN,
                            NOTIFICATION_ID
                    );
                }

            } else if (ACTION_TWO_MIN_ALERT.equals(action)) {
                // **2-MINUTE WARNING** (Uses COUNTDOWN tone - set to IMPORTANCE_HIGH to ring)
                long twoMinAlertWindowMs = TimeUnit.MINUTES.toMillis(ALERT_BEFORE_TWO_MINUTES) + timeBufferMs;

                if (timeUntilPrayerMs > timeBufferMs && timeUntilPrayerMs <= twoMinAlertWindowMs) {
                    String timeDisplay = formatTimeRemaining(timeUntilPrayerMs);
                    triggerAlertAndWakeScreen(
                            "2 MINUTE COUNTDOWN: " + nextPrayer.name,
                            nextPrayer.name + " starts in approx. " + timeDisplay + " at " + nextPrayer.time,
                            CHANNEL_ID_COUNTDOWN,
                            NOTIFICATION_ID_COUNTDOWN
                    );
                }

            } else if (ACTION_POST_PRAYER.equals(action)) {
                // **1-MINUTE POST-PRAYER TONE** (Uses COUNTDOWN tone, then cancels alert)
                long oneMinuteAfter = -TimeUnit.MINUTES.toMillis(1);
                // Check if the current time is 1-2 minutes *after* the prayer time
                if (timeUntilPrayerMs < -timeBufferMs && timeUntilPrayerMs >= oneMinuteAfter - timeBufferMs) {
                    triggerAlertAndWakeScreen(
                            "PRAYER STARTED: " + nextPrayer.name,
                            "Take a moment for " + nextPrayer.name + ". Reminder dismissed.",
                            CHANNEL_ID_COUNTDOWN, // Using the distinct tone/channel
                            NOTIFICATION_ID_COUNTDOWN // Using the temporary notification ID
                    );
                    // Dismiss the temporary alert notification after it fires
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(NOTIFICATION_ID_COUNTDOWN);
                }
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error processing alarm alert.", e);
        }

        // Always reschedule the next event after handling ANY alarm
        scheduleNextEvent();

        // Immediately trigger a notification content update after handling the alert
        updateRealTimeNotificationContent();
    }

    // -----------------------------------------------------------------------
    // REAL-TIME NOTIFICATION UPDATER LOGIC
    // -----------------------------------------------------------------------

    private void startNotificationUpdateTimer() {
        stopNotificationUpdateTimer();
        notificationUpdateTimer = new Timer();
        notificationUpdateTask = new TimerTask() {
            @Override
            public void run() {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateRealTimeNotificationContent();
                    }
                });
            }
        };

        notificationUpdateTimer.scheduleAtFixedRate(
                notificationUpdateTask,
                0,
                NOTIFICATION_UPDATE_INTERVAL_MS
        );
        Log.d(TAG, "Notification real-time update timer started with " + (NOTIFICATION_UPDATE_INTERVAL_MS/1000) + "s interval.");
    }

    private void stopNotificationUpdateTimer() {
        if (notificationUpdateTimer != null) {
            notificationUpdateTimer.cancel();
            notificationUpdateTimer = null;
            Log.d(TAG, "Notification real-time update timer stopped.");
        }
        if (notificationUpdateTask != null) {
            notificationUpdateTask.cancel();
            notificationUpdateTask = null;
        }
    }

    /**
     * Calculates the next event and updates the ongoing notification with the time remaining.
     */
    private void updateRealTimeNotificationContent() {
        if (todayPrayerTimes.isEmpty()) {
            updateNotification("No Data", "No times loaded. Checking again in " + DATA_CHECK_INTERVAL_HOURS + " hours.");
            return;
        }

        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();
        int nowMinutesTotal = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        // Find the next prayer
        clock.PrayerTime nextPrayer = null;
        // Re-sort the list just in case it wasn't done
        todayPrayerTimes.sort(Comparator.comparingInt(pt -> timeToMinutes(pt.time)));

        for (clock.PrayerTime pt : todayPrayerTimes) {
            if (timeToMinutes(pt.time) > nowMinutesTotal) {
                nextPrayer = pt;
                break;
            }
        }
        if (nextPrayer == null) {
            // Next is tomorrow's Fajr
            nextPrayer = todayPrayerTimes.get(0);
        }

        try {
            Calendar prayerCal = getPrayerCalendar(nextPrayer, now);
            long prayerTimeMs = prayerCal.getTimeInMillis();

            long timeUntilNextEvent = prayerTimeMs - nowMillis;
            String timeDisplay = formatTimeRemaining(timeUntilNextEvent);

            updateNotification("Next Prayer: " + nextPrayer.name,
                    "Time in " + timeDisplay + " (" + nextPrayer.time + ")");

        } catch (ParseException e) {
            Log.e(TAG, "Error calculating real-time notification update.", e);
            updateNotification("Error", "Error calculating time.");
        }
    }


    // -----------------------------------------------------------------------
    // FIREBASE & UTILITY LOGIC
    // -----------------------------------------------------------------------

    private void setupRealTimeUpdates() {
        removeFirebaseListener();
        stopNotificationUpdateTimer();

        final String dateString = dateFormatter.format(new Date());
        String path = "prayerTimes/" + dateString;

        prayerTimeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Data structure parsing logic
                    Map<String, Object> rawData = (Map<String, Object>) snapshot.getValue();
                    List<clock.PrayerTime> times = new ArrayList<>();

                    for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                        String key = entry.getKey();
                        Object valueObj = entry.getValue();
                        String timeStr = null;
                        if (valueObj instanceof Map) {
                            Map<String, String> value = (Map<String, String>) valueObj;
                            timeStr = value.get("time");
                        } else if (valueObj instanceof String) {
                            timeStr = (String) valueObj;
                        }

                        if (timeStr != null) {
                            String normalizedName = key;
                            if (key.equalsIgnoreCase("Isha")) {
                                normalizedName = "Isha'a";
                            }
                            // Assumes 'clock.PrayerTime' constructor is clock.PrayerTime(String name, String time)
                            times.add(new clock.PrayerTime(normalizedName, timeStr));
                        }
                    }

                    times.sort(Comparator.comparingInt(pt -> timeToMinutes(pt.time)));
                    todayPrayerTimes = times;

                    scheduleNextEvent();
                    startNotificationUpdateTimer();

                    Log.d(TAG, "Firebase data loaded. Alarms scheduled. Real-time updates started.");

                } else {
                    todayPrayerTimes = new ArrayList<>();
                    scheduleNextEvent();
                    updateRealTimeNotificationContent();
                    Log.w(TAG, "Firebase snapshot not found for today's date: " + dateString);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                todayPrayerTimes = new ArrayList<>();
                scheduleNextEvent();
                updateRealTimeNotificationContent();
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        database.getReference(path).addValueEventListener(prayerTimeListener);
    }

    private void removeFirebaseListener() {
        if (prayerTimeListener != null && database != null) {
            final String dateString = dateFormatter.format(new Date());
            String path = "prayerTimes/" + dateString;
            database.getReference(path).removeEventListener(prayerTimeListener);
            Log.d(TAG, "Firebase listener removed.");
        }
    }

    private Calendar getPrayerCalendar(clock.PrayerTime pt, Calendar now) throws ParseException {
        Calendar prayerCal = Calendar.getInstance();
        prayerCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        Date prayerTimeOnly = timeFormat.parse(pt.time);
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(prayerTimeOnly);

        prayerCal.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY));
        prayerCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE));
        prayerCal.set(Calendar.SECOND, 0);
        prayerCal.set(Calendar.MILLISECOND, 0);

        // If the calculated time is in the past (by more than 1 second), move it to the next day
        if (prayerCal.getTimeInMillis() < now.getTimeInMillis() - 1000) {
            prayerCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return prayerCal;
    }

    private String formatTimeRemaining(long milliseconds) {
        if (milliseconds < 0) {
            return "Passed";
        }
        if (milliseconds < 1000) {
            return "Less than 1s";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        long hours = TimeUnit.MINUTES.toHours(minutes);

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.US, "%02dm %02ds", minutes, seconds);
        } else {
            return String.format(Locale.US, "%02ds", seconds);
        }
    }

    private int timeToMinutes(String timeStr) {
        try {
            Date date = timeFormat.parse(timeStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + timeStr, e);
            return 0;
        }
    }
}