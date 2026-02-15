package com.example.islamic;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class userdashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the content view to your layout file, R.layout.activity_userdashboard
        setContentView(R.layout.activity_userdashboard);

        // 1. Find the NavHostFragment that is hosting your fragments
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_container);

        // Check if the fragment was found
        if (navHostFragment != null) {
            // 2. Get the NavController from the NavHostFragment
            NavController navController = navHostFragment.getNavController();

            // 3. Find the BottomNavigationView
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

            // 4. Set up the BottomNavigationView with the NavController
            // This links the menu items in bottom_nav_menu (referenced in your XML)
            // to the destination IDs in your nav_graph.xml.
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }
}
