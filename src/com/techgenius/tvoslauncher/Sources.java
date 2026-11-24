package com.techgenius.tvoslauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The devices plugged into the TV's HDMI ports, shown as quick picks on Home (the Sources card).
 * A device counts as on while its input is CONNECTED; standby and unplugged both read as off.
 */
final class Sources {

    /** Label, HDMI port number (position in the TV's HDMI list), picture in assets/sources. */
    static final String[][] DEVICES = {
            {"Switch", "1", "sources/switch.webp"},
            {"PS5", "2", "sources/ps5.jpg"},
            {"PC", "3", "sources/pc.jpg"}};

    static final ColorMatrixColorFilter GRAYSCALE;

    static {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        GRAYSCALE = new ColorMatrixColorFilter(m);
    }

    private static final Map<String, Bitmap> PICTURES = new HashMap<>();

    private Sources() {
    }

    static TvInputManager manager(Context c) {
        try {
            return (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        } catch (Exception e) {
            return null;
        }
    }

    /** The TV's own HDMI inputs, in the order of their labels (HDMI 1, HDMI 2, ...). */
    private static List<TvInputInfo> hdmiInputs(Context c) {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tm = manager(c);
        if (tm == null) return out;
        try {
            for (TvInputInfo i : tm.getTvInputList()) {
                if (i.getType() == TvInputInfo.TYPE_HDMI && i.getParentId() == null && !i.isHidden(c)
                        && i.getId().contains("/HW")) {
                    out.add(i);
                }
            }
        } catch (Exception ignored) {
            // no input service
        }
        java.util.Collections.sort(out, (a, b) -> label(c, a).compareToIgnoreCase(label(c, b)));
        return out;
    }

    private static String label(Context c, TvInputInfo i) {
        CharSequence l = i.loadLabel(c);
        return l != null ? l.toString() : i.getId();
    }

    /** The input id of a device, or null when the TV has no such HDMI port. */
    static String inputId(Context c, int device) {
        int port = Integer.parseInt(DEVICES[device][1]);
        List<TvInputInfo> hdmi = hdmiInputs(c);
        return port >= 1 && port <= hdmi.size() ? hdmi.get(port - 1).getId() : null;
    }

    /** True when the TV has HDMI inputs at all (a box like a Mi Box has none). */
    static boolean available(Context c) {
        return !hdmiInputs(c).isEmpty();
    }

    static boolean isOn(Context c, int device) {
        String id = inputId(c, device);
        TvInputManager tm = manager(c);
        if (id == null || tm == null) return false;
        try {
            return tm.getInputState(id) == TvInputManager.INPUT_STATE_CONNECTED;
        } catch (Exception e) {
            return false;
        }
    }

    /** Switches the TV to a device's port, as the remote's Input button does. */
    static boolean open(Context c, int device) {
        String id = inputId(c, device);
        if (id == null) return false;
        Inputs.Input in = new Inputs.Input();
        in.id = id;
        in.type = TvInputInfo.TYPE_HDMI;
        in.passthrough = true;
        in.label = DEVICES[device][0];
        return Inputs.open(c, in);
    }

    static String portLabel(int device) {
        return "HDMI " + DEVICES[device][1];
    }

    static synchronized Bitmap picture(Context c, int device) {
        String path = DEVICES[device][2];
        Bitmap b = PICTURES.get(path);
        if (b != null && !b.isRecycled()) return b;
        try (InputStream in = c.getAssets().open(path)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            o.inJustDecodeBounds = false;
            // Tiles are at most ~400 px wide: decode the 2160 px PC picture at a quarter.
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= 480) o.inSampleSize *= 2;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream again = c.getAssets().open(path)) {
                b = BitmapFactory.decodeStream(again, null, o);
            }
            if (b != null) PICTURES.put(path, b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
