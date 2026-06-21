package com.mohammado_ibraheima_obaydas.geomemo;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class GeoMemoApplication extends Application {

    private static final String PREFS_NAME = "geomemo_prefs";
    private static final String KEY_APP_THEME = "app_theme";

    @Override
    public void onCreate() {
        super.onCreate();

        // Load saved theme and apply it immediately
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String themeStr = prefs.getString(KEY_APP_THEME, "system");
        applyTheme(themeStr);
    }

    public static void applyTheme(String themeStr) {
        int nightMode;
        switch (themeStr) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "system":
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
