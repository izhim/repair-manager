package com.example.gestoravisos.Clases;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.compose.Span;
import androidx.core.content.ContextCompat;

import com.example.gestoravisos.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventDecorator implements DayViewDecorator {
    private final Set<CalendarDay> daysWithEvents;
    private final Drawable backgroundDrawable;
    private final int textColor;
    private final boolean boldText;

    public EventDecorator(Collection<CalendarDay> daysWithEvents, Context context, boolean isUrgent) {
        this.daysWithEvents = new HashSet<>(daysWithEvents);
        this.backgroundDrawable = ContextCompat.getDrawable(context,
                isUrgent ? R.drawable.calendar_day_urgent_background : R.drawable.calendar_day_background);
        this.textColor = Color.WHITE; // Texto blanco
        this.boldText = true; // Texto en negrita
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return daysWithEvents.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(backgroundDrawable);
        view.addSpan(new ForegroundColorSpan(textColor));
        if (boldText) {
            view.addSpan(new StyleSpan(Typeface.BOLD));
        }
        view.addSpan(new RelativeSizeSpan(1.1f));
    }
}