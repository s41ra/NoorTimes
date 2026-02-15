package com.example.islamic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent; // 1. IMPORT Intent
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Assuming ClockActivity is the class name for clock.class
// NOTE: You must create a ClockActivity class for this to work.
// import com.example.islamic.ClockActivity; // Import if ClockActivity is in a different package

public class CartFragment extends Fragment {

    private static final String WEBVIEW_URL = "file:///android_asset/prayer.html";
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private String currentUsername = "Guest";

    public CartFragment() {
        // Required empty public constructor
    }

    public static CartFragment newInstance(String param1, String param2) {
        CartFragment fragment = new CartFragment();
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

        // Load username early in onCreate
        Context context = getContext();
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            // NOTE: Using saved user information. The username is derived from the provided signup code.
            // However, the current code loads from SharedPreferences using KEY_USERNAME.
            currentUsername = sharedPref.getString(KEY_USERNAME, "Guest");
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Ensure this layout is fragment_cart.xml where the ImageView was added
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the WebView and configure it (omitted for brevity, no change here)
        WebView webView = view.findViewById(R.id.webview);
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();

            // 🚀 **Performance Enhancements for Faster Loading** 🚀
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

            // Add JavaScript Interface to pass username to HTML
            webView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

            // Handle links internally
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // Inject username after page loads
                    view.evaluateJavascript(
                            "javascript:if(typeof setUsername === 'function') { setUsername('" + currentUsername + "'); }",
                            null
                    );
                }
            });

            // Load the specified URL
            webView.loadUrl(WEBVIEW_URL);
        }

        // 2. Find the TextView and set the personalized text (omitted for brevity, no change here)
        TextView titleTextView = view.findViewById(R.id.title);
        if (titleTextView != null) {
            String personalizedTitle = "Prayer Time";
            titleTextView.setText(personalizedTitle);
        }

        // 3. Find the Clock Icon and set its click listener
        ImageView clockIcon = view.findViewById(R.id.clock_icon);
        if (clockIcon != null) {
            clockIcon.setOnClickListener(v -> {
                Context context = getContext();
                if (context != null) {
                    // Show "loading message" Toast ⏳
                    Toast.makeText(
                            context,
                            "Please wait...",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Intent to navigate to ClockActivity
                    // **You must create a class named ClockActivity in your project**
                    Intent intent = new Intent(context, clock.class);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * JavaScript Interface class to expose Android methods to JavaScript
     */
    public class WebAppInterface {
        @JavascriptInterface
        public String getUsername() {
            return currentUsername;
        }
    }
}