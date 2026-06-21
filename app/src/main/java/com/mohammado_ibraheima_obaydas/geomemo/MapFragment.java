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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment {

    private static final int REQUEST_ADD_MEMO = 1;
    private static final double DEFAULT_LAT = 31.7683;
    private static final double DEFAULT_LNG = 35.2137;

    private MapView mapView;
    private FloatingActionButton fabAddMemo;
    private MemoDBHelper dbHelper;

    private double selectedLat = DEFAULT_LAT;
    private double selectedLng = DEFAULT_LNG;

    private EditText editSearchMap;
    private ImageButton btnClearSearch;
    private android.location.LocationListener activeLocationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Initialize OSMDroid Configuration
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map_view);
        fabAddMemo = view.findViewById(R.id.fab_add_memo);
        editSearchMap = view.findViewById(R.id.edit_search_map);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        dbHelper = MemoDBHelper.getInstance(requireContext());

        // Setup OSM MapView Options
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        
        // 100% Free OpenStreetMap Mapnik tiles (Hebrew/English labels)
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Center map on last known location or default
        GeoPoint startPoint = getLastKnownGeoPoint();
        if (startPoint == null) {
            startPoint = new GeoPoint(DEFAULT_LAT, DEFAULT_LNG);
        }
        selectedLat = startPoint.getLatitude();
        selectedLng = startPoint.getLongitude();
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(startPoint);

        // Track panning and zooming to keep the selected location synchronized with the map center
        mapView.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                updateCoordinatesFromMapCenter();
                return true;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                updateCoordinatesFromMapCenter();
                return true;
            }
        });

        setupMapSearch();

        // FAB click to add a memo using the current selection coordinates
        fabAddMemo.setOnClickListener(v -> openAddMemoActivity());

        // Setup location centering button click listener
        View btnMyLocation = view.findViewById(R.id.btn_my_location);
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                    return;
                }
                if (!checkGpsEnabled()) {
                    return;
                }
                GeoPoint currentPoint = getLastKnownGeoPoint();
                if (currentPoint != null) {
                    selectedLat = currentPoint.getLatitude();
                    selectedLng = currentPoint.getLongitude();
                    if (mapView != null) {
                        mapView.getController().animateTo(currentPoint, 15.0, null);
                    }
                    loadAndPlotMemos();
                } else {
                    Toast.makeText(requireContext(), "Fetching current location...", Toast.LENGTH_SHORT).show();
                }
                requestFreshLocation();
            });
        }

        return view;
    }

    private void loadAndPlotMemos() {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        // 1. Add map tap event listener to center map on tapped coordinate
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (mapView != null) {
                    mapView.getController().animateTo(p);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        mapView.getOverlays().add(new MapEventsOverlay(receiver));

        // 2. Plot saved memos
        List<Memo> memos = dbHelper.getAllMemos();
        for (Memo memo : memos) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(memo.getLatitude(), memo.getLongitude()));
            marker.setTitle(memo.getTitle());
            marker.setSubDescription(memo.getText());
            marker.setRelatedObject(memo);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(MarkerHelper.getScaledMarker(requireContext(), memo.getPointerIcon(), 36));
            marker.setOnMarkerClickListener((m, mv) -> {
                if (m.getRelatedObject() instanceof Memo) {
                    Memo clicked = (Memo) m.getRelatedObject();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).onMemoClick(clicked);
                    }
                    return true;
                }
                return false;
            });
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void updateCoordinatesFromMapCenter() {
        if (mapView != null) {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            selectedLat = center.getLatitude();
            selectedLng = center.getLongitude();
        }
    }

    private void openAddMemoActivity() {
        if (fabAddMemo != null) {
            fabAddMemo.setImageDrawable(null);
        }
        Intent intent = new Intent(getActivity(), AddMemoActivity.class);
        intent.putExtra("latitude", selectedLat);
        intent.putExtra("longitude", selectedLng);
        if (fabAddMemo != null) {
            int[] location = new int[2];
            fabAddMemo.getLocationOnScreen(location);
            int fabX = location[0] + fabAddMemo.getWidth() / 2;
            int fabY = location[1] + fabAddMemo.getHeight() / 2;
            intent.putExtra("fab_x", fabX);
            intent.putExtra("fab_y", fabY);
        }
        startActivityForResult(intent, REQUEST_ADD_MEMO);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(0, 0);
        }
    }

    private GeoPoint getLastKnownGeoPoint() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return null;

        Location location = null;
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            Location lastKnown = locationManager.getLastKnownLocation(provider);
            if (lastKnown != null) {
                if (location == null || lastKnown.getTime() > location.getTime()) {
                    location = lastKnown;
                }
            }
        }

        if (location != null) {
            return new GeoPoint(location.getLatitude(), location.getLongitude());
        }
        return null;
    }

    private void requestFreshLocation() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        try {
            // Remove previous active listener if any to avoid leak
            if (activeLocationListener != null) {
                locationManager.removeUpdates(activeLocationListener);
            }

            activeLocationListener = new android.location.LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location loc) {
                    selectedLat = loc.getLatitude();
                    selectedLng = loc.getLongitude();
                    if (isAdded() && mapView != null) {
                        mapView.getController().animateTo(new GeoPoint(selectedLat, selectedLng), 15.0, null);
                        loadAndPlotMemos();
                    }
                    locationManager.removeUpdates(this);
                    activeLocationListener = null;
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, activeLocationListener, android.os.Looper.getMainLooper());
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, activeLocationListener, android.os.Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_MEMO && resultCode == getActivity().RESULT_OK) {
            loadAndPlotMemos();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        loadAndPlotMemos();
        if (fabAddMemo != null) {
            fabAddMemo.setImageResource(android.R.drawable.ic_input_add);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (activeLocationListener != null && getContext() != null) {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.removeUpdates(activeLocationListener);
            }
            activeLocationListener = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupMapSearch() {
        if (editSearchMap == null || btnClearSearch == null) return;

        editSearchMap.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> editSearchMap.setText(""));

        editSearchMap.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = editSearchMap.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(editSearchMap.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        if (getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        String userAgent = appContext.getPackageName() + "/1.0";

        new Thread(() -> {
            GeoPoint searchResult = null;

            // 1. Try Nominatim (OpenStreetMap's official geocoder) - supports English & Hebrew
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery
                        + "&format=json&limit=1&accept-language=en,he";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", userAgent);
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
                        Geocoder geocoder = new Geocoder(appContext, Locale.ENGLISH);
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
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getActivity() != null) {
                        if (finalResult != null) {
                            selectedLat = finalResult.getLatitude();
                            selectedLng = finalResult.getLongitude();
                            if (mapView != null) {
                                mapView.getController().animateTo(finalResult, 15.0, null);
                            }
                            loadAndPlotMemos();
                        } else {
                            showSearchErrorToast();
                        }
                    }
                });
            }
        }).start();
    }

    private void showSearchErrorToast() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> 
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private boolean checkGpsEnabled() {
        if (getContext() == null) return false;
        android.location.LocationManager lm = (android.location.LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception e) {}
        try {
            networkEnabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {}

        if (!gpsEnabled && !networkEnabled) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Location Services Disabled")
                    .setMessage("GeoMemo requires location services to be enabled. Please turn on GPS/Location services in your settings.")
                    .setPositiveButton("Settings", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return false;
        }
        return true;
    }
}
