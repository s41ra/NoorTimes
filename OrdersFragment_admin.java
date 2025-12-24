package com.example.islamic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

public class OrdersFragment_admin extends Fragment {

    private TextView titleTextView;
    private WebView webView;
    private ImageView eventIconImageView;

    // WebView file upload variables
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;

    // SharedPreferences constants (same as AccountFragment_admin)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    public OrdersFragment_admin() {
        // Required empty public constructor
    }

    public static OrdersFragment_admin newInstance() {
        return new OrdersFragment_admin();
    }

    // --- New Method: Register the Activity Result Launcher ---
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Register the launcher for handling the file selection intent result
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if we have a callback waiting and the result is OK
                    if (filePathCallback != null) {
                        Uri[] results = null;

                        if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                            String dataString = result.getData().getDataString();
                            if (dataString != null) {
                                results = new Uri[]{Uri.parse(dataString)};
                            } else if (result.getData().getClipData() != null) {
                                // Handle multiple selection (if allowed by the intent)
                                int count = result.getData().getClipData().getItemCount();
                                results = new Uri[count];
                                for (int i = 0; i < count; i++) {
                                    results[i] = result.getData().getClipData().getItemAt(i).getUri();
                                }
                            }
                        }

                        // Pass the results back to the WebView
                        filePathCallback.onReceiveValue(results);
                        filePathCallback = null; // Clear the callback
                    }
                }
        );
    }
    // --------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_orders_admin, container, false);

        // Bind views
        titleTextView = view.findViewById(R.id.title);
        webView = view.findViewById(R.id.webview);
        eventIconImageView = view.findViewById(R.id.event_icon);

        // Set the click listener for the new event icon
        eventIconImageView.setOnClickListener(v -> {
            // Create an Intent to navigate to admin_viewevents.class
            Intent intent = new Intent(getContext(), admin_viewevents.class);
            startActivity(intent);

            // Optional: You can remove the Toast now that the navigation is implemented
            // Toast.makeText(getContext(), "Event Icon Clicked! (Placeholder Action)", Toast.LENGTH_SHORT).show();
        });


        // Get username from SharedPreferences
        String username = getUsername();
        //     titleTextView.setText("Hello, " + username + "!");

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // For better page support

        // **Crucial for file upload:** Set to true to allow file access from content
        webSettings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());

        // --- Modified: Custom WebChromeClient for File Upload ---
        webView.setWebChromeClient(new WebChromeClient() {
            // This is the method we must override to handle file selection
            @Override
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                // If there's an existing callback, cancel it
                if (OrdersFragment_admin.this.filePathCallback != null) {
                    OrdersFragment_admin.this.filePathCallback.onReceiveValue(null);
                }

                // Store the new callback
                OrdersFragment_admin.this.filePathCallback = filePathCallback;

                // Create and configure the intent to pick files (specifically images)
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);

                // Use the accept types from the HTML input, or default to images
                String[] acceptTypes = fileChooserParams.getAcceptTypes();
                if (acceptTypes != null && acceptTypes.length > 0 && !"*/*".equals(acceptTypes[0])) {
                    contentSelectionIntent.setType(acceptTypes[0]);
                } else {
                    contentSelectionIntent.setType("image/*"); // Default to images
                }

                // If multiple selection is allowed in HTML, set the intent flag
                if (fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                // Start the activity for result
                try {
                    fileChooserLauncher.launch(contentSelectionIntent);
                } catch (android.content.ActivityNotFoundException e) {
                    // Handle case where no application can handle the intent
                    OrdersFragment_admin.this.filePathCallback = null;
                    return false;
                }

                return true;
            }
        });
        // --------------------------------------------------------

        webView.loadUrl("file:///android_asset/admin_postevent.html"); // Replace with your URL

        return view;
    }

    // Helper method to get the username from SharedPreferences
    private String getUsername() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREF_FILE_NAME, getContext().MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "Admin");
    }
}