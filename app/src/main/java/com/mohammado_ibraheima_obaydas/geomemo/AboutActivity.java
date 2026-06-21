package com.mohammado_ibraheima_obaydas.geomemo;

import android.os.Bundle;

public class AboutActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation(R.id.nav_about);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AboutFragment(), "about")
                    .commit();
        }
    }
}
