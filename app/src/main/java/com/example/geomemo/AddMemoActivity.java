package com.example.geomemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

public class AddMemoActivity extends AppCompatActivity {

    private EditText editTitle;
    private EditText editText;
    private TextView textSelectedLocation;
    private Button btnSave;

    private double selectedLat = Double.NaN;
    private double selectedLng = Double.NaN;
    private MemoDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_memo);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_add_memo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Memo");
        }

        // Initialize views
        editTitle = findViewById(R.id.edit_memo_title);
        editText = findViewById(R.id.edit_memo_text);
        textSelectedLocation = findViewById(R.id.text_selected_location);
        btnSave = findViewById(R.id.btn_save_memo);

        // Initialize database helper
        dbHelper = new MemoDBHelper(this);

        // Get coordinates from intent extras
        Intent intent = getIntent();
        selectedLat = intent.getDoubleExtra("latitude", Double.NaN);
        selectedLng = intent.getDoubleExtra("longitude", Double.NaN);

        if (Double.isNaN(selectedLat) || Double.isNaN(selectedLng)) {
            GeoPoint currentPoint = getCurrentLocation();
            selectedLat = currentPoint.getLatitude();
            selectedLng = currentPoint.getLongitude();
        }

        updateLocationText(selectedLat, selectedLng);

        ImageButton btnEditLocation = findViewById(R.id.btn_edit_location);
        btnEditLocation.setOnClickListener(v -> showLocationPickerDialog());

        // Save button listener
        btnSave.setOnClickListener(v -> saveMemo());
    }

    private void saveMemo() {
        String title = editTitle.getText().toString().trim();
        String text = editText.getText().toString().trim();

        if (title.isEmpty()) {
            editTitle.setError("Title is required");
            editTitle.requestFocus();
            return;
        }

        if (Double.isNaN(selectedLat) || Double.isNaN(selectedLng)) {
            Toast.makeText(this, "Coordinates are missing", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        Memo memo = new Memo();
        memo.setTitle(title);
        memo.setText(text);
        memo.setLatitude(selectedLat);
        memo.setLongitude(selectedLng);
        memo.setCreatedAt(currentDate);

        dbHelper.insertMemo(memo);

        Toast.makeText(this, "Memo created successfully", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private GeoPoint getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                List<String> providers = locationManager.getProviders(true);
                Location bestLocation = null;
                for (String provider : providers) {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l != null) {
                        if (bestLocation == null || l.getTime() > bestLocation.getTime()) {
                            bestLocation = l;
                        }
                    }
                }
                if (bestLocation != null) {
                    return new GeoPoint(bestLocation.getLatitude(), bestLocation.getLongitude());
                }
            }
        }
        return new GeoPoint(31.7683, 35.2137); // Fallback to Jerusalem
    }

    private void updateLocationText(double lat, double lng) {
        textSelectedLocation.setText("Loading location...");
        new Thread(() -> {
            String address = getAddressFromLocation(lat, lng);
            runOnUiThread(() -> textSelectedLocation.setText(address));
        }).start();
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

    private void showLocationPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_map_picker, null);
        MapView dialogMapView = dialogView.findViewById(R.id.picker_map_view);

        final double[] tempLat = {selectedLat};
        final double[] tempLng = {selectedLng};
        final Marker[] markerRef = {null};

        // Initialize OSMDroid for the dialog map
        Configuration.getInstance().setUserAgentValue(getPackageName());

        dialogMapView.setMultiTouchControls(true);
        dialogMapView.setTileSource(TileSourceFactory.MAPNIK);

        GeoPoint startPoint = new GeoPoint(tempLat[0], tempLng[0]);
        dialogMapView.getController().setZoom(16.0);
        dialogMapView.getController().setCenter(startPoint);

        // Map events receiver for tap positioning
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                tempLat[0] = p.getLatitude();
                tempLng[0] = p.getLongitude();
                if (markerRef[0] != null) {
                    markerRef[0].setPosition(p);
                    dialogMapView.invalidate();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        dialogMapView.getOverlays().add(new MapEventsOverlay(receiver));

        Marker marker = new Marker(dialogMapView);
        marker.setPosition(startPoint);
        marker.setTitle("Drag me or tap map to position");
        marker.setDraggable(true);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        Drawable icon = getCustomMarkerIcon();
        if (icon != null) {
            marker.setIcon(icon);
        }
        dialogMapView.getOverlays().add(marker);
        markerRef[0] = marker;

        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker m) {
                tempLat[0] = m.getPosition().getLatitude();
                tempLng[0] = m.getPosition().getLongitude();
            }

            @Override
            public void onMarkerDragStart(Marker marker) {}
        });

        // Setup Search inside picker dialog
        EditText editSearchPicker = dialogView.findViewById(R.id.edit_search_picker);
        ImageButton btnClearSearchPicker = dialogView.findViewById(R.id.btn_clear_search_picker);
        if (editSearchPicker != null && btnClearSearchPicker != null) {
            editSearchPicker.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    btnClearSearchPicker.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            btnClearSearchPicker.setOnClickListener(v -> editSearchPicker.setText(""));

            editSearchPicker.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    String query = editSearchPicker.getText().toString().trim();
                    if (!query.isEmpty()) {
                        new Thread(() -> {
                            GeoPoint searchResult = null;

                            // 1. Try Nominatim (OSM official geocoder) - supports English & Hebrew
                            try {
                                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery
                                        + "&format=json&limit=1&accept-language=en,he";
                                URL url = new URL(urlStr);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("GET");
                                conn.setRequestProperty("User-Agent", getPackageName() + "/1.0");
                                conn.setConnectTimeout(5000);
                                conn.setReadTimeout(5000);

                                int responseCode = conn.getResponseCode();
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                    StringBuilder response = new StringBuilder();
                                    String inputLine;
                                    while ((inputLine = in.readLine()) != null) {
                                        response.append(inputLine);
                                    }
                                    in.close();

                                    JSONArray results = new JSONArray(response.toString());
                                    if (results.length() > 0) {
                                        JSONObject result = results.getJSONObject(0);
                                        double lat = result.getDouble("lat");
                                        double lon = result.getDouble("lon");
                                        searchResult = new GeoPoint(lat, lon);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // 2. Fallback to Android Geocoder if Nominatim failed
                            if (searchResult == null) {
                                try {
                                    if (Geocoder.isPresent()) {
                                        Geocoder geocoder = new Geocoder(AddMemoActivity.this, Locale.ENGLISH);
                                        List<Address> addresses = geocoder.getFromLocationName(query, 1);
                                        if (addresses != null && !addresses.isEmpty()) {
                                            Address address = addresses.get(0);
                                            searchResult = new GeoPoint(address.getLatitude(), address.getLongitude());
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // 3. Process search result on Main Thread
                            final GeoPoint finalResult = searchResult;
                            runOnUiThread(() -> {
                                if (finalResult != null && markerRef[0] != null) {
                                    tempLat[0] = finalResult.getLatitude();
                                    tempLng[0] = finalResult.getLongitude();
                                    markerRef[0].setPosition(finalResult);
                                    dialogMapView.getController().animateTo(finalResult, 16.0, null);
                                    dialogMapView.invalidate();
                                } else {
                                    Toast.makeText(AddMemoActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    }
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(editSearchPicker.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Select", (d, which) -> {
                    selectedLat = tempLat[0];
                    selectedLng = tempLng[0];
                    updateLocationText(selectedLat, selectedLng);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialogMapView.onResume());
        dialog.setOnDismissListener(d -> {
            dialogMapView.onPause();
        });

        dialog.show();
    }

    private Drawable getCustomMarkerIcon() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.map_pointer);
        if (drawable != null) {
            int height = (int) (48 * getResources().getDisplayMetrics().density);
            int width = (int) (height * ((double) drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight()));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return new BitmapDrawable(getResources(), bitmap);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
