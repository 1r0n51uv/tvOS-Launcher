package com.techgenius.tvoslauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Calendar;
import java.util.Random;

/**
 * Seasonal themes for Home, ported from the Mi TV launcher in debloated-tv: Christmas hangs a
 * string of twinkling lights along the top edge and lets snow fall over the screen. A theme
 * switches itself on between two dates; Settings can also pin one, or none.
 */
final class Themes {

    static final String PREF_KEY = "seasonal_theme";
    static final String AUTO = "auto";
    static final String NONE = "none";
    static final String CHRISTMAS = "christmas";

    /** Christmas runs from 1 December to 6 January (month * 100 + day, wrapping the year). */
    private static final int CHRISTMAS_FROM = 1201, CHRISTMAS_TO = 106;

    private Themes() {
    }

    static String choice(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString(PREF_KEY, AUTO);
    }

    static void setChoice(Context c, String choice) {
        c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString(PREF_KEY, choice).apply();
    }

    static boolean christmasSeason(Calendar now) {
        int md = (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DAY_OF_MONTH);
        return md >= CHRISTMAS_FROM || md <= CHRISTMAS_TO;
    }

    /** The theme on screen now: CHRISTMAS or NONE. */
    static String active(Context c) {
        String choice = choice(c);
        if (CHRISTMAS.equals(choice)) return CHRISTMAS;
        if (NONE.equals(choice)) return NONE;
        return christmasSeason(Calendar.getInstance()) ? CHRISTMAS : NONE;
    }

    /** The effects of the active theme over Home, or null for none. They never take focus or touches. */
    static Layer buildLayer(Context c) {
        if (!CHRISTMAS.equals(active(c))) return null;
        return new Layer(c);
    }

    /** Snow over the whole screen and lights along the top; animate only while Home is in front. */
    static final class Layer extends FrameLayout {
        private final SnowView snow;
        private final LightsView lights;

        Layer(Context c) {
            super(c);
            setFocusable(false);
            setClickable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            float dp = c.getResources().getDisplayMetrics().density;
            snow = new SnowView(c);
            addView(snow, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            lights = new LightsView(c);
            addView(lights, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(52 * dp), Gravity.TOP));
        }

        void setRunning(boolean on) {
            snow.setRunning(on);
            lights.setRunning(on);
        }
    }

    /** Falling snow. ~30 fps: the TV's GPU is busy enough with the glass. */
    static final class SnowView extends View {
        private static final int COUNT = 70;
        private static final long FRAME_MS = 33;

        private final float[] baseX = new float[COUNT], y = new float[COUNT], radius = new float[COUNT],
                speed = new float[COUNT], phase = new float[COUNT], sway = new float[COUNT], freq = new float[COUNT];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float dp;
        private boolean running;
        private long last;
        private float time;

        SnowView(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
            paint.setColor(0xFFFFFFFF);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            Random r = new Random(7);
            for (int i = 0; i < COUNT; i++) {
                float size = r.nextFloat();                       // bigger = nearer = faster
                radius[i] = (1.1f + size * 2.6f) * dp;
                speed[i] = (20f + size * 42f) * dp;
                sway[i] = (6f + r.nextFloat() * 14f) * dp;
                freq[i] = 0.5f + r.nextFloat() * 0.8f;
                phase[i] = r.nextFloat() * 6.283f;
                baseX[i] = r.nextFloat() * w;
                y[i] = r.nextFloat() * h;
            }
        }

        void setRunning(boolean on) {
            running = on;
            last = 0;
            if (on) invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            long now = System.nanoTime();
            float dt = last == 0 ? 0f : Math.min(0.05f, (now - last) / 1e9f);
            last = now;
            time += dt;
            int h = getHeight();
            for (int i = 0; i < COUNT; i++) {
                y[i] += speed[i] * dt;
                if (y[i] - radius[i] > h) y[i] = -radius[i];
                float x = baseX[i] + sway[i] * (float) Math.sin(time * freq[i] + phase[i]);
                paint.setAlpha(110 + Math.round(radius[i] / dp * 40f));
                c.drawCircle(x, y[i], radius[i], paint);
            }
            if (running) postInvalidateDelayed(FRAME_MS);
        }
    }

    /** A string of twinkling lights hanging along the top edge. */
    static final class LightsView extends View {
        private static final int[] COLORS = {0xFFFF3B30, 0xFFFFCC00, 0xFF34C759, 0xFF0A84FF};
        private static final int PER_SWAG = 4;

        private final float dp;
        private final Paint wire = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint socket = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bulb = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RadialGradient[] glows = new RadialGradient[COLORS.length];
        private final Path wirePath = new Path();
        private final RectF oval = new RectF();
        private float[] bx = new float[0], by = new float[0];
        private boolean running;
        private float time;
        private long last;

        LightsView(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
            wire.setStyle(Paint.Style.STROKE);
            wire.setStrokeWidth(1.6f * dp);
            wire.setColor(0xFF2B4A38);
            socket.setColor(0xFF1E2A24);
            for (int i = 0; i < COLORS.length; i++) {
                glows[i] = new RadialGradient(0, 0, 20 * dp, (COLORS[i] & 0xFFFFFF) | 0x99000000,
                        COLORS[i] & 0xFFFFFF, Shader.TileMode.CLAMP);
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            int swags = Math.max(1, Math.round(w / (170 * dp)));
            float width = (float) w / swags, sag = 15 * dp, top = 2 * dp;
            bx = new float[swags * PER_SWAG];
            by = new float[swags * PER_SWAG];
            wirePath.reset();
            wirePath.moveTo(0, top);
            for (int s = 0; s < swags; s++) {
                float x0 = s * width;
                wirePath.quadTo(x0 + width / 2f, top + sag * 2f, x0 + width, top);
                for (int k = 0; k < PER_SWAG; k++) {
                    float t = (k + 0.5f) / PER_SWAG;
                    bx[s * PER_SWAG + k] = x0 + width * t;
                    by[s * PER_SWAG + k] = top + sag * 2f * t * (1 - t) * 2f;   // point on the quad curve
                }
            }
        }

        void setRunning(boolean on) {
            running = on;
            last = 0;
            if (on) invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            long now = System.nanoTime();
            if (last != 0) time += Math.min(0.2f, (now - last) / 1e9f);
            last = now;
            c.drawPath(wirePath, wire);
            for (int i = 0; i < bx.length; i++) {
                int ci = i % COLORS.length;
                float b = 0.6f + 0.4f * (float) Math.sin(time * 2.4f + i * 1.9f);
                c.save();
                c.translate(bx[i], by[i]);
                c.drawRect(-2.5f * dp, 0, 2.5f * dp, 5 * dp, socket);
                c.translate(0, 11 * dp);
                halo.setShader(glows[ci]);
                halo.setAlpha(Math.round(255 * b));
                c.drawCircle(0, 0, 20 * dp, halo);
                bulb.setColor(COLORS[ci]);
                bulb.setAlpha(Math.round(150 + 105 * b));
                oval.set(-4.5f * dp, -6.5f * dp, 4.5f * dp, 6.5f * dp);
                c.drawOval(oval, bulb);
                c.restore();
            }
            if (running) postInvalidateDelayed(50);
        }
    }
}
