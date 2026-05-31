package com.example.geomemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PointerCircleView extends View {

    private Paint progressPaint;
    private RectF rectF;
    private float progress = 0f; // 0.0 to 1.0

    public PointerCircleView(Context context) {
        super(context);
        init();
    }

    public PointerCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PointerCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(0xFFC1563A); // Theme Red/Orange color (#C1563A)
        progressPaint.setStyle(Paint.Style.STROKE); // Draw a ring instead of a filled circle

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2;

        int centerX = width / 2;
        int centerY = height / 2;

        // Draw progress sweep using the theme red color
        if (progress > 0f) {
            float strokeWidth = radius * 0.55f; // Stroke thickness to match the red ring
            progressPaint.setStrokeWidth(strokeWidth);

            // Shift the bounds inwards by half the stroke width to prevent clipping
            float midRadius = radius - (strokeWidth / 2f);
            rectF.set(centerX - midRadius, centerY - midRadius, centerX + midRadius, centerY + midRadius);

            // Draw arc starting from top (-90 degrees), useCenter = false for stroked outline
            canvas.drawArc(rectF, -90f, progress * 360f, false, progressPaint);
        }
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    public float getProgress() {
        return this.progress;
    }
}
