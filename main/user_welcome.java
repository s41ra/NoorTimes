package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog; // 💡 NEW: Import for the spinning loader
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler; // 💡 NEW: Import for managing a slight delay
import android.util.Log;
import android.webkit.WebChromeClient; // 💡 ADDED: Import for WebChromeClient
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class user_welcome extends AppCompatActivity {

    private static final String TAG = "UserWelcomeActivity";
    // 🚀 CONSTANTS for SharedPreferences file name and key (must match Login.java)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    private WebView centerWebView;
    private Button getStartedButton;
    private TextView welcomeTextView;

    // Placeholder for the URL you want to display in the WebView
    private final String WEB_VIEW_URL = "file:///android_asset/todayevent_admin.html";

    // 💡 NEW: Handler for scheduling the delay
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_welcome);

        // Initialize views
        centerWebView = findViewById(R.id.centerWebView);
        getStartedButton = findViewById(R.id.getStartedButton);
        welcomeTextView = findViewById(R.id.welcomeTextView);

        // 🌟 START: Retrieve Username from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        String username = sharedPref.getString(KEY_USERNAME, "Guest");

        Log.d(TAG, "Retrieved Username: " + username);

        // 🌟 UPDATED: Use the retrieved username to personalize the welcome message
        if (welcomeTextView != null) {
            // Personalize the greeting with "Assalamu Alaikum" and the username
            welcomeTextView.setText("Assalamu Alaikum, " + username + "!");
        }
        // 🌟 END: Retrieve and set Username

        // 1. Configure and Load the WebView
        setupWebView();

        // 2. Set OnClickListener for the Continue button
        getStartedButton.setOnClickListener(v -> {
            // 💡 MODIFIED: Call the new method to show loading and navigate
            showLoadingAndProceedToDashboard();
        });
    }

    /**
     * Shows a spinning loading dialog and then navigates to userdashboard.class.
     */
    private void showLoadingAndProceedToDashboard() {
        // 💡 NEW: Create and show the ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(user_welcome.this);
        progressDialog.setMessage("Loading dashboard... Please wait...");
        progressDialog.setCancelable(false); // User must wait for the load
        progressDialog.show();

        // 💡 NEW: Use a Handler to introduce a small delay (e.g., 1000ms = 1 second)
        // This simulates a network/startup operation and allows the user to see the loading dialog.
        handler.postDelayed(() -> {
            // 1. Dismiss the loading dialog
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            // 2. Navigate to the user dashboard.
            Intent intent = new Intent(user_welcome.this, userdashboard.class);
            startActivity(intent);

            // 🛑 REMOVED finish(); 🛑
            // By removing finish(), user_welcome remains on the back stack.
            // Pressing 'Back' from userdashboard will now return here.

        }, 1000); // Wait for 1 second (adjust as needed)
    }

    /**
     * Configures the WebView settings and loads the specified HTML content.
     */
    private void setupWebView() {
        // Enable JavaScript for interactive web pages
        centerWebView.getSettings().setJavaScriptEnabled(true);

        // 🟢 ADDED: Required for modern web content features like alerts, progress, and <video>
        centerWebView.setWebChromeClient(new WebChromeClient());

        // 🟢 ADDED: Enable DOM storage (Web Storage API - localStorage and sessionStorage)
        // This is often required for modern web applications to function correctly.
        centerWebView.getSettings().setDomStorageEnabled(true);

        // ❌ NEW: Disable the built-in zoom controls (the +/- buttons or a zoom overlay)
        centerWebView.getSettings().setBuiltInZoomControls(false);

        // ❌ OPTIONAL: This prevents the zoom controls from showing even if they are enabled.
        // If you set setBuiltInZoomControls(false), this line is often redundant,
        // but included for completeness if you wanted to disable the *display* of controls.
        centerWebView.getSettings().setDisplayZoomControls(false);

        // Set a WebViewClient to handle links internally within the app
        centerWebView.setWebViewClient(new WebViewClient());

        // Load the specified local HTML file
        centerWebView.loadUrl(WEB_VIEW_URL);
    }
}