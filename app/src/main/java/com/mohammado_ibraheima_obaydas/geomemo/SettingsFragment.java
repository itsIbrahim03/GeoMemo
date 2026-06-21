package com.mohammado_ibraheima_obaydas.geomemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.slider.Slider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "geomemo_prefs";
    private static final String KEY_DETECTION_RADIUS = "detection_radius";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_APP_THEME = "app_theme";

    private Slider sliderRadius;
    private TextView textRadiusValue;
    private Spinner spinnerTheme;
    private SwitchCompat switchNotifications;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sliderRadius = view.findViewById(R.id.slider_radius);
        textRadiusValue = view.findViewById(R.id.text_radius_value);
        spinnerTheme = view.findViewById(R.id.spinner_theme);
        switchNotifications = view.findViewById(R.id.switch_notifications);

        // Load SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int savedRadius = prefs.getInt(KEY_DETECTION_RADIUS, 100);
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        String savedTheme = prefs.getString(KEY_APP_THEME, "system");

        // Clamp savedRadius between 50 and 300 and snap to steps of 50
        int clampedRadius = Math.max(50, Math.min(300, savedRadius));
        clampedRadius = ((clampedRadius + 25) / 50) * 50;
        sliderRadius.setValue(clampedRadius);
        textRadiusValue.setText(clampedRadius + "m");

        // Set spinner selection based on saved theme
        int themePosition = 2; // default system (index 2)
        if ("light".equals(savedTheme)) {
            themePosition = 0;
        } else if ("dark".equals(savedTheme)) {
            themePosition = 1;
        }
        spinnerTheme.setSelection(themePosition);

        // Set switch state
        switchNotifications.setChecked(notificationsEnabled);

        // Radius Slider listener
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            int selectedRadius = (int) value;
            textRadiusValue.setText(selectedRadius + "m");
            prefs.edit().putInt(KEY_DETECTION_RADIUS, selectedRadius).apply();
        });

        // Theme Spinner listener
        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                String themeStr;
                switch (position) {
                    case 0:
                        themeStr = "light";
                        break;
                    case 1:
                        themeStr = "dark";
                        break;
                    case 2:
                    default:
                        themeStr = "system";
                        break;
                }

                String currentTheme = prefs.getString(KEY_APP_THEME, "system");
                if (!themeStr.equals(currentTheme)) {
                    prefs.edit().putString(KEY_APP_THEME, themeStr).apply();
                    // Apply theme immediately
                    GeoMemoApplication.applyTheme(themeStr);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No action needed
            }
        });

        // Switch listener
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            
            android.content.Intent serviceIntent = new android.content.Intent(requireContext(), LocationService.class);
            if (isChecked && androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.content.ContextCompat.startForegroundService(requireContext(), serviceIntent);
            } else {
                requireContext().stopService(serviceIntent);
            }
        });

        return view;
    }
}
