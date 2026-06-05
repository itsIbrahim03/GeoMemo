package com.example.geomemo;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView aboutAppId = view.findViewById(R.id.about_app_id);
        TextView aboutStudents = view.findViewById(R.id.about_students);
        TextView aboutSystem = view.findViewById(R.id.about_system);

        aboutAppId.setText(requireContext().getPackageName());
        aboutStudents.setText(getString(R.string.student_names) + "\n" + getString(R.string.student_ids));

        String systemInfo = "Android Version: " + Build.VERSION.RELEASE + "\n"
                + "API Level: " + Build.VERSION.SDK_INT + "\n"
                + "Device Model: " + Build.MODEL;
        aboutSystem.setText(systemInfo);

        return view;
    }
}
