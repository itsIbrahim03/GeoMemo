package com.mohammado_ibraheima_obaydas.geomemo;

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
    private boolean isExiting = false;
    private String selectedPointerIcon = "pointer_1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_memo);

        // Make window layout full screen even though it is floating
        getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);

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
        dbHelper = MemoDBHelper.getInstance(this);

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

        // Setup dynamic pointer icon options
        android.widget.LinearLayout layoutPointerOptions = findViewById(R.id.layout_pointer_options);
        final String[] pointerIcons = {
            "pointer_1", "pointer_2", "pointer_3", "pointer_4",
            "pointer_5", "pointer_6", "pointer_7", "pointer_8",
            "pointer_9", "pointer_10", "pointer_11", "pointer_12"
        };
        final View[] optionViews = new View[12];

        for (int i = 0; i < 12; i++) {
            final int index = i;
            final String pointerName = pointerIcons[i];
            int resId = getResources().getIdentifier(pointerName, "drawable", getPackageName());

            com.google.android.material.card.MaterialCardView cardView = 
                    new com.google.android.material.card.MaterialCardView(this);
            int sizePx = (int) (60 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (6 * getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams params = 
                    new android.widget.LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            cardView.setLayoutParams(params);
            cardView.setRadius(8 * getResources().getDisplayMetrics().density);
            cardView.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            cardView.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.transparent));
            cardView.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.card_background));
            cardView.setClickable(true);
            cardView.setFocusable(true);

            android.widget.ImageView imageView = new android.widget.ImageView(this);
            int paddingPx = (int) (8 * getResources().getDisplayMetrics().density);
            imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            imageView.setImageResource(resId);
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            cardView.addView(imageView);

            optionViews[i] = cardView;

            cardView.setOnClickListener(v -> {
                selectedPointerIcon = pointerName;
                for (int j = 0; j < 12; j++) {
                    com.google.android.material.card.MaterialCardView cv = 
                            (com.google.android.material.card.MaterialCardView) optionViews[j];
                    if (cv != null) {
                        if (j == index) {
                            cv.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
                            cv.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent));
                        } else {
                            cv.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.transparent));
                            cv.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.card_background));
                        }
                    }
                }
            });

            layoutPointerOptions.addView(cardView);
        }

        if (optionViews[0] != null) {
            optionViews[0].performClick();
        }

        // Save button listener
        btnSave.setOnClickListener(v -> saveMemo());

        // Animation to grow from FAB
        final int fabX = getIntent().getIntExtra("fab_x", -1);
        final int fabY = getIntent().getIntExtra("fab_y", -1);
        final View rootLayout = findViewById(R.id.add_memo_root);
        final View contentContainer = findViewById(R.id.content_container);
        final View addMemoScrollView = findViewById(R.id.add_memo_scrollview);
        final View scrollContentContainer = findViewById(R.id.scroll_content_container);

        if (fabX != -1 && fabY != -1) {
            rootLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        rootLayout.setVisibility(View.VISIBLE);
                        int[] rootLocation = new int[2];
                        rootLayout.getLocationOnScreen(rootLocation);
                        int relativeX = fabX - rootLocation[0];
                        int relativeY = fabY - rootLocation[1];

                        float startRadius = 28 * getResources().getDisplayMetrics().density;
                        float endRadius = (float) Math.hypot(
                            Math.max(relativeX, rootLayout.getWidth() - relativeX),
                            Math.max(relativeY, rootLayout.getHeight() - relativeY)
                        );

                        android.animation.Animator anim = android.view.ViewAnimationUtils.createCircularReveal(
                            rootLayout, relativeX, relativeY, startRadius, endRadius
                        );
                        anim.setDuration(450);
                        anim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                        anim.addListener(new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(android.animation.Animator animation) {
                                contentContainer.setVisibility(View.VISIBLE);
                                addMemoScrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        addMemoScrollView.setVisibility(View.VISIBLE);
                                        int[] scrollViewLocation = new int[2];
                                        addMemoScrollView.getLocationOnScreen(scrollViewLocation);
                                        int relativeScrollX = fabX - scrollViewLocation[0];
                                        int relativeScrollY = fabY - scrollViewLocation[1];

                                        relativeScrollX = Math.max(0, Math.min(relativeScrollX, addMemoScrollView.getWidth()));
                                        relativeScrollY = Math.max(0, Math.min(relativeScrollY, addMemoScrollView.getHeight()));

                                        float scrollStartRadius = 0;
                                        float scrollEndRadius = (float) Math.hypot(
                                            Math.max(relativeScrollX, addMemoScrollView.getWidth() - relativeScrollX),
                                            Math.max(relativeScrollY, addMemoScrollView.getHeight() - relativeScrollY)
                                        );

                                        android.animation.Animator scrollAnim = android.view.ViewAnimationUtils.createCircularReveal(
                                            addMemoScrollView, relativeScrollX, relativeScrollY, scrollStartRadius, scrollEndRadius
                                        );
                                        scrollAnim.setDuration(400);
                                        scrollAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

                                        scrollContentContainer.setVisibility(View.VISIBLE);
                                        scrollContentContainer.setAlpha(0f);
                                        scrollContentContainer.setTranslationY(80f);
                                        scrollContentContainer.animate()
                                            .alpha(1f)
                                            .translationY(0f)
                                            .setDuration(400)
                                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                            .start();

                                        scrollAnim.start();
                                    }
                                });
                            }
                        });
                        anim.start();
                    } else {
                        rootLayout.setVisibility(View.VISIBLE);
                        contentContainer.setVisibility(View.VISIBLE);
                        addMemoScrollView.setVisibility(View.VISIBLE);
                        scrollContentContainer.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            rootLayout.setVisibility(View.VISIBLE);
            contentContainer.setVisibility(View.VISIBLE);
            addMemoScrollView.setVisibility(View.VISIBLE);
            scrollContentContainer.setVisibility(View.VISIBLE);
        }
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
        memo.setPointerIcon(selectedPointerIcon);

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
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    textSelectedLocation.setText(address);
                }
            });
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

        double startLat = Double.isNaN(selectedLat) ? 31.7683 : selectedLat;
        double startLng = Double.isNaN(selectedLng) ? 35.2137 : selectedLng;
        final double[] tempLat = {startLat};
        final double[] tempLng = {startLng};

        // Initialize OSMDroid for the dialog map
        Configuration.getInstance().setUserAgentValue(getPackageName());

        dialogMapView.setMultiTouchControls(true);
        dialogMapView.setTileSource(TileSourceFactory.MAPNIK);

        GeoPoint startPoint = new GeoPoint(startLat, startLng);
        dialogMapView.getController().setZoom(16.0);
        dialogMapView.getController().setCenter(startPoint);

        // Track panning/scrolling to keep picker selection aligned with the center pointer
        dialogMapView.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                GeoPoint center = (GeoPoint) dialogMapView.getMapCenter();
                tempLat[0] = center.getLatitude();
                tempLng[0] = center.getLongitude();
                return true;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                GeoPoint center = (GeoPoint) dialogMapView.getMapCenter();
                tempLat[0] = center.getLatitude();
                tempLng[0] = center.getLongitude();
                return true;
            }
        });

        // Map events receiver to center map on single tap
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                dialogMapView.getController().animateTo(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        dialogMapView.getOverlays().add(new MapEventsOverlay(receiver));

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

                            // 3. Process search result on Main Thread
                            final GeoPoint finalResult = searchResult;
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    if (finalResult != null) {
                                        dialogMapView.getController().animateTo(finalResult, 16.0, null);
                                    } else {
                                        Toast.makeText(AddMemoActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                                    }
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

    @Override
    public void finish() {
        if (isExiting) {
            super.finish();
            return;
        }

        final int fabX = getIntent().getIntExtra("fab_x", -1);
        final int fabY = getIntent().getIntExtra("fab_y", -1);
        final View rootLayout = findViewById(R.id.add_memo_root);
        final View contentContainer = findViewById(R.id.content_container);

        if (fabX != -1 && fabY != -1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            isExiting = true;

            // Instantly hide the content container so only the solid orange background is visible
            contentContainer.setVisibility(View.INVISIBLE);

            // Shrink orange background back to FAB circle
            int[] rootLocation = new int[2];
            rootLayout.getLocationOnScreen(rootLocation);
            int relativeX = fabX - rootLocation[0];
            int relativeY = fabY - rootLocation[1];

            float startRadius = 28 * getResources().getDisplayMetrics().density;
            float endRadius = (float) Math.hypot(
                Math.max(relativeX, rootLayout.getWidth() - relativeX),
                Math.max(relativeY, rootLayout.getHeight() - relativeY)
            );

            android.animation.Animator anim = android.view.ViewAnimationUtils.createCircularReveal(
                rootLayout, relativeX, relativeY, endRadius, startRadius
            );
            anim.setDuration(350);
            anim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    AddMemoActivity.super.finish();
                    overridePendingTransition(0, 0);
                }
            });
            anim.start();
        } else {
            super.finish();
            overridePendingTransition(R.anim.stay, R.anim.slide_down);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
