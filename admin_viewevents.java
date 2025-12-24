package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.widget.TextView;
import android.util.Log;
import android.content.Intent; // 👈 New Import for Intents
import android.net.Uri; // 👈 New Import for URI parsing

public class admin_viewevents extends AppCompatActivity {

    private TextView titleTextView;
    private WebView webView;

    // Define the same SharedPreferences constants used in AccountFragment_admin
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";
    private static final String KEY_USER_ID = "current_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to the activity_admin_viewevents.xml layout
        setContentView(R.layout.activity_admin_viewevents);

        // 1. Initialize views by finding them by their ID
        titleTextView = findViewById(R.id.title);
        webView = findViewById(R.id.webview);

        // 2. Set up the dynamic title (Hello, username!)
        String username = getUsername();
      //  titleTextView.setText("Welcome, " + username + "!");

        // 3. Configure the WebView settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        // 4. Custom WebViewClient to handle map links externally
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Check if the URL is a Google Maps link or a Geo URI
                if (url.startsWith("http://maps.google.com/") || url.startsWith("https://maps.google.com/") || url.startsWith("geo:")) {
                    try {
                        // Create an intent to view the URI, which opens the native map application
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        // Flag needed when starting an activity from a non-activity context (like a nested WebViewClient)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true; // Indicate that the URL was handled externally
                    } catch (Exception e) {
                        Log.e("WebView", "Could not open native map app for URL: " + url, e);
                        // If intent fails, fall through to default behavior (load in WebView)
                        return false;
                    }
                }

                // For all other links (non-map links), return false.
                // This tells the WebView to load the URL internally as usual.
                return false;
            }
        });

        // 5. Attach the Android-to-JavaScript bridge
        // The HTML script can now call Android.getUsername() and Android.getUserId()
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        // 6. Load the events management content
        String eventsUrl = "file:///android_asset/events_admin.html";
        Log.d("WebView", "Loading Events URL: " + eventsUrl);
        webView.loadUrl(eventsUrl);
    }

    /**
     * Retrieves the stored username from SharedPreferences.
     */
    private String getUsername() {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        // Retrieve the username, defaulting to "Admin" if the key is not found
        return prefs.getString(KEY_USERNAME, "Admin");
    }

    /**
     * Retrieves the stored user ID from SharedPreferences.
     */
    private String getUserId() {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        // Retrieve the user ID, defaulting to a fallback ID
        return prefs.getString(KEY_USER_ID, "anon_user_xyz");
    }

    // 7. Android-to-JavaScript Interface
    /**
     * Exposes methods annotated with @JavascriptInterface to the WebView JavaScript environment
     * under the name 'Android'.
     */
    private class AndroidBridge {

        @JavascriptInterface
        public String getUsername() {
            return admin_viewevents.this.getUsername();
        }

        @JavascriptInterface
        public String getUserId() {
            return admin_viewevents.this.getUserId();
        }
    }

    // 8. Handle back button presses for WebView navigation
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
