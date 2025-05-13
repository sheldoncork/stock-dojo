package com.softdev.stocksim.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.softdev.stocksim.utils.AppConfig;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Singleton class for Volley request queue.
 */
public class VolleySingleton {

    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private static Context ctx;
    private final CookieManager cookieManager;

    private VolleySingleton(Context context) {
        ctx = context;
        handleSSL();
        requestQueue = getRequestQueue();

        // Initialize cookie manager with stored cookies
        cookieManager = new CookieManager(new SharedPreferencesCookieStore(context), CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    /**
     * Handles SSL connections for the Volley request queue.
     * Needed for HTTPS connections.
     */
    private void handleSSL(){
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());

            HurlStack hurlStack = new HurlStack(null, sc.getSocketFactory());
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext(), hurlStack);

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void clearCookies() {
        SharedPreferences prefs = ctx.getSharedPreferences(AppConfig.Prefs.COOKIE_PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        cookieManager.getCookieStore().removeAll();
    }

    // Simple cookie store that uses SharedPreferences
    private static class SharedPreferencesCookieStore implements java.net.CookieStore {
        private final SharedPreferences prefs;

        SharedPreferencesCookieStore(Context context) {
            prefs = context.getSharedPreferences(AppConfig.Prefs.COOKIE_PREFS, Context.MODE_PRIVATE);
            // Load any stored cookies
            loadCookies();
        }

        private void loadCookies() {
            // Implementation of loading cookies from SharedPreferences
            String cookieString = prefs.getString("cookies", null);
            if (cookieString != null) {
                try {
                    HttpCookie cookie = HttpCookie.parse(cookieString).get(0);
                    add(null, cookie);
                } catch (Exception ignored) {
                    // Handle parse errors silently
                }
            }
        }

        @Override
        public void add(java.net.URI uri, HttpCookie cookie) {
            // Save cookie to SharedPreferences
            prefs.edit().putString("cookies", cookie.toString()).apply();
        }

        @Override
        public List<HttpCookie> get(java.net.URI uri) {
            List<HttpCookie> cookies = new ArrayList<>();
            String cookieString = prefs.getString("cookies", null);
            if (cookieString != null) {
                try {
                    cookies.add(HttpCookie.parse(cookieString).get(0));
                } catch (Exception ignored) {}
            }
            return cookies;
        }

        @Override
        public List<HttpCookie> getCookies() {
            return get(null);
        }

        @Override
        public List<java.net.URI> getURIs() {
            return new ArrayList<>();
        }

        @Override
        public boolean remove(java.net.URI uri, HttpCookie cookie) {
            prefs.edit().remove("cookies").apply();
            return true;
        }

        @Override
        public boolean removeAll() {
            prefs.edit().clear().apply();
            return true;
        }
    }
}