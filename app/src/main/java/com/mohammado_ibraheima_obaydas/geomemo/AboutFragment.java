package com.mohammado_ibraheima_obaydas.geomemo;

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
        TextView aboutSubmissionDate = view.findViewById(R.id.about_submission_date);

        aboutAppId.setText(requireContext().getPackageName());
        String[] names = getString(R.string.student_names).split(",");
        String[] ids = getString(R.string.student_ids).split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(names.length, ids.length); i++) {
            sb.append(names[i].trim()).append(" (").append(ids[i].trim()).append(")");
            if (i < Math.min(names.length, ids.length) - 1) {
                sb.append("\n");
            }
        }
        aboutStudents.setText(sb.toString());

        String systemInfo = "Android Version: " + Build.VERSION.RELEASE + "\n"
                + "API Level: " + Build.VERSION.SDK_INT + "\n"
                + "Device Model: " + Build.MODEL;
        aboutSystem.setText(systemInfo);
        
        aboutSubmissionDate.setText(getString(R.string.submission_date));

        return view;
    }
}
