package com.example.gestoravisos.Clases;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import com.prolificinteractive.materialcalendarview.spans.DotSpan;

public class CustomDotSpan implements LineBackgroundSpan {
    private final float radius;
    private final int color;
    private final int offset;

    public CustomDotSpan(float radius, int color, int offset) {
        this.radius = radius;
        this.color = color;
        this.offset = offset;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint,
                               int left, int right, int top, int baseline, int bottom,
                               CharSequence text, int start, int end, int lineNum) {
        int oldColor = paint.getColor();
        paint.setColor(color);
        // Posición ajustada para que los puntos sean visibles
        canvas.drawCircle((left + right) / 2 + offset, bottom + 10, radius, paint);
        paint.setColor(oldColor);
    }
}