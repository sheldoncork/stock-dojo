package com.softdev.stocksim.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.softdev.stocksim.R;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.api.VolleySingleton;

/**
 * Manages user preferences and authentication state using SharedPreferences.
 * Implements the Singleton pattern to ensure a single instance manages all preferences.
 *
 * @author Blake Nelson
 */
public class UserPreferences {
    private final SharedPreferences preferences;
    private static UserPreferences instance;
    private final Context context;

    /**
     * Private constructor to prevent instantiation of the UserPreferences class.
     *
     * @param context The application context.
     */
    private UserPreferences(Context context) {
        this.context = context.getApplicationContext(); // Stores the application context
        preferences = context.getSharedPreferences(AppConfig.Prefs.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the singleton instance of the UserPreferences class.
     *
     * @param context The application context.
     * @return The singleton instance of the UserPreferences class.
     */
    public static synchronized UserPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new UserPreferences(context);
        }
        return instance;
    }

    /**
     * Saves user's authentication data and marks them as logged in.
     * Stores all user-related data in SharedPreferences.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @param userType The type of the user.
     */
    public void saveUserData(String username, String password, String userType, String email) {
        preferences.edit()
                .putString(AppConfig.Prefs.KEY_USERNAME, username)
                .putString(AppConfig.Prefs.KEY_PASSWORD, password)
                .putString(AppConfig.Prefs.KEY_USER_TYPE, userType)
                .putString(AppConfig.Prefs.EMAIL, email)
                .putBoolean(AppConfig.Prefs.KEY_IS_LOGGED_IN, true)
                .apply();
    }

    /**
     * Get the username of the logged-in user.
     *
     * @return The username of the logged-in user, or null if not logged in.
     */
    public String getUsername() {
        return preferences.getString(AppConfig.Prefs.KEY_USERNAME, null);
    }

    /**
     * Get the password of the logged-in user.
     *
     * @return The password of the logged-in user, or null if not logged in.
     */
    public String getPassword() {
        return preferences.getString(AppConfig.Prefs.KEY_PASSWORD, null);
    }

    /**
     * Get the type of the logged-in user.
     *
     * @return The type of the logged-in user, or null if not logged in.
     */
    public String getUserType() {
        return preferences.getString(AppConfig.Prefs.KEY_USER_TYPE, null);
    }

    /**
     * Get the email of the logged-in user.
     *
     * @return The email of the logged-in user, or null if not logged in.
     */
    public String getEmail() {
        return preferences.getString(AppConfig.Prefs.EMAIL, null);
    }

    /**
     * Check if the user is logged in.
     *
     * @return the login status of the user
     */
    public boolean isLoggedIn() {
        return preferences.getBoolean(AppConfig.Prefs.KEY_IS_LOGGED_IN, false);
    }

    /**
     * Save and apply the selected theme mode
     *
     * @param themeMode The theme mode to apply (AppCompatDelegate.MODE_NIGHT_*)
     */
    public void setTheme(int themeMode) {
        preferences.edit()
                .putInt(AppConfig.Prefs.KEY_THEME_MODE, themeMode)
                .apply();
    }

    /**
     * Get the current theme mode
     *
     * @return The current theme mode, defaults to system default if not set
     */
    public int getTheme() {
            return preferences.getInt(AppConfig.Prefs.KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Save and apply the selected contrast mode
     *
     * @param contrastMode The contrast mode to apply (R.style.ContrastMode*)
     */
    public void setContrast(int contrastMode) {
        preferences.edit()
                .putInt(AppConfig.Prefs.KEY_CONTRAST_MODE, contrastMode)
                .apply();
    }

    /**
     * Get the current contrast level
     *
     * @return The current contrast level, defaults to normal if not set
     */
    public int getContrast() {
        return preferences.getInt(AppConfig.Prefs.KEY_CONTRAST_MODE, R.style.Theme_StockTeachingSim);
    }

    /**
     * Logout the user by clearing their data and cookies.
     */
    public void logout() {
        // Clear user data and keep theme and contrast
        preferences.edit()
                .remove(AppConfig.Prefs.KEY_USERNAME)
                .remove(AppConfig.Prefs.KEY_PASSWORD)
                .remove(AppConfig.Prefs.KEY_USER_TYPE)
                .putBoolean(AppConfig.Prefs.KEY_IS_LOGGED_IN, false)
                .apply();

        // Clear cookies on logout
        VolleySingleton.getInstance(context).clearCookies();
    }
}