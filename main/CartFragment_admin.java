package com.example.islamic;

import android.content.Intent; // Import Intent
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

// IMPORTANT: You must create a new Activity class named 'admin_prayer'
// in the 'com.example.islamic' package for this code to work.
// I am assuming the class name is 'AdminPrayerActivity.class' based on convention,
// but I will use the name 'admin_prayer.class' as you specified.

public class CartFragment_admin extends Fragment {

    private static final String TAG = "CartFragment_admin";

    private TextView titleTextView;
    private WebView webView;
    private ImageView calendarIcon;

    // SharedPreferences constants
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    public CartFragment_admin() {
        // Required empty public constructor
    }

    public static CartFragment_admin newInstance() {
        return new CartFragment_admin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cart_admin, container, false);

        // Bind views
        titleTextView = view.findViewById(R.id.title);
        webView = view.findViewById(R.id.webview);
        calendarIcon = view.findViewById(R.id.calendar_icon);

        // Set click listener for the calendar icon
        calendarIcon.setOnClickListener(v -> {
            Log.d(TAG, "Calendar icon in header clicked. Navigating to admin_prayer.");

            // 🔑 KEY CHANGE: Start the admin_prayer Activity
            // Assuming 'admin_prayer.class' is the Activity you want to launch.
            Intent intent = new Intent(requireActivity(), admin_prayer.class);
            startActivity(intent);
        });

        // Get username from SharedPreferences
        final String username = getUsername();
        //    titleTextView.setText("Hello, " + username + "!");

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Set a custom WebViewClient to inject JavaScript
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // 1. Escape the username for safe injection into a JavaScript string
                String safeUsername = username.replace("'", "\\'");

                // 2. Define the JavaScript code to execute
                String jsInjectionCode = "javascript: " +
                        "localStorage.setItem('adminUsername', '" + safeUsername + "');" +
                        "if (typeof setUsernameFromAndroid === 'function') { setUsernameFromAndroid(); }";

                // 3. Execute the JavaScript
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript(jsInjectionCode, null);
                } else {
                    view.loadUrl(jsInjectionCode);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/admin_postprayer.html");

        return view;
    }

    // Helper method to get the username from SharedPreferences
    private String getUsername() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_FILE_NAME, getContext().MODE_PRIVATE);
        // User-defined data used: The username is came from the provided signup code.
        return prefs.getString(KEY_USERNAME, "Admin");
    }
}