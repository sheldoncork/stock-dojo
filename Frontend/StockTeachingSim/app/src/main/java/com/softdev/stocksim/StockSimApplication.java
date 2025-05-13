package com.softdev.stocksim;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import com.softdev.stocksim.data.UserPreferences;

/**
 * Custom Application class that handles application-wide initialization.
 * Responsible for setting up global theme and contrast preferences before
 *
 * @author Blake Nelson
 */
public class StockSimApplication extends Application {
    @Override
    public void onCreate() {

        // Initialize theme before any activity starts
        UserPreferences userPreferences = UserPreferences.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(userPreferences.getTheme());
        getTheme().applyStyle(userPreferences.getContrast(), true);

        super.onCreate();
    }
}