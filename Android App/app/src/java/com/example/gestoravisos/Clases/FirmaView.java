package com.example.gestoravisos.Clases;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FirmaView extends View {
    private Paint paint;
    private Path path;
    public List<Path> paths = new ArrayList<>();
    private int currentColor = Color.BLACK;
    private int strokeWidth = 5;

    public FirmaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
    }

    private void setupPaint() {
        path = new Path();
        paint = new Paint();
        paint.setColor(currentColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path p : paths) {
            canvas.drawPath(p, paint);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                paths.add(new Path(path));
                path.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void limpiarFirma() {
        paths.clear();
        path.reset();
        invalidate();
    }

    public Bitmap getFirmaBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    public void cargarFirmaExistente(Bitmap bitmap) {
        // Mostrar la firma como imagen estática
        Canvas canvas = new Canvas();
        canvas.drawBitmap(bitmap, 0, 0, new Paint());
        invalidate();

        // Deshabilitar dibujo nuevo
        setEnabled(false);
    }
}