package com.example.geomemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class MarkerHelper {

    public static BitmapDrawable getScaledMarker(Context context, String pointerIconName, int sizeDp) {
        int resId = context.getResources().getIdentifier(pointerIconName, "drawable", context.getPackageName());
        if (resId == 0) {
            resId = R.drawable.pointer_1; // fallback
        }
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) return null;

        int widthPx = (int) (sizeDp * context.getResources().getDisplayMetrics().density);
        int heightPx = (int) (widthPx * 340.0 / 300.0);
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, widthPx, heightPx);
        drawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
