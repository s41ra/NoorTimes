package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

// Imports for Navigation Component setup
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class admindashboard extends AppCompatActivity {

    private String username;
    private TextView welcomeTextView; // Optional: if you want to show username somewhere

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        // Retrieve the username from the Intent
        username = getIntent().getStringExtra("username");

        // Optional: show welcome message
        welcomeTextView = findViewById(R.id.welcomeTextView); // make sure your layout has this TextView
        if (welcomeTextView != null) {
            if (username != null && !username.isEmpty()) {
                welcomeTextView.setText("Welcome, " + username + "!");
            } else {
                welcomeTextView.setText("Welcome, Admin!");
            }
        }

        // Navigation setup
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }
}
