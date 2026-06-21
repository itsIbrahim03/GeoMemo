package com.mohammado_ibraheima_obaydas.geomemo;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.List;
import java.util.Locale;

public class NotesActivity extends BaseNavigationActivity implements MemoAdapter.OnMemoClickListener {

    private MemoDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database helper
        dbHelper = MemoDBHelper.getInstance(this);

        setupNavigation(R.id.nav_notes);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotesFragment(), "notes")
                    .commit();
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
        marker.setIcon(MarkerHelper.getScaledMarker(this, memo.getPointerIcon(), 36));
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
                    
                    Toast.makeText(NotesActivity.this, "Memo deleted", Toast.LENGTH_SHORT).show();
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

        // Choose Memo Icon Label
        android.widget.TextView labelIcon = new android.widget.TextView(this);
        labelIcon.setText("Choose Memo Icon");
        labelIcon.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = margin;
        labelParams.bottomMargin = margin / 2;
        labelIcon.setLayoutParams(labelParams);
        layout.addView(labelIcon);

        // HorizontalScrollView for pointer choices
        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
        android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollView.setLayoutParams(scrollParams);
        scrollView.setHorizontalScrollBarEnabled(false);

        android.widget.LinearLayout layoutPointerOptions = new android.widget.LinearLayout(this);
        layoutPointerOptions.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layoutPointerOptions.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.addView(layoutPointerOptions);
        layout.addView(scrollView);

        final String[] pointerIcons = {
            "pointer_1", "pointer_2", "pointer_3", "pointer_4",
            "pointer_5", "pointer_6", "pointer_7", "pointer_8",
            "pointer_9", "pointer_10", "pointer_11", "pointer_12"
        };
        final View[] optionViews = new View[12];
        final String[] selectedPointerIcon = { memo.getPointerIcon() != null ? memo.getPointerIcon() : "pointer_1" };

        for (int i = 0; i < 12; i++) {
            final int index = i;
            final String pointerName = pointerIcons[i];
            int resId = getResources().getIdentifier(pointerName, "drawable", getPackageName());

            com.google.android.material.card.MaterialCardView cardView = 
                    new com.google.android.material.card.MaterialCardView(this);
            int sizePx = (int) (60 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (6 * getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams cardParams = 
                    new android.widget.LinearLayout.LayoutParams(sizePx, sizePx);
            cardParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            cardView.setLayoutParams(cardParams);
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
                selectedPointerIcon[0] = pointerName;
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

        // Initially select the correct icon card
        int initialSelectionIndex = 0;
        for (int i = 0; i < 12; i++) {
            if (pointerIcons[i].equals(selectedPointerIcon[0])) {
                initialSelectionIndex = i;
                break;
            }
        }
        if (optionViews[initialSelectionIndex] != null) {
            optionViews[initialSelectionIndex].performClick();
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Memo")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = editTitle.getText().toString().trim();
                    String newDesc = editDesc.getText().toString().trim();

                    if (newTitle.isEmpty()) {
                        Toast.makeText(NotesActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    memo.setTitle(newTitle);
                    memo.setText(newDesc);
                    memo.setPointerIcon(selectedPointerIcon[0]);
                    dbHelper.updateMemo(memo);
                    
                    // Reload notes fragment
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof NotesFragment) {
                        ((NotesFragment) currentFragment).loadMemos();
                    }
                    
                    Toast.makeText(NotesActivity.this, "Memo updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                        sb.append(featureName).append(" ").append(thoroughfare);
                    } else {
                        sb.append(thoroughfare);
                    }
                } else if (featureName != null) {
                    sb.append(featureName);
                }
                
                if (locality != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(locality);
                }
                
                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format(Locale.ENGLISH, "%.4f, %.4f", latitude, longitude);
    }
}
