package com.example.islamic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// NOTE: You must have 'admin_useraccount.class', 'admin_profile_account.class',
// 'admin_viewaccount.class', 'admin_event_analytics.class', 'admin_prayer_analytics.class',
// 'admin_data_analytics.class', AND 'admin_sms.class' files in your project
// for this to compile and run correctly.
public class AccountFragment_admin extends Fragment {

    private TextView titleTextView;
    private ImageButton logoutButton;

    // Member variables for the admin menu buttons
    private Button profileButton;
    private Button userAccountButton;
    private Button adminAccountButton;
    private Button eventsButton;
    private Button prayersButton;
    private Button dataAnalyticsButton;
    private Button smsButton;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    // Use the same constants as Login.java
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    // Constant for the simulated loading delay in milliseconds (e.g., 500ms)
    private static final long LOADING_DELAY_MS = 500;

    public AccountFragment_admin() {
        // Required empty public constructor
    }

    public static AccountFragment_admin newInstance(String param1, String param2) {
        AccountFragment_admin fragment = new AccountFragment_admin();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // The layout file name is inferred from the original code
        return inflater.inflate(R.layout.fragment_account_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Header UI components
        titleTextView = view.findViewById(R.id.title);
        logoutButton = view.findViewById(R.id.logout_button);

        // Initialize the admin menu Button components
        profileButton = view.findViewById(R.id.button_profile);
        userAccountButton = view.findViewById(R.id.button_user_account);
        adminAccountButton = view.findViewById(R.id.button_admin_account);
        eventsButton = view.findViewById(R.id.button_events);
        prayersButton = view.findViewById(R.id.button_prayers);
        dataAnalyticsButton = view.findViewById(R.id.button_data_analytics);
        smsButton = view.findViewById(R.id.button_sms);

        // Load the username from SharedPreferences
        String username = getUsername();

        // Display personalized title using saved username
        // Based on the remembered instruction: username is derived from the signup code.
        titleTextView.setText("Account (" + username + ")");

        // Set up click listeners for the buttons
        setupButtonListeners();

        // Logout button action
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    /**
     * Sets up the click listeners for the administration buttons.
     */
    private void setupButtonListeners() {

        // Profile Button - Navigates to admin_profile_account.class
        profileButton.setOnClickListener(v -> handleProfileNavigation());

        // User Account Button - Navigates to admin_useraccount.class
        userAccountButton.setOnClickListener(v -> handleNavigation(
                admin_useraccount.class,
                "Loading User Account Management...",
                null
        ));

        // Admin Account Button - Navigates to admin_viewaccount.class
        adminAccountButton.setOnClickListener(v -> handleAdminAccountNavigation());

        // Events Button - Navigates to admin_event_analytics.class
        eventsButton.setOnClickListener(v -> handleNavigation(
                admin_event_analytics.class,
                "Loading Event Analytics...",
                null
        ));

        // Prayers Button - Navigates to admin_prayer_analytics.class
        prayersButton.setOnClickListener(v -> handleNavigation(
                admin_prayer_analytics.class,
                "Loading Prayers Management...",
                null
        ));

        // Data Analytics Button - Navigates to admin_data_analytics.class
        dataAnalyticsButton.setOnClickListener(v -> handleNavigation(
                admin_data_analytics.class,
                "Loading Data Analytics Dashboard...",
                null
        ));

        // ✅ SMS Button - MODIFIED to navigate to admin_sms.class
        smsButton.setOnClickListener(v -> handleNavigation(
                admin_sms.class, // <-- Target class set to admin_sms.class
                "Loading SMS Communication Tool...",
                null // Target class is set, no placeholder needed
        ));
    }

    /**
     * Handles navigation to admin_profile_account, passing the current username.
     */
    private void handleProfileNavigation() {
        final String username = getUsername(); // Retrieve the username
        final String loadingMessage = "Loading Admin Profile...";

        // 1. Show the loading message immediately
        Toast.makeText(getContext(), loadingMessage, Toast.LENGTH_SHORT).show();

        // 2. Schedule the navigation after a delay
        new Handler().postDelayed(() -> {
            if (getActivity() == null) {
                return; // Guard against fragment being detached
            }

            // Navigate to admin_profile_account.class and pass the username
            Intent intent = new Intent(getActivity(), admin_profile_account.class);
            // Pass the username using the public static key from admin_profile_account.java
            intent.putExtra(admin_profile_account.EXTRA_USERNAME, username);
            startActivity(intent);

        }, LOADING_DELAY_MS);
    }

    /**
     * Shows a loading message and then navigates to the target Activity after a short delay.
     * @param targetClass The class of the Activity to start.
     * @param loadingMessage The message to display while loading.
     * @param toastMessage The message to display if targetClass is null (for TODOs).
     */
    private void handleNavigation(@Nullable final Class<?> targetClass, @NonNull String loadingMessage, @Nullable String toastMessage) {
        // 1. Show the loading message immediately
        Toast.makeText(getContext(), loadingMessage, Toast.LENGTH_SHORT).show();

        // 2. Schedule the navigation after a delay
        new Handler().postDelayed(() -> {
            if (getActivity() == null) {
                return; // Guard against fragment being detached
            }

            if (targetClass != null) {
                // Navigate to the specified class
                Intent intent = new Intent(getActivity(), targetClass);
                startActivity(intent);
            } else if (toastMessage != null) {
                // For TODO items: Show a final Toast message instead of navigating
                Toast.makeText(getContext(), "TODO: Navigate to " + toastMessage, Toast.LENGTH_SHORT).show();
            }
        }, LOADING_DELAY_MS);
    }

    /**
     * Handles navigation to admin_viewaccount, passing the current username.
     */
    private void handleAdminAccountNavigation() {
        final String username = getUsername(); // Retrieve the username
        final String loadingMessage = "Loading Admin Account Settings...";

        // 1. Show the loading message immediately
        Toast.makeText(getContext(), loadingMessage, Toast.LENGTH_SHORT).show();

        // 2. Schedule the navigation after a delay
        new Handler().postDelayed(() -> {
            if (getActivity() == null) {
                return; // Guard against fragment being detached
            }

            // Navigate to admin_viewaccount.class and pass the username
            Intent intent = new Intent(getActivity(), admin_viewaccount.class);
            // Pass the username using the public static key from admin_viewaccount.java
            // NOTE: This assumes 'admin_viewaccount.class' has 'EXTRA_USERNAME' defined.
            intent.putExtra(admin_viewaccount.EXTRA_USERNAME, username);
            startActivity(intent);

        }, LOADING_DELAY_MS);
    }

    private String getUsername() {
        // Use requireActivity() to safely get the Activity
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_FILE_NAME, getContext().MODE_PRIVATE);
        // The saved username is derived from the signup code.
        return prefs.getString(KEY_USERNAME, "Admin");
    }

    private void handleLogout() {
        // Clear the same SharedPreferences used in Login.java
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_FILE_NAME, getContext().MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Redirect to Login and clear the back stack
        Intent intent = new Intent(getActivity(), Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}