package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.webkit.JavascriptInterface;

public class admin_viewaccount extends AppCompatActivity {

    private ImageButton backButton;
    private WebView webView;
    private TextView titleTextView;

    // Define a key for the Intent extra
    public static final String EXTRA_USERNAME = "com.example.islamic.EXTRA_USERNAME";

    // 🚀 NEW: SharedPreferences constants (must match AccountFragment)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    // Define the URL to load in the WebView
    private static final String WEBVIEW_URL = "file:///android_asset/view_admin_account.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view to the layout file
        setContentView(R.layout.activity_admin_viewaccount);

        // Initialize UI components
        backButton = findViewById(R.id.back_button);
        webView = findViewById(R.id.webview);
        titleTextView = findViewById(R.id.title);

        // --- Personalization: Set Username ---
        // 🚀 UPDATED: Retrieve username from SharedPreferences (like in AccountFragment)
        SharedPreferences sharedPref = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        String username = sharedPref.getString(KEY_USERNAME, "Admin"); // Default to "Admin" if not found

        // Update the title with the fetched username
        if (username != null && !username.trim().isEmpty() && !username.equals("Admin")) {
            // If we have a real username, use it
            // titleTextView.setText("Hello, " + username + "!");
        } else {
            // Default text if no username is found in SharedPreferences
            titleTextView.setText("Hello, Admin!");
        }

        // --- WebView Setup ---
        // Enable JavaScript (often required for modern web content)
        webView.getSettings().setJavaScriptEnabled(true);

        // 🟢 NEW: Enable DOM Storage for HTML5 web apps
        webView.getSettings().setDomStorageEnabled(true);

        // Set a WebViewClient to handle page navigation within the WebView itself
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); // Load all links within this WebView
                return true;
            }
        });

        // Set a WebChromeClient to handle UI-related web events
        webView.setWebChromeClient(new WebChromeClient());

        // 🟢 NEW: Add JavaScript Interface to allow HTML/JS to call Java methods
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Load the specified URL
        webView.loadUrl(WEBVIEW_URL);

        // --- Back Button Setup ---
        // Set the click listener for the custom back button (ImageButton)
        backButton.setOnClickListener(v -> {
            // Check if the WebView can go back in its history
            if (webView.canGoBack()) {
                webView.goBack(); // Navigate back in WebView history
            } else {
                // If the WebView can't go back, close the current activity
                finish();
            }
        });
    }

    // --- System Back Press Handling ---
    /**
     * This method intercepts the system back button press.
     * It allows the WebView to navigate back in its history first,
     * and only closes the Activity if the WebView is at its starting page.
     */
    @Override
    public void onBackPressed() {
        // Check if the WebView can go back in its history
        if (webView != null && webView.canGoBack()) {
            webView.goBack(); // Navigate back in WebView history
        } else {
            // If the WebView can't go back, proceed with the default back action (close activity)
            super.onBackPressed();
        }
    }

    /**
     * 🟢 NEW INNER CLASS: JavaScript Interface for WebView communication
     * This allows the HTML/JavaScript in the WebView to call Android methods
     */
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Method to retrieve the username from Android SharedPreferences
         * This can be called from JavaScript using: Android.getUsername()
         */
        @JavascriptInterface
        public String getUsername() {
            SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_USERNAME, "Admin");
        }

        /**
         * Method to retrieve the User ID (which is the username in this app)
         * This can be called from JavaScript using: Android.getUserId()
         */
        @JavascriptInterface
        public String getUserId() {
            SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_USERNAME, "Admin");
        }
    }
}