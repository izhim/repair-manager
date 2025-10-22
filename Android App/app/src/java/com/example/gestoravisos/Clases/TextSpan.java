package com.example.gestoravisos.Clases;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.style.LineBackgroundSpan;

public class TextSpan implements LineBackgroundSpan {
    private final SpannableString text;

    public TextSpan(SpannableString text) {
        this.text = text;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint,
                               int left, int right, int top, int baseline, int bottom,
                               CharSequence charSequence,
                               int start, int end, int lineNum) {
        Paint textPaint = new Paint(paint);
        textPaint.setTextSize(24f); // Tamaño más adecuado
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Posición ajustada para que el texto sea visible
        float centerX = (left + right) / 2f;
        float centerY = bottom + 30; // Ajusta este valor según necesites

        canvas.drawText(text, 0, text.length(), centerX, centerY, textPaint);
    }
}