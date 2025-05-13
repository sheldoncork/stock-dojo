package com.softdev.stocksim.utils;

/**
 * Provides centralized configuration constants for the application.
 * This utility class contains all configuration values and cannot be instantiated.
 *
 * @author Blake Nelson
 */
public final class AppConfig {

    /**
     * Private constructor to prevent instantiation of the AppConfig class.
     */
    private AppConfig() {
        throw new AssertionError("AppConfig class should not be instantiated");
    }

    // API and Network Constants
    public static final String BASE_URL = "";
    public static final String CLASSROOM_CHAT_WEBSOCKET_URL = "wss://" + BASE_URL + "/ws/classroom";
    public static final String SEARCH_WEBSOCKET_URL = "wss://" + BASE_URL + "/ws/search";
    public static final String PORTFOLIO_WEBSOCKET_URL = "wss://" + BASE_URL + "/ws/portfolio";
    public static final boolean DEBUG_MODE = false;

    // Shared Preferences Constants
    public static final class Prefs {
        public static final String PREF_NAME = "UserPrefs";
        public static final String EMAIL = "email";
        public static final String KEY_USERNAME = "username";
        public static final String KEY_USER_TYPE = "userType";
        public static final String KEY_PASSWORD = "password";
        public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
        public static final String KEY_THEME_MODE = "theme_mode";
        public static final String KEY_CONTRAST_MODE = "contrast_level";
        public static final String COOKIE_PREFS = "CookiePreferences";
    }

    // Constants for UserType
    public static final class UserType {

        public static final String STUDENT = "STUDENT";
        public static final String TEACHER = "TEACHER";
        public static final String STANDARD = "STANDARD";
    }
}