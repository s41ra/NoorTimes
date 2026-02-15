package com.example.islamic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
// import android.widget.ImageButton; // REMOVED: No longer needed for notification icon
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * A simple {@link Fragment} subclass for displaying orders,
 * featuring a header with a personalized welcome and a WebView for content.
 */
public class OrdersFragment extends Fragment {

    private static final String TAG = "OrdersFragment";

    // CONSTANTS for SharedPreferences file name and key
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    private static final String ARG_SIGNUP_CODE = "signup_code";

    private String mSignupCode;
    private TextView titleTextView;
    private WebView webView;
    // private ImageButton notificationIcon; // REMOVED: Declaration for the notification icon

    // Inner class for the JavaScript Interface
    private static class WebAppInterface {
        private String username;

        /** Instantiate the interface and set the username */
        WebAppInterface(String username) {
            this.username = username;
        }

        /** Get the username for the web page */
        @JavascriptInterface
        public String getUsername() {
            return username;
        }

        // Returns a derived user ID for comment tracking in HTML
        @JavascriptInterface
        public String getUserId() {
            return "user_" + username.toLowerCase().replaceAll("[^a-z0-9]", "");
        }
    }

    public OrdersFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided signup code.
     *
     * @param signupCode The signup code used to derive the username.
     * @return A new instance of fragment OrdersFragment.
     */
    public static OrdersFragment newInstance(String signupCode) {
        OrdersFragment fragment = new OrdersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SIGNUP_CODE, signupCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSignupCode = getArguments().getString(ARG_SIGNUP_CODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        // 1. Initialize Views
        titleTextView = view.findViewById(R.id.title);
        webView = view.findViewById(R.id.webview);

        // REMOVED: Initialization and set click listener for the notification icon

        // 2. Set the Username
        String currentUsername = "Guest";
        if (mSignupCode != null && !mSignupCode.isEmpty()) {
            currentUsername = mSignupCode; // Prioritize signup code (as per user instruction)
        } else {
            if (getContext() != null) {
                SharedPreferences sharedPref = getContext().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
                currentUsername = sharedPref.getString(KEY_USERNAME, "Guest");
            }
        }

        // Apply the "Hello, [username]!" format only if you uncomment the line below.
        // Keeping the 'Events' title as per your previous code for now.
        titleTextView.setText("Events" );
        // titleTextView.setText("Hello, " + currentUsername );

        // 3. Configure and Load WebView
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();

            // Use the CustomWebViewClient to handle links
            webView.setWebViewClient(new CustomWebViewClient());

            // 1. Enable JavaScript
            webSettings.setJavaScriptEnabled(true);

            // 2. Enable DOM storage (localStorage/sessionStorage)
            webSettings.setDomStorageEnabled(true);

            // 3. Enable Database/Caching for better performance on re-loads
            webSettings.setDatabaseEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

            // 4. Enable support for the 'viewport' HTML meta tag
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

            // Add the JavaScript Interface
            webView.addJavascriptInterface(new WebAppInterface(currentUsername), "Android");

            String ordersUrl = "file:///android_asset/events_witharchived.html";
            webView.loadUrl(ordersUrl);
        }

        return view;
    }

    // ---------------------------------------------------------------------
    // Custom WebViewClient to force specific links (like Google Maps) to open in the native app
    // ---------------------------------------------------------------------
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 1. Check if the URL is a Google Maps link or a Geo URI
            if (url.contains("maps.google.com") || url.contains("google.com/maps") || url.startsWith("geo:")) {
                Log.d(TAG, "Attempting to open Maps link externally: " + url);

                try {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                    // Try 1: Force opening the Google Maps application by setting the package name.
                    final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
                    intent.setPackage(GOOGLE_MAPS_PACKAGE);

                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        // Found the specific Google Maps app, launch it directly.
                        startActivity(intent);
                        return true; // Indicate that we handled the URL with the native app
                    } else {
                        // Try 2 (Fallback): Google Maps app not found or cannot resolve.
                        Log.d(TAG, "Google Maps app not found or cannot resolve. Trying generic map intent with chooser.");

                        Intent genericIntent = new Intent(Intent.ACTION_VIEW, uri);
                        Intent chooser = Intent.createChooser(genericIntent, "View Map with...");

                        if (getActivity() != null && chooser.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(chooser);
                            return true;
                        }

                        // Final fallback: If no suitable app is found even with the chooser, let WebView load the URL (the website).
                        Log.d(TAG, "No suitable app found to handle the Maps Intent. Letting WebView handle.");
                        return false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Google Maps link:", e);
                }
            }

            // 2. Handle other specific native schemes (phone, email, SMS)
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                        return true; // Link handled by native app
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening specific scheme link:", e);
                }
            }

            // 3. Handle all other external http/https links by opening them in the default browser/app
            if (url.startsWith("http://") || url.startsWith("https://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                        return true; // Link handled by external app
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening generic external link:", e);
                }
            }

            // 4. For all other URLs (e.g., internal assets), let WebView load them.
            return false;
        }
    }

    /**
     * Optional: Handle back button presses within the WebView.
     */
    public boolean canGoBackInWebView() {
        return webView != null && webView.canGoBack();
    }

    public void goBackInWebView() {
        if (webView != null) {
            webView.goBack();
        }
    }
}