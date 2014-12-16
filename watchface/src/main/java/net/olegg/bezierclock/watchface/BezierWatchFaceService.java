package net.olegg.bezierclock.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import net.olegg.bezierclock.core.BezierAnimator;

import java.util.TimeZone;

/**
 * Created by olegg on 11.12.14.
 */
public class BezierWatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        private int background = Color.WHITE;
        private int foreground = Color.BLACK;

        private int ambientBackground = Color.BLACK;
        private int ambientForeground = Color.WHITE;

        private Paint paint = new Paint();
        private Paint ambientPaint = new Paint();
        private Matrix matrix = new Matrix();
        private Path path = new Path();

        private final float[] shifts = {0.0f, 300.0f, 800.0f, 1100.0f, 1600.0f, 1900.0f};
        private final RectF modelRect = new RectF(0.0f, 0.0f, 2380.0f, 550.0f);
        private final RectF ambientRect = new RectF(0.0f, 0.0f, 1580.0f, 550.0f);
        private final RectF realRect = new RectF();

        private final Time time = new Time();

        private BezierAnimator[] digits = {
                new BezierAnimator(36000.0f, 5.0f),
                new BezierAnimator(3600.0f, 5.0f),
                new BezierAnimator(600.0f, 5.0f),
                new BezierAnimator(60.0f, 5.0f),
                new BezierAnimator(10.0f, 2.0f),
                new BezierAnimator(1.0f, 1.0f),
        };

        private volatile boolean visible = true;
        private volatile boolean ambient = false;
        private volatile boolean lowbit = false;
        private volatile boolean burnin = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            paint.setStrokeWidth(20);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setColor(foreground);
            paint.setStrokeCap(Paint.Cap.ROUND);

            ambientPaint.setStrokeWidth(20);
            ambientPaint.setStyle(Paint.Style.STROKE);
            ambientPaint.setAntiAlias(true);
            ambientPaint.setColor(ambientForeground);
            ambientPaint.setStrokeCap(Paint.Cap.ROUND);

            setWatchFaceStyle(new WatchFaceStyle.Builder(BezierWatchFaceService.this)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setViewProtection(WatchFaceStyle.PROTECT_HOTWORD_INDICATOR | WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
                    .build());
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                registerReceiver();
                time.clear(TimeZone.getDefault().getID());
                time.setToNow();
                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            ambient = inAmbientMode;
            ambientPaint.setAntiAlias(!lowbit || !inAmbientMode);
            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowbit = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnin = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            time.setToNow();
            if (isInAmbientMode()) {
                drawAmbient(canvas, bounds);
            } else {
                drawInteractive(canvas, bounds);
            }
        }

        private void drawInteractive(Canvas canvas, Rect bounds) {
            realRect.set(bounds);
            matrix.setRectToRect(modelRect, realRect, Matrix.ScaleToFit.CENTER);
            canvas.setMatrix(matrix);
            canvas.drawColor(background);

            int millis = (int)(System.currentTimeMillis() % 1000);

            int secondsUnit = time.second % 10;
            int secondsTen = time.second / 10;
            float secondsUnitRatio = millis / 1000.0f;
            float secondsTenRatio = (secondsUnit * 1000 + millis) / 10000.0f;
            digits[5].update(secondsUnit, getNextInt(secondsUnit, 10), secondsUnitRatio);
            digits[4].update(secondsTen, getNextInt(secondsTen, 6), secondsTenRatio);

            // Minutes
            int minutesUnit = time.minute % 10;
            int minutesTen = time.minute / 10;
            float minutesUnitRatio = (time.second * 1000 + millis) / 60000.0f;
            float minutesTenRatio = (minutesUnit * 60000 + time.second * 1000 + millis) / 600000.0f;
            digits[3].update(minutesUnit, getNextInt(minutesUnit, 10), minutesUnitRatio);
            digits[2].update(minutesTen, getNextInt(minutesTen, 6), minutesTenRatio);

            // Hours
            int hoursUnit = time.hour % 10;
            int hoursTen = time.hour / 10;
            float hoursUnitRatio = (time.minute * 60000 + time.second * 1000 + millis) / 3600000.0f;
            float hoursTenRatio;
            int hoursUnitNext;
            if (time.hour == 23) {
                hoursUnitNext = 0;
                hoursTenRatio = (hoursUnit * 3600000 + time.minute * 60000 + time.second * 1000 + millis) / ( 4 * 3600000.0f);
            } else {
                hoursUnitNext = getNextInt(hoursUnit, 10);
                hoursTenRatio = (hoursUnit * 3600000 + time.minute * 60000 + time.second * 1000 + millis) / 36000000.0f;
            }

            digits[1].update(hoursUnit, hoursUnitNext, hoursUnitRatio);
            digits[0].update(hoursTen, getNextInt(hoursTen, 3), hoursTenRatio);

            path.reset();
            for (int i = 0; i < 6; ++i) {
                path.moveTo(shifts[i] + digits[i].points[0], digits[i].points[1]);
                for (int j = 0, k = 2; j < 4; ++j) {
                    path.cubicTo(
                            shifts[i] + digits[i].points[k++], digits[i].points[k++],
                            shifts[i] + digits[i].points[k++], digits[i].points[k++],
                            shifts[i] + digits[i].points[k++], digits[i].points[k++]);
                }
            }

            canvas.drawPath(path, paint);

            if (visible && !ambient) {
                invalidate();
            }
        }

        private void drawAmbient(Canvas canvas, Rect bounds) {
            realRect.set(bounds);
            if (burnin) {
                realRect.inset(10, 10);
            }
            matrix.setRectToRect(ambientRect, realRect, Matrix.ScaleToFit.CENTER);
            canvas.setMatrix(matrix);
            canvas.drawColor(ambientBackground);

            // Minutes
            int minutesUnit = time.minute % 10;
            int minutesTen = time.minute / 10;
            digits[3].update(minutesUnit);
            digits[2].update(minutesTen);

            // Hours
            int hoursUnit = time.hour % 10;
            int hoursTen = time.hour / 10;
            digits[1].update(hoursUnit);
            digits[0].update(hoursTen);

            path.reset();
            for (int i = 0; i < 4; ++i) {
                path.moveTo(shifts[i] + digits[i].points[0], digits[i].points[1]);
                for (int j = 0, k = 2; j < 4; ++j) {
                    path.cubicTo(
                            shifts[i] + digits[i].points[k++], digits[i].points[k++],
                            shifts[i] + digits[i].points[k++], digits[i].points[k++],
                            shifts[i] + digits[i].points[k++], digits[i].points[k++]);
                }
            }

            canvas.drawPath(path, ambientPaint);
        }

        private int getNextInt(int current, int max) {
            return (current + 1) % max;
        }

        private boolean timeZoneReceiverRegistered = false;
        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.clear(intent.getStringExtra("time-zone"));
                time.setToNow();
                invalidate();
            }
        };

        private void registerReceiver() {
            if (timeZoneReceiverRegistered) {
                return;
            }
            timeZoneReceiverRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            BezierWatchFaceService.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!timeZoneReceiverRegistered) {
                return;
            }
            timeZoneReceiverRegistered = false;
            BezierWatchFaceService.this.unregisterReceiver(timeZoneReceiver);
        }
    }
}
