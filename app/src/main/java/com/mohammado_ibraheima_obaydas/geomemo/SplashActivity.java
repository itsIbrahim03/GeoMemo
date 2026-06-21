package com.mohammado_ibraheima_obaydas.geomemo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3750;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View logoContainer = findViewById(R.id.logo_container);
        ImageView paper = findViewById(R.id.splash_paper);
        View pointerContainer = findViewById(R.id.splash_pointer_container);
        PointerCircleView pointerOverlay = findViewById(R.id.splash_pointer_overlay);
        TextView appTitle = findViewById(R.id.splash_title);
        TextView studentInfo = findViewById(R.id.text_student_info);
        TextView submissionDate = findViewById(R.id.text_submission_date);

        // Format student info exactly as in About page
        String[] names = getString(R.string.student_names).split(",");
        String[] ids = getString(R.string.student_ids).split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(names.length, ids.length); i++) {
            sb.append(names[i].trim()).append(" (").append(ids[i].trim()).append(")");
            if (i < Math.min(names.length, ids.length) - 1) {
                sb.append("\n");
            }
        }
        studentInfo.setText(sb.toString());

        // Initial state: everything hidden
        paper.setAlpha(0f);
        paper.setTranslationX(-600f);   // paper starts off-screen left
        pointerContainer.setAlpha(0f);
        pointerContainer.setTranslationY(-500f); // pointer container starts above screen
        pointerOverlay.setAlpha(1f);    // overlay starts fully visible (covers the red/brown circle with white)
        pointerOverlay.setProgress(0f); // progress starts at 0 (meaning solid white circle)
        appTitle.setAlpha(0f);
        studentInfo.setAlpha(0f);
        submissionDate.setAlpha(0f);

        paper.post(() -> {
            // Step 1: Paper slides in from the left (0ms - 600ms)
            paper.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .withEndAction(() -> {

                        // Step 2: Pointer falls down into the paper (600ms - 1000ms)
                        pointerContainer.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(400)
                                .setInterpolator(new AccelerateInterpolator(1.2f))
                                .withEndAction(() -> {

                                    // Step 3: Pop/pulse — container pushes down then bounces up
                                    logoContainer.animate()
                                            .translationY(30f)
                                            .setDuration(120)
                                            .setInterpolator(new AccelerateInterpolator())
                                            .withEndAction(() -> {
                                                logoContainer.animate()
                                                        .translationY(0f)
                                                        .setDuration(500)
                                                        .setInterpolator(new OvershootInterpolator(3f))
                                                        .withEndAction(() -> {

                                                            // After the pulse: animate progress of red circle from 0 to 1
                                                            android.animation.ObjectAnimator progressAnimator =
                                                                    android.animation.ObjectAnimator.ofFloat(pointerOverlay, "progress", 0f, 1f);
                                                            progressAnimator.setDuration(800); // 800ms load animation duration
                                                            progressAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                                                            progressAnimator.start();

                                                             // Step 4: Fade in title, student info, and date
                                                             appTitle.animate()
                                                                     .alpha(1f)
                                                                     .setDuration(500)
                                                                     .start();
                                                             studentInfo.animate()
                                                                     .alpha(1f)
                                                                     .setDuration(500)
                                                                     .start();
                                                             submissionDate.animate()
                                                                     .alpha(1f)
                                                                     .setDuration(500)
                                                                     .start();
                                                        })
                                                        .start();
                                            })
                                            .start();
                                })
                                .start();
                    })
                    .start();
        });

        // Navigate to MainActivity
        navigateRunnable = () -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            finish();
        };
        handler.postDelayed(navigateRunnable, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && navigateRunnable != null) {
            handler.removeCallbacks(navigateRunnable);
        }
    }
}
