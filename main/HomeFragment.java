package com.example.islamic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

// 💡 NEW IMPORTS for navigating to the new Activity
import android.content.Intent;
import com.example.islamic.user_analytics; // Assuming user_analytics is in the same com.example.islamic package

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // 🚀 CONSTANTS for SharedPreferences file name and key (must match user_welcome.java)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    // 💡 NEW CONSTANT for Runtime Permission Request
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Member variables for the UI components defined in the layout
    private WebView webView;
    private TextView titleTextView;
    private ImageButton headerIconButton; // 💡 NEW: Member variable for the header icon
    // 💡 NEW: Variable to hold the retrieved username
    private String currentUsername = "Traveler";

    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    // ... (newInstance and onCreate methods remain unchanged)

    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment (assuming the XML is named fragment_home.xml)
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize TextView (Title)
        titleTextView = view.findViewById(R.id.title);

        // 💡 NEW: Initialize ImageButton
        headerIconButton = view.findViewById(R.id.header_icon_button);

        // 🌟 START: Retrieve Username from SharedPreferences
        String username = "Traveler"; // Default to a safe placeholder

        // Get the SharedPreferences only if the fragment is attached to an activity
        if (getActivity() != null) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            // Retrieve the username, using "Traveler" as the default value if not found
            username = sharedPref.getString(KEY_USERNAME, "Traveler");
        }
        // 💡 NEW: Store the retrieved username in a member variable
        currentUsername = username;

        // 🌟 UPDATED: Use the retrieved username to personalize the welcome message
        titleTextView.setText("Assalamu Alaikum, " + currentUsername );
        // 🌟 END: Retrieve and set Username

        // 💡 UPDATED: Set up click listener for the header icon to navigate to user_analytics.class
        headerIconButton.setOnClickListener(v -> {
            // 🚀 NEW: Start the user_analytics Activity
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), user_analytics.class);
                startActivity(intent);
            } else {
                // Fallback: show a Toast message if the Fragment is not attached
                Toast.makeText(getContext(), "Error: Activity not found.", Toast.LENGTH_SHORT).show();
            }
        });


        // 2. Initialize WebView
        webView = view.findViewById(R.id.webview);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // 🚀 START: UNRESTRICTING WEBVIEW SETTINGS 🚀
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // 🚀 END: UNRESTRICTING WEBVIEW SETTINGS 🚀

        // 💡 NEW: Register the JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(getContext()), "Android");

        // Set a WebViewClient and WebChromeClient
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

        // 3. 🚨 NEW LOCATION PERMISSION CHECK & REQUEST 🚨
        if (checkLocationPermission()) {
            // Permission is already granted, load content immediately
            webView.loadUrl("file:///android_asset/home.html");
        } else {
            // Request permission. The loading will happen in the callback.
            requestLocationPermission();
        }

        return view;
    }

    // ---------------------------------------------------------
    // 💡 NEW: JAVASCRIPT INTERFACE CLASS
    // ---------------------------------------------------------

    /**
     * WebAppInterface provides a bridge between Android code and JavaScript in the WebView.
     * It allows JavaScript to call a Java method to retrieve the username.
     */
    private class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Returns the current username to the JavaScript.
         */
        @JavascriptInterface
        public String getUsername() {
            // Use the member variable currentUsername populated in onCreateView
            return currentUsername;
        }
    }

    // ---------------------------------------------------------
    // 💡 LOCATION PERMISSION METHODS
    // ---------------------------------------------------------

    /**
     * Checks if location permission (ACCESS_FINE_LOCATION) is granted.
     */
    private boolean checkLocationPermission() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location permission from the user.
     */
    private void requestLocationPermission() {
        if (getActivity() != null) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Handles the result of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Check if permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted by the user, now load the WebView content
                if (webView != null) {
                    webView.loadUrl("file:///android_asset/home.html");
                }
            } else {
                // Permission denied. Display a message to the user.
                if (titleTextView != null) {
                    titleTextView.setText("Hello, Location Blocked! 🚫");
                }
                if (webView != null) {
                    // Load a small HTML snippet informing the user
                    String errorHtml = "<div style='padding: 20px; color: #E0E7E2; background-color: #213A33; text-align: center;'>" +
                            "<h2>Location Required</h2>" +
                            "<p>Please enable location access in your device settings for the Qibla Compass to function.</p>" +
                            "</div>";
                    webView.loadData(errorHtml, "text/html", "utf-8");
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 💡 WEBCHROMECLIENT
    // ---------------------------------------------------------

    /**
     * Custom WebChromeClient to handle permissions required by web APIs.
     * This grants the web-level permission prompt for Geolocation.
     */
    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // This grants the web page permission, assuming the Android native permission is already granted.
            callback.invoke(origin, true, false);
        }
    }
}