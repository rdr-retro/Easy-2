package com.rdr.easy2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class BatteryIconView extends View {
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint terminalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bodyRect = new RectF();
    private final RectF terminalRect = new RectF();
    private final RectF fillRect = new RectF();

    private int batteryLevel = 100;

    public BatteryIconView(Context context) {
        super(context);
        init();
    }

    public BatteryIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dpToPx(1.8f));
        outlinePaint.setColor(0xFFFFFFFF);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xFFFFFFFF);

        terminalPaint.setStyle(Paint.Style.FILL);
        terminalPaint.setColor(0xFFFFFFFF);
    }

    public void setBatteryLevel(int level) {
        int clampedLevel = Math.max(0, Math.min(100, level));
        if (batteryLevel == clampedLevel) {
            return;
        }
        batteryLevel = clampedLevel;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float strokeWidth = outlinePaint.getStrokeWidth();
        float bodyTop = height * 0.16f;
        float bodyBottom = height * 0.84f;
        float terminalWidth = width * 0.12f;
        float terminalHeight = height * 0.30f;
        float bodyRight = width - terminalWidth - strokeWidth;
        float cornerRadius = height * 0.14f;

        bodyRect.set(
                strokeWidth,
                bodyTop,
                bodyRight,
                bodyBottom
        );
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, outlinePaint);

        terminalRect.set(
                bodyRect.right + strokeWidth * 0.35f,
                (height - terminalHeight) / 2f,
                width - strokeWidth * 0.2f,
                (height + terminalHeight) / 2f
        );
        canvas.drawRoundRect(terminalRect, cornerRadius / 2f, cornerRadius / 2f, terminalPaint);

        float innerPadding = strokeWidth * 1.7f;
        float innerLeft = bodyRect.left + innerPadding;
        float innerTop = bodyRect.top + innerPadding;
        float innerRight = bodyRect.right - innerPadding;
        float innerBottom = bodyRect.bottom - innerPadding;
        float availableWidth = Math.max(0f, innerRight - innerLeft);
        float fillWidth = availableWidth * (batteryLevel / 100f);

        if (batteryLevel > 0f) {
            fillWidth = Math.max(fillWidth, strokeWidth * 1.4f);
        }

        if (fillWidth <= 0f) {
            return;
        }

        fillRect.set(
                innerLeft,
                innerTop,
                Math.min(innerLeft + fillWidth, innerRight),
                innerBottom
        );
        float fillRadius = Math.min(cornerRadius * 0.8f, (fillRect.bottom - fillRect.top) / 2f);
        canvas.drawRoundRect(fillRect, fillRadius, fillRadius, fillPaint);
    }

    private float dpToPx(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
