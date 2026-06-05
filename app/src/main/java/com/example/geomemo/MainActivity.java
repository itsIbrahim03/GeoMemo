package com.example.geomemo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import android.location.Address;
import android.location.Geocoder;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener, MemoAdapter.OnMemoClickListener {

    private static final int LOCATION_PERMISSION_REQUEST = 200;
    private static final String PREFS_NAME = "geomemo_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MemoDBHelper dbHelper;
    private int selectedItemId = R.id.nav_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup ActionBarDrawerToggle (3 horizontal lines)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize database helper
        dbHelper = MemoDBHelper.getInstance(this);

        // Restore state or set default
        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt("selected_item_id", R.id.nav_map);
        } else {
            selectedItemId = R.id.nav_map;
            // Load MapFragment by default
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment(), "map")
                    .commit();
        }

        // Highlight selected menu item and set title
        MenuItem menuItem = navigationView.getMenu().findItem(selectedItemId);
        if (menuItem != null) {
            menuItem.setChecked(true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(menuItem.getTitle());
            }
        }

        // Request permissions (Location and Notifications for Android 13+)
        java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add("android.permission.POST_NOTIFICATIONS");
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startLocationServiceIfNeeded();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_item_id", selectedItemId);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        selectedItemId = id;
        item.setChecked(true);

        Fragment fragment = null;
        String tag = null;

        if (id == R.id.nav_map) {
            fragment = new MapFragment();
            tag = "map";
        } else if (id == R.id.nav_notes) {
            fragment = new NotesFragment();
            tag = "notes";
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "settings";
        } else if (id == R.id.nav_about) {
            fragment = new AboutFragment();
            tag = "about";
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(item.getTitle());
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass result down to the currently loaded fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            currentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMemoClick(Memo memo) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_memo_details, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogDate = dialogView.findViewById(R.id.dialog_date);
        TextView dialogDescription = dialogView.findViewById(R.id.dialog_description);
        TextView dialogCoordinates = dialogView.findViewById(R.id.dialog_coordinates);
        MapView dialogMapView = dialogView.findViewById(R.id.dialog_map_view);

        dialogTitle.setText(memo.getTitle());
        dialogDate.setText("Created: " + memo.getCreatedAt());

        String desc = memo.getText();
        if (desc == null || desc.trim().isEmpty()) {
            dialogDescription.setText("No description provided.");
            dialogDescription.setTypeface(null, android.graphics.Typeface.ITALIC);
        } else {
            dialogDescription.setText(desc);
        }

        dialogCoordinates.setText("Location: Loading...");
        double latitude = memo.getLatitude();
        double longitude = memo.getLongitude();
        new Thread(() -> {
            String address = getAddressFromLocation(latitude, longitude);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    dialogCoordinates.setText("Location: " + address);
                }
            });
        }).start();

        // Setup Map inside dialog with OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        dialogMapView.setMultiTouchControls(true);
        dialogMapView.setTileSource(TileSourceFactory.MAPNIK);

        GeoPoint position = new GeoPoint(memo.getLatitude(), memo.getLongitude());
        dialogMapView.getController().setZoom(16.0);
        dialogMapView.getController().setCenter(position);

        Marker marker = new Marker(dialogMapView);
        marker.setPosition(position);
        marker.setTitle(memo.getTitle());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        dialogMapView.getOverlays().add(marker);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        dialog.setOnShowListener(d -> dialogMapView.onResume());
        dialog.setOnDismissListener(d -> {
            dialogMapView.onPause();
        });

        dialog.show();
    }

    @Override
    public void onDeleteClick(Memo memo) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Memo")
                .setMessage("Are you sure you want to delete \"" + memo.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteMemo(memo.getId());
                    
                    // Reload memos in NotesFragment if visible
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof NotesFragment) {
                        ((NotesFragment) currentFragment).loadMemos();
                    }
                    
                    Toast.makeText(MainActivity.this, "Memo deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewClick(Memo memo) {
        onMemoClick(memo);
    }

    @Override
    public void onEditClick(Memo memo) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(margin, margin / 2, margin, margin / 2);

        final EditText editTitle = new EditText(this);
        editTitle.setHint("Title");
        editTitle.setText(memo.getTitle());
        editTitle.setSelection(memo.getTitle().length());
        
        final EditText editDesc = new EditText(this);
        editDesc.setHint("Description");
        editDesc.setText(memo.getText());
        editDesc.setMinLines(3);
        editDesc.setGravity(android.view.Gravity.TOP);

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = margin / 2;
        editTitle.setLayoutParams(params);
        editDesc.setLayoutParams(params);

        layout.addView(editTitle);
        layout.addView(editDesc);

        new AlertDialog.Builder(this)
                .setTitle("Edit Memo")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = editTitle.getText().toString().trim();
                    String newDesc = editDesc.getText().toString().trim();

                    if (newTitle.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    memo.setTitle(newTitle);
                    memo.setText(newDesc);
                    dbHelper.updateMemo(memo);
                    
                    // Reload notes fragment
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof NotesFragment) {
                        ((NotesFragment) currentFragment).loadMemos();
                    }
                    
                    Toast.makeText(MainActivity.this, "Memo updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startLocationServiceIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && notificationsEnabled) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            Intent serviceIntent = new Intent(this, LocationService.class);
            stopService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationServiceIfNeeded();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                String thoroughfare = address.getThoroughfare();
                String featureName = address.getFeatureName();
                String locality = address.getLocality();
                
                if (thoroughfare != null) {
                    if (featureName != null && !featureName.equals(thoroughfare)) {
                        sb.append(featureName).append(" ");
                    }
                    sb.append(thoroughfare);
                } else if (address.getAddressLine(0) != null) {
                    sb.append(address.getAddressLine(0));
                } else {
                    sb.append(String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude));
                }
                
                if (locality != null && !sb.toString().contains(locality)) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(locality);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude);
    }
}