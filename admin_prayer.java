package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebChromeClient; // Import WebChromeClient
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class admin_prayer extends AppCompatActivity {

    // Declare member variables for the views
    private TextView titleTextView;
    private WebView mainWebView;

    // Use the same constants as AccountFragment_admin.java and Login.java
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this layout name matches your XML file (e.g., activity_admin_prayer.xml)
        setContentView(R.layout.activity_admin_prayer);

        // 1. Find and initialize the views by their IDs
        titleTextView = findViewById(R.id.title);
        mainWebView = findViewById(R.id.webview);

        // --- Customization ---

        // Get the username from SharedPreferences
        String username = getUsername();

        // Display personalized title using saved username.
        // Based on the remembered instruction: username is derived from the signup code.
  //      titleTextView.setText("Hello, " + username + "!");

        // 2. Configure the WebView
        // Enable JavaScript for interactive web content
        mainWebView.getSettings().setJavaScriptEnabled(true);

        // *** ADDED: Enable DOM Storage for web applications that use localStorage ***
        mainWebView.getSettings().setDomStorageEnabled(true);

        // Ensure links are opened within the WebView itself, not in an external browser
        mainWebView.setWebViewClient(new WebViewClient());

        // *** ADDED: Set WebChromeClient to handle UI-related events (e.g., JavaScript alerts, progress, favicon) ***
        mainWebView.setWebChromeClient(new WebChromeClient());

        // Load content into the WebView (Example: a prayer times website)
        // NOTE: This URL is just an example. You can replace it with any internal or external link.
        String prayerTimeUrl = "file:///android_asset/admin_view_prayer.html";
        mainWebView.loadUrl(prayerTimeUrl);

        // NOTE: If you are using external URLs, you must also add the INTERNET permission
        // to your AndroidManifest.xml file: <uses-permission android:name="android.permission.INTERNET" />
    }

    /**
     * Retrieves the saved username from SharedPreferences.
     * The username is saved upon successful login/signup (derived from the signup code).
     * @return The saved username or "Admin" as a default.
     */
    private String getUsername() {
        // Use MODE_PRIVATE to ensure the preferences file is only accessible by this app
        SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        // The saved username is derived from the signup code.
        return prefs.getString(KEY_USERNAME, "Admin");
    }
}