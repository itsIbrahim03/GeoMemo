package com.mohammado_ibraheima_obaydas.geomemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseNavigationActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected int currentCheckedItemId;

    protected void setupNavigation(int checkedItemId) {
        this.currentCheckedItemId = checkedItemId;
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Use our custom 3-dots icon instead of the hamburger icon to unfold the navigation drawer
        toggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_three_dots);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem menuItem = navigationView.getMenu().findItem(checkedItemId);
        if (menuItem != null) {
            menuItem.setChecked(true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(menuItem.getTitle());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navigationView != null) {
            MenuItem menuItem = navigationView.getMenu().findItem(currentCheckedItemId);
            if (menuItem != null) {
                menuItem.setChecked(true);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        drawerLayout.closeDrawer(GravityCompat.START);

        if (id == R.id.nav_map) {
            if (!(this instanceof MainActivity)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
            return true;
        } else if (id == R.id.nav_notes) {
            if (!(this instanceof NotesActivity)) {
                Intent intent = new Intent(this, NotesActivity.class);
                startActivity(intent);
                if (!(this instanceof MainActivity)) {
                    finish();
                }
            }
            return true;
        } else if (id == R.id.nav_settings) {
            if (!(this instanceof SettingsActivity)) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                if (!(this instanceof MainActivity)) {
                    finish();
                }
            }
            return true;
        } else if (id == R.id.nav_about) {
            if (!(this instanceof AboutActivity)) {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                if (!(this instanceof MainActivity)) {
                    finish();
                }
            }
            return true;
        } else if (id == R.id.nav_exit) {
            showExitDialog();
            return true;
        }
        return false;
    }

    protected void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                    finishAffinity();
                })
                .setNegativeButton(R.string.btn_no, null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
