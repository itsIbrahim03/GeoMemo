package com.example.geomemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;

public class NotesFragment extends Fragment {

    private static final int REQUEST_ADD_MEMO = 1;

    private RecyclerView recyclerMemos;
    private LinearLayout emptyStateLayout;
    private MemoAdapter memoAdapter;
    private MemoDBHelper dbHelper;
    private List<Memo> memoList;

    private EditText editSearchMemos;
    private ImageButton btnClearSearchMemos;
    private Spinner spinnerSortMemos;

    private String searchQuery = "";
    private int sortOption = 0; // 0: Date Newest, 1: Date Oldest, 2: Distance Nearest
    private double currentLat = Double.NaN;
    private double currentLng = Double.NaN;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerMemos = view.findViewById(R.id.recycler_memos);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        FloatingActionButton fabAddMemo = view.findViewById(R.id.fab_add_memo);

        editSearchMemos = view.findViewById(R.id.edit_search_memos);
        btnClearSearchMemos = view.findViewById(R.id.btn_clear_search_memos);
        spinnerSortMemos = view.findViewById(R.id.spinner_sort_memos);

        dbHelper = MemoDBHelper.getInstance(requireContext());

        // Setup RecyclerView
        memoList = new ArrayList<>();
        if (getActivity() instanceof MemoAdapter.OnMemoClickListener) {
            memoAdapter = new MemoAdapter(memoList, (MemoAdapter.OnMemoClickListener) getActivity());
        }
        recyclerMemos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMemos.setAdapter(memoAdapter);

        // Setup search input listener
        if (editSearchMemos != null && btnClearSearchMemos != null) {
            editSearchMemos.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString();
                    btnClearSearchMemos.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    loadMemos();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnClearSearchMemos.setOnClickListener(v -> editSearchMemos.setText(""));
        }

        // Setup sort spinner
        if (spinnerSortMemos != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSortMemos.setAdapter(adapter);

            spinnerSortMemos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sortOption = position;
                    loadMemos();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // FAB click
        fabAddMemo.setOnClickListener(v -> {
            fabAddMemo.setImageDrawable(null);
            Intent intent = new Intent(getActivity(), AddMemoActivity.class);
            int[] location = new int[2];
            fabAddMemo.getLocationOnScreen(location);
            int fabX = location[0] + fabAddMemo.getWidth() / 2;
            int fabY = location[1] + fabAddMemo.getHeight() / 2;
            intent.putExtra("fab_x", fabX);
            intent.putExtra("fab_y", fabY);
            startActivityForResult(intent, REQUEST_ADD_MEMO);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(0, 0);
            }
        });

        loadMemos();

        return view;
    }

    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                List<String> providers = lm.getProviders(true);
                Location bestLocation = null;
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l != null) {
                        if (bestLocation == null || l.getTime() > bestLocation.getTime()) {
                            bestLocation = l;
                        }
                    }
                }
                if (bestLocation != null) {
                    currentLat = bestLocation.getLatitude();
                    currentLng = bestLocation.getLongitude();
                }
            }
        }
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new Date(0);
        }
        SimpleDateFormat sdfWithSec = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        try {
            return sdfWithSec.parse(dateStr);
        } catch (Exception e) {
            SimpleDateFormat sdfWithoutSec = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            try {
                return sdfWithoutSec.parse(dateStr);
            } catch (Exception ex) {
                return new Date(0);
            }
        }
    }

    public void loadMemos() {
        if (dbHelper == null) return;
        List<Memo> memos = dbHelper.getAllMemos();

        // 1. Filter by Search Query
        List<Memo> filteredMemos = new ArrayList<>();
        String q = searchQuery.toLowerCase().trim();
        for (Memo m : memos) {
            if (q.isEmpty() || 
                (m.getTitle() != null && m.getTitle().toLowerCase().contains(q)) || 
                (m.getText() != null && m.getText().toLowerCase().contains(q))) {
                filteredMemos.add(m);
            }
        }

        // 2. Sort
        if (sortOption == 0) { // Date (Newest First)
            Collections.sort(filteredMemos, (m1, m2) -> {
                Date d1 = parseDate(m1.getCreatedAt());
                Date d2 = parseDate(m2.getCreatedAt());
                return d2.compareTo(d1); // Newest first
            });
        } else if (sortOption == 1) { // Date (Oldest First)
            Collections.sort(filteredMemos, (m1, m2) -> {
                Date d1 = parseDate(m1.getCreatedAt());
                Date d2 = parseDate(m2.getCreatedAt());
                return d1.compareTo(d2); // Oldest first
            });
        } else if (sortOption == 2) { // Distance (Nearest First)
            fetchCurrentLocation();
            if (!Double.isNaN(currentLat) && !Double.isNaN(currentLng)) {
                Collections.sort(filteredMemos, (m1, m2) -> {
                    float[] dist1 = new float[1];
                    Location.distanceBetween(currentLat, currentLng, m1.getLatitude(), m1.getLongitude(), dist1);
                    float[] dist2 = new float[1];
                    Location.distanceBetween(currentLat, currentLng, m2.getLatitude(), m2.getLongitude(), dist2);
                    return Float.compare(dist1[0], dist2[0]);
                });
            }
        } else if (sortOption == 3) { // Distance (Farthest First)
            fetchCurrentLocation();
            if (!Double.isNaN(currentLat) && !Double.isNaN(currentLng)) {
                Collections.sort(filteredMemos, (m1, m2) -> {
                    float[] dist1 = new float[1];
                    Location.distanceBetween(currentLat, currentLng, m1.getLatitude(), m1.getLongitude(), dist1);
                    float[] dist2 = new float[1];
                    Location.distanceBetween(currentLat, currentLng, m2.getLatitude(), m2.getLongitude(), dist2);
                    return Float.compare(dist2[0], dist1[0]); // Farthest first (descending)
                });
            }
        }

        memoAdapter.updateMemos(filteredMemos);

        if (filteredMemos.isEmpty()) {
            recyclerMemos.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerMemos.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_MEMO && resultCode == getActivity().RESULT_OK) {
            loadMemos();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMemos();
        if (getView() != null) {
            FloatingActionButton fabAddMemo = getView().findViewById(R.id.fab_add_memo);
            if (fabAddMemo != null) {
                fabAddMemo.setImageResource(android.R.drawable.ic_input_add);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
