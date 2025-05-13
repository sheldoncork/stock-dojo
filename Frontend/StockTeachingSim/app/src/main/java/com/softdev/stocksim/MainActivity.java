package com.softdev.stocksim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.softdev.stocksim.data.UserPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  Main activity for the application. Handles primary navigation, theme management,
 *  and global UI structure.
 * @author Blake Nelson
 */
public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private BottomNavigationView bottomNav;
    private AppBarConfiguration appBarConfiguration;
    private boolean doubleBackToExitPressedOnce = false;
    private static final int BACK_PRESS_INTERVAL = 2000;

    /**
     * Initializes the activity, sets up navigation, and applies theme/contrast settings.
     *
     * @param savedInstanceState State data from a previous instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply contrast theme before super.onCreate()
        UserPreferences userPreferences = UserPreferences.getInstance(this);

        // Set the theme
        AppCompatDelegate.setDefaultNightMode(userPreferences.getTheme());

        // Apply contrast overlay if needed
        int contrastTheme = userPreferences.getContrast();
        if (contrastTheme != R.style.Theme_StockTeachingSim) {
            getTheme().applyStyle(contrastTheme, true);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation();
        setupBackPressedCallback();
    }

    /**
     * Sets up the navigation controller and bottom navigation.
     */
    private void setupNavigation() {
        // Setup NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            throw new IllegalStateException("NavHostFragment not found");
        }

        // Setup Bottom Navigation
        bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Define top-level destinations (all bottom nav destinations)
        Set<Integer> topLevelDestinations = new HashSet<>(Arrays.asList(
                R.id.homeFragment,
                R.id.searchFragment,
                R.id.classroomContainerFragment,
                R.id.settingsFragment
        ));

        // Setup TopAppBar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        // Configure AppBar with top-level destinations
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .build();

        // Setup the ActionBar with NavController and AppBarConfiguration
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Handle destination changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Title will be automatically updated by NavigationUI
            // Back button visibility is handled by AppBarConfiguration

            // Ensure bottom nav stays visible
            bottomNav.setVisibility(View.VISIBLE);
        });

        // Handle bottom nav reselection
        bottomNav.setOnItemReselectedListener(item -> {
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null && currentDestination.getId() == item.getItemId()) {
                // Pop to the start of the current navigation graph
                navController.popBackStack(item.getItemId(), false);
            }
        });
    }

    /**
     * Sets up the back press callback to handle back navigation.
     */
    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavDestination currentDestination = navController.getCurrentDestination();
                if (currentDestination != null) {
                    handleBackNavigation(currentDestination.getId());
                }
            }
        });
    }

    /**
     * Handles back navigation based on the current destination.
     *
     * @param currentDestinationId The ID of the current destination
     */
    private void handleBackNavigation(int currentDestinationId) {
        if (isMainDestination(currentDestinationId)) {
            if (currentDestinationId == R.id.homeFragment) {
                // At home fragment, handle double back to exit
                handleDoubleBackExit();
            } else {
                // At other main destinations, go to home
                navController.navigate(R.id.homeFragment);
            }
        } else {
            // Handle back navigation for sub-screens
            if (!navController.navigateUp()) {
                // If navigate up fails, go to the parent tab
                navigateToParentTab(currentDestinationId);
            }
        }
    }

    /**
     * Navigates to the parent tab based on the current destination.
     *
     * @param currentDestinationId The ID of the current destination
     */
    private void navigateToParentTab(int currentDestinationId) {
        // Determine parent tab based on current destination
        if (currentDestinationId == R.id.portfolioManagerFragment ||
                currentDestinationId == R.id.resultsFragment) {
            navController.navigate(R.id.homeFragment);
        } else if (currentDestinationId == R.id.resultsFragment) {
            navController.navigate(R.id.searchFragment);
        }
        // Add more cases as needed
    }

    /**
     * Checks if the destination is a main destination.
     *
     * @param destinationId The ID of the destination
     * @return True if the destination is a main destination
     */
    private boolean isMainDestination(int destinationId) {
        return destinationId == R.id.homeFragment ||
                destinationId == R.id.searchFragment ||
                destinationId == R.id.classroomContainerFragment ||
                destinationId == R.id.settingsFragment;
    }

    /**
     * Handles the back press to exit the app.
     */
    private void handleDoubleBackExit() {
        if (doubleBackToExitPressedOnce) {
            finish();
        } else {
            doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> doubleBackToExitPressedOnce = false,
                    BACK_PRESS_INTERVAL
            );
        }
    }

    /**
     * Applies theme and contrast changes at runtime.
     * Can update either theme, contrast, or both.
     *
     * @param themeMode New theme mode to apply (null to keep current)
     * @param contrastTheme New contrast theme to apply
     */
    public void applyThemeAndContrast(@Nullable Integer themeMode, int contrastTheme) {
        if (themeMode != null) {
            // Update day/night mode if it was changed
            AppCompatDelegate.setDefaultNightMode(themeMode);
        }

        // Apply contrast overlay
        if (contrastTheme != R.style.Theme_StockTeachingSim) {
            getTheme().applyStyle(contrastTheme, true);
        }

        // Recreate to apply changes
        recreate();
    }


    /**
     * Handles "up" navigation support.
     * Integrates with NavigationUI for consistent navigation behavior.
     *
     * @return true if navigation was handled, false otherwise
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Creates the options menu in the top app bar.
     *
     * @param menu Menu to inflate into
     * @return true to display the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    /**
     * Handles selection of menu items in the top app bar.
     * Integrates with NavigationUI for navigation actions.
     *
     * @param item Selected menu item
     * @return true if the item selection was handled
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }
}