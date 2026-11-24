package com.techgenius.tvoslauncher;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default Home Dashboard displayed when no application is selected, when focus is on
 * non-app elements (Settings, More, top bar), or when the focused app has no Continue Watching items.
 *
 * Displays customizable Liquid Glass widgets in one horizontal row: Sources (the TV's HDMI
 * inputs as quick picks), Weather, Football Matches, Live News, Latest Releases and Top Media.
 */
public final class HomeDashboard {

    private static final String PREF = "home_dashboard_prefs";
    private static final String KEY_ENABLED = "dashboard_enabled";
    private static final String KEY_SHOW_WHEN_NO_CW = "show_when_no_cw";
    private static final String KEY_W_SOURCES = "widget_sources";
    private static final String KEY_W_FOOTBALL = "widget_football";
    private static final String KEY_W_RELEASES = "widget_releases";
    private static final String KEY_W_WEATHER = "widget_weather";
    private static final String KEY_W_NEWS = "widget_news";
    private static final String KEY_W_TOP_MEDIA = "widget_top_media";
    private static final String KEY_WIDGET_ORDER = "widget_order";
    private static final String KEY_SOURCES_FIRST = "widget_sources_first_v1";
    private static final String KEY_ALL_DEFAULT_V2 = "widgets_all_default_v2";

    public static final String WIDGET_SOURCES = "sources";
    public static final String WIDGET_FOOTBALL = "football";
    public static final String WIDGET_RELEASES = "releases";
    public static final String WIDGET_WEATHER = "weather";
    public static final String WIDGET_NEWS = "news";
    public static final String WIDGET_TOP_MEDIA = "top_media";

    public static final String[] DEFAULT_WIDGET_IDS = {
        WIDGET_SOURCES, WIDGET_WEATHER, WIDGET_FOOTBALL, WIDGET_NEWS, WIDGET_RELEASES, WIDGET_TOP_MEDIA
    };

    private static volatile String sMovingWidgetId = null;


    private static final String KEY_FAV_TEAMS = "football_fav_teams";
    private static final String KEY_FAV_TEAMS_EU = "football_fav_teams_eu_v1";
    private static final String KEY_FOOTBALL_LEAGUE = "football_league";
    private static final String KEY_FOOTBALL_LEAGUE_EU = "football_league_eu_v3";
    private static final String KEY_FOOTBALL_CACHE_JSON = "football_cache_json_eu2";
    private static final String KEY_FOOTBALL_CACHE_TS = "football_cache_ts_eu2";

    private static final String KEY_RELEASES_AUTO = "releases_countries_auto";
    private static final String KEY_RELEASES_SET = "releases_countries_set";
    private static final String KEY_RELEASES_CACHE_JSON = "releases_cache_json";
    private static final String KEY_RELEASES_CACHE_SIG = "releases_cache_sig";
    private static final String KEY_RELEASES_CACHE_TS = "releases_cache_ts";
    private static final String KEY_NEWS_COUNTRY = "news_country";

    /**
     * Supported countries for Latest Releases widget: { ISO-3166 alpha-2 code, Display Name, UI language code }.
     */
    public static final String[][] RELEASES_COUNTRIES = {
        {"US", "United States", "en"},
        {"GB", "United Kingdom", "en"},
        {"DE", "Deutschland (Germany)", "de"},
        {"FR", "France", "fr"},
        {"ES", "España (Spain)", "es"},
        {"IT", "Italia (Italy)", "it"},
        {"PT", "Portugal", "pt"},
        {"BR", "Brasil (Brazil)", "pt"},
        {"RU", "Россия (Russia)", "ru"}
    };

    /**
     * Supported countries/editions for Live News widget: { ISO code (gl), Display Name, Google News hl, ceid }.
     */
    public static final String[][] NEWS_COUNTRIES = {
        {"US", "\uD83C\uDDFA\uD83C\uDDF8  United States", "en-US", "US:en"},
        {"GB", "\uD83C\uDDEC\uD83C\uDDE7  United Kingdom", "en-GB", "GB:en"},
        {"CA", "\uD83C\uDDE8\uD83C\uDDE6  Canada", "en-CA", "CA:en"},
        {"AU", "\uD83C\uDDE6\uD83C\uDDFA  Australia", "en-AU", "AU:en"},
        {"BR", "\uD83C\uDDE7\uD83C\uDDF7  Brasil (Brazil)", "pt-BR", "BR:pt-419"},
        {"PT", "\uD83C\uDDF5\uD83C\uDDF9  Portugal", "pt-PT", "PT:pt-150"},
        {"DE", "\uD83C\uDDE9\uD83C\uDDEA  Deutschland (Germany)", "de", "DE:de"},
        {"FR", "\uD83C\uDDEB\uD83C\uDDF7  France", "fr", "FR:fr"},
        {"ES", "\uD83C\uDDEA\uD83C\uDDF8  España (Spain)", "es", "ES:es"},
        {"MX", "\uD83C\uDDF2\uD83C\uDDFD  México (Mexico)", "es-419", "MX:es-419"},
        {"IT", "\uD83C\uDDEE\uD83C\uDDF9  Italia (Italy)", "it", "IT:it"},
        {"RU", "\uD83C\uDDF7\uD83C\uDDFA  Россия (Russia)", "ru", "RU:ru"},
        {"PL", "\uD83C\uDDF5\uD83C\uDDF1  Polska (Poland)", "pl", "PL:pl"},
        {"SE", "\uD83C\uDDF8\uD83C\uDDEA  Sverige (Sweden)", "sv", "SE:sv"}
    };

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private HomeDashboard() {}

    private static SharedPreferences prefs(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean(KEY_ALL_DEFAULT_V2, false)) {
            sp.edit()
                    .putBoolean(KEY_ALL_DEFAULT_V2, true)
                    .putBoolean(KEY_W_NEWS, true)
                    .putBoolean(KEY_W_TOP_MEDIA, true)
                    .apply();
        }
        // Once: the old date-range fetch showed nothing, so a league picked while trying to make it
        // work is likely not wanted; start again from every European competition.
        if (!sp.getBoolean(KEY_FOOTBALL_LEAGUE_EU, false)) {
            sp.edit().putString(KEY_FOOTBALL_LEAGUE, "all").putBoolean(KEY_FOOTBALL_LEAGUE_EU, true)
                    .remove(KEY_FOOTBALL_CACHE_JSON).putLong(KEY_FOOTBALL_CACHE_TS, 0L).apply();
        }
        return sp;
    }

    public static boolean isEnabled(Context c) {
        return prefs(c).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_ENABLED, en).apply();
    }

    public static boolean isShowWhenNoCw(Context c) {
        return prefs(c).getBoolean(KEY_SHOW_WHEN_NO_CW, true);
    }

    public static void setShowWhenNoCw(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_SHOW_WHEN_NO_CW, en).apply();
    }

    public static boolean isWidgetSourcesEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_SOURCES, true);
    }

    public static void setWidgetSourcesEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_SOURCES, en).apply();
    }

    public static boolean isWidgetFootballEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_FOOTBALL, true);
    }

    public static void setWidgetFootballEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_FOOTBALL, en).apply();
    }

    public static boolean isWidgetReleasesEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_RELEASES, true);
    }

    public static void setWidgetReleasesEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_RELEASES, en).apply();
    }

    public static boolean isWidgetWeatherEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_WEATHER, true);
    }

    public static void setWidgetWeatherEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_WEATHER, en).apply();
    }

    public static boolean isWidgetNewsEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_NEWS, true);
    }

    public static void setWidgetNewsEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_NEWS, en).apply();
    }

    public static boolean isWidgetTopMediaEnabled(Context c) {
        return prefs(c).getBoolean(KEY_W_TOP_MEDIA, true);
    }

    public static void setWidgetTopMediaEnabled(Context c, boolean en) {
        prefs(c).edit().putBoolean(KEY_W_TOP_MEDIA, en).apply();
    }

    public static List<String> getWidgetOrder(Context c) {
        SharedPreferences sp = prefs(c);
        // Once: a saved order predates the Sources card, which would land last; put it first.
        if (!sp.getBoolean(KEY_SOURCES_FIRST, false)) {
            String saved = sp.getString(KEY_WIDGET_ORDER, "");
            SharedPreferences.Editor e = sp.edit().putBoolean(KEY_SOURCES_FIRST, true);
            if (saved != null && !saved.isEmpty() && !saved.contains(WIDGET_SOURCES)) {
                e.putString(KEY_WIDGET_ORDER, WIDGET_SOURCES + "," + saved);
            }
            e.apply();
        }
        String raw = sp.getString(KEY_WIDGET_ORDER, "");
        List<String> out = new ArrayList<String>();
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(",")) {
                String p = part.trim();
                if (!p.isEmpty() && !out.contains(p)) {
                    for (String def : DEFAULT_WIDGET_IDS) {
                        if (def.equals(p)) {
                            out.add(p);
                            break;
                        }
                    }
                }
            }
        }
        for (String def : DEFAULT_WIDGET_IDS) {
            if (!out.contains(def)) {
                out.add(def);
            }
        }
        return out;
    }

    public static void setWidgetOrder(Context c, List<String> order) {
        if (order == null) return;
        prefs(c).edit().putString(KEY_WIDGET_ORDER, TextUtils.join(",", order)).apply();
    }

    public static List<String> getEnabledWidgetOrder(Context c) {
        List<String> out = new ArrayList<String>();
        for (String id : getWidgetOrder(c)) {
            if (isWidgetEnabledById(c, id)) {
                out.add(id);
            }
        }
        return out;
    }

    public static boolean isWidgetEnabledById(Context c, String id) {
        if (WIDGET_SOURCES.equals(id)) return isWidgetSourcesEnabled(c);
        if (WIDGET_FOOTBALL.equals(id)) return isWidgetFootballEnabled(c);
        if (WIDGET_RELEASES.equals(id)) return isWidgetReleasesEnabled(c);
        if (WIDGET_WEATHER.equals(id)) return isWidgetWeatherEnabled(c);
        if (WIDGET_NEWS.equals(id)) return isWidgetNewsEnabled(c);
        if (WIDGET_TOP_MEDIA.equals(id)) return isWidgetTopMediaEnabled(c);
        return false;
    }

    public static void setWidgetEnabledById(Context c, String id, boolean en) {
        if (WIDGET_SOURCES.equals(id)) setWidgetSourcesEnabled(c, en);
        else if (WIDGET_FOOTBALL.equals(id)) setWidgetFootballEnabled(c, en);
        else if (WIDGET_RELEASES.equals(id)) setWidgetReleasesEnabled(c, en);
        else if (WIDGET_WEATHER.equals(id)) setWidgetWeatherEnabled(c, en);
        else if (WIDGET_NEWS.equals(id)) setWidgetNewsEnabled(c, en);
        else if (WIDGET_TOP_MEDIA.equals(id)) setWidgetTopMediaEnabled(c, en);
    }

    public static String getWidgetTitleById(Context c, String id) {
        if (WIDGET_SOURCES.equals(id)) return I18n.t(c, "dash_sources");
        if (WIDGET_FOOTBALL.equals(id)) return I18n.t(c, "dash_football");
        if (WIDGET_RELEASES.equals(id)) return I18n.t(c, "dash_latest_releases");
        if (WIDGET_WEATHER.equals(id)) return I18n.t(c, "weather");
        if (WIDGET_NEWS.equals(id)) return I18n.t(c, "dash_news");
        if (WIDGET_TOP_MEDIA.equals(id)) return I18n.t(c, "dash_top_media");
        return id;
    }

    public static String getWidgetEmojiById(String id) {
        if (WIDGET_SOURCES.equals(id)) return "🎮  ";
        if (WIDGET_FOOTBALL.equals(id)) return "⚽  ";
        if (WIDGET_RELEASES.equals(id)) return "🎬  ";
        if (WIDGET_WEATHER.equals(id)) return "⛅  ";
        if (WIDGET_NEWS.equals(id)) return "📰  ";
        if (WIDGET_TOP_MEDIA.equals(id)) return "🎥  ";
        return "";
    }

    public static boolean isMovingWidget() {
        return sMovingWidgetId != null;
    }

    public static void startMovingWidget(MainActivity host, String widgetId) {
        sMovingWidgetId = widgetId;
        refreshHostDashboardAndFocus(host, widgetId);
    }

    public static void endMovingWidget(MainActivity host) {
        String id = sMovingWidgetId;
        sMovingWidgetId = null;
        if (host != null && id != null) {
            refreshHostDashboardAndFocus(host, id);
        }
    }

    public static boolean moveWidgetInEnabledOrder(Context c, String widgetId, int delta) {
        List<String> enabled = getEnabledWidgetOrder(c);
        int curIdx = enabled.indexOf(widgetId);
        int targetIdx = curIdx + delta;
        if (curIdx < 0 || targetIdx < 0 || targetIdx >= enabled.size()) return false;
        String otherId = enabled.get(targetIdx);
        List<String> full = new ArrayList<String>(getWidgetOrder(c));
        int fullA = full.indexOf(widgetId);
        int fullB = full.indexOf(otherId);
        if (fullA < 0 || fullB < 0) return false;
        Collections.swap(full, fullA, fullB);
        setWidgetOrder(c, full);
        return true;
    }

    public static void moveActiveWidget(MainActivity host, int delta) {
        String id = sMovingWidgetId;
        if (host == null || id == null) return;
        if (moveWidgetInEnabledOrder(host, id, delta)) {
            Clicks.move(host, 0);
            refreshHostDashboardAndFocus(host, id);
        }
    }

    public static void refreshHostDashboardAndFocus(final MainActivity host, final String focusWidgetId) {
        if (host == null) return;
        if (host.sceneHome != null && Scenes.isScenesLook(host)) {
            host.sceneHome.refreshDashboard();
            if (focusWidgetId != null) {
                List<String> en = getEnabledWidgetOrder(host);
                int idx = en.indexOf(focusWidgetId);
                if (idx >= 0 && idx < host.sceneHome.dashboardCards.size()) {
                    host.sceneHome.dashboardCards.get(idx).requestFocus();
                }
            }
        } else {
            host.refreshClassicDashboard();
            if (focusWidgetId != null) {
                List<String> en = getEnabledWidgetOrder(host);
                int idx = en.indexOf(focusWidgetId);
                if (idx >= 0 && idx < host.classicDashboardCards.size()) {
                    host.classicDashboardCards.get(idx).requestFocus();
                }
            }
        }
    }

    public static boolean hasAnyWidgetEnabled(Context c) {
        return isEnabled(c) && (isWidgetSourcesEnabled(c)
                || isWidgetFootballEnabled(c)
                || isWidgetReleasesEnabled(c)
                || isWidgetWeatherEnabled(c)
                || isWidgetNewsEnabled(c)
                || isWidgetTopMediaEnabled(c));
    }

    public static boolean isAnyEnabled(Context c) {
        return hasAnyWidgetEnabled(c);
    }

    // =========================================================================
    // 0. LATEST RELEASES COUNTRY SETTINGS (AUTO UI-LANG SYNC + MULTI-COUNTRY)
    // =========================================================================

    public static String defaultCountryForUiLang(Context c) {
        String lang = I18n.getLang(c);
        if ("de".equals(lang)) return "DE";
        if ("fr".equals(lang)) return "FR";
        if ("es".equals(lang)) return "ES";
        if ("it".equals(lang)) return "IT";
        if ("pt-BR".equals(lang)) return "BR";
        if ("pt".equals(lang)) return "PT";
        if ("ru".equals(lang)) return "RU";
        if ("pl".equals(lang)) return "PL";
        if ("sv".equals(lang)) return "SE";
        return "US";
    }

    public static String getCountryDisplayName(String code) {
        if (code == null) return "";
        for (String[] row : RELEASES_COUNTRIES) {
            if (row[0].equalsIgnoreCase(code)) return row[1];
        }
        return code.toUpperCase(Locale.US);
    }

    public static boolean isReleasesCountryAuto(Context c) {
        SharedPreferences sp = prefs(c);
        if (!sp.getBoolean(KEY_RELEASES_AUTO, true)) {
            Set<String> custom = sp.getStringSet(KEY_RELEASES_SET, null);
            return custom == null || custom.isEmpty();
        }
        return true;
    }

    public static void setReleasesCountryAuto(Context c, boolean auto) {
        SharedPreferences.Editor ed = prefs(c).edit().putBoolean(KEY_RELEASES_AUTO, auto);
        if (auto) {
            ed.remove(KEY_RELEASES_SET);
        }
        ed.remove(KEY_RELEASES_CACHE_JSON)
          .remove(KEY_RELEASES_CACHE_SIG)
          .remove(KEY_RELEASES_CACHE_TS)
          .apply();
    }

    public static List<String> getEffectiveReleasesCountries(Context c) {
        if (isReleasesCountryAuto(c)) {
            List<String> single = new ArrayList<String>(1);
            single.add(defaultCountryForUiLang(c));
            return single;
        }
        Set<String> raw = prefs(c).getStringSet(KEY_RELEASES_SET, Collections.<String>emptySet());
        List<String> ordered = new ArrayList<String>();
        for (String[] row : RELEASES_COUNTRIES) {
            if (raw != null && raw.contains(row[0])) {
                ordered.add(row[0]);
            }
        }
        if (ordered.isEmpty()) {
            ordered.add(defaultCountryForUiLang(c));
        }
        return ordered;
    }

    public static boolean isReleasesCountrySelected(Context c, String code) {
        if (code == null) return false;
        if (isReleasesCountryAuto(c)) {
            return code.equalsIgnoreCase(defaultCountryForUiLang(c));
        }
        Set<String> raw = prefs(c).getStringSet(KEY_RELEASES_SET, Collections.<String>emptySet());
        return raw != null && raw.contains(code.toUpperCase(Locale.US));
    }

    public static void toggleReleasesCountry(Context c, String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) return;
        String cc = countryCode.trim().toUpperCase(Locale.US);
        SharedPreferences sp = prefs(c);
        Set<String> next = new HashSet<String>();
        if (isReleasesCountryAuto(c)) {
            String autoCode = defaultCountryForUiLang(c);
            next.add(autoCode);
            if (cc.equalsIgnoreCase(autoCode)) {
                // Already the active auto country; switch to explicit custom selection of this country
            } else {
                next.add(cc);
            }
        } else {
            Set<String> cur = sp.getStringSet(KEY_RELEASES_SET, Collections.<String>emptySet());
            if (cur != null) next.addAll(cur);
            if (next.contains(cc)) {
                next.remove(cc);
            } else {
                next.add(cc);
            }
        }
        if (next.isEmpty()) {
            setReleasesCountryAuto(c, true);
        } else {
            sp.edit()
                    .putBoolean(KEY_RELEASES_AUTO, false)
                    .putStringSet(KEY_RELEASES_SET, next)
                    .remove(KEY_RELEASES_CACHE_JSON)
                    .remove(KEY_RELEASES_CACHE_SIG)
                    .remove(KEY_RELEASES_CACHE_TS)
                    .apply();
        }
    }

    public static String getReleasesCountriesSummary(Context c) {
        if (isReleasesCountryAuto(c)) {
            String cc = defaultCountryForUiLang(c);
            return I18n.t(c, "releases_country_auto") + " (" + cc + ")";
        }
        List<String> codes = getEffectiveReleasesCountries(c);
        if (codes.size() == 1) {
            return getCountryDisplayName(codes.get(0));
        }
        return TextUtils.join(", ", codes);
    }

    private static String getReleasesCacheSignature(Context c) {
        return I18n.getLang(c) + ":" + TextUtils.join(",", getEffectiveReleasesCountries(c));
    }

    public static List<Suggestions.Item> getCachedReleases(Context c) {
        SharedPreferences sp = prefs(c);
        String sig = getReleasesCacheSignature(c);
        String savedSig = sp.getString(KEY_RELEASES_CACHE_SIG, "");
        String raw = sp.getString(KEY_RELEASES_CACHE_JSON, null);
        if (raw != null && sig.equals(savedSig)) {
            try {
                JSONArray arr = new JSONArray(raw);
                List<Suggestions.Item> out = new ArrayList<Suggestions.Item>();
                for (int i = 0; i < arr.length(); i++) {
                    out.add(Suggestions.Item.fromJson(arr.getJSONObject(i)));
                }
                if (!out.isEmpty()) return out;
            } catch (Throwable ignored) {}
        }
        return Collections.emptyList();
    }

    public interface ReleasesCallback {
        void onResult(List<Suggestions.Item> items);
    }

    public static void fetchLatestReleases(final Context c, final boolean force, final ReleasesCallback cb) {
        final SharedPreferences sp = prefs(c);
        final String sig = getReleasesCacheSignature(c);
        long now = System.currentTimeMillis();
        long lastTs = sp.getLong(KEY_RELEASES_CACHE_TS, 0L);
        if (!force && sig.equals(sp.getString(KEY_RELEASES_CACHE_SIG, "")) && (now - lastTs) < 60 * 60_000L) {
            List<Suggestions.Item> cached = getCachedReleases(c);
            if (!cached.isEmpty()) {
                if (cb != null) cb.onResult(cached);
                return;
            }
        }
        final List<String> countries = getEffectiveReleasesCountries(c);
        EXEC.execute(() -> {
            try {
                final List<Suggestions.Item> fetched = Suggestions.fetchReleasesForCountries(c, countries);
                if (fetched != null && !fetched.isEmpty()) {
                    JSONArray arr = new JSONArray();
                    for (Suggestions.Item it : fetched) {
                        arr.put(it.toJson());
                    }
                    sp.edit()
                            .putString(KEY_RELEASES_CACHE_JSON, arr.toString())
                            .putString(KEY_RELEASES_CACHE_SIG, sig)
                            .putLong(KEY_RELEASES_CACHE_TS, System.currentTimeMillis())
                            .apply();
                    if (cb != null) {
                        MAIN.post(() -> cb.onResult(fetched));
                    }
                    return;
                }
            } catch (Throwable ignored) {}
            final List<Suggestions.Item> fallback = getCachedReleases(c);
            if (cb != null) {
                MAIN.post(() -> cb.onResult(fallback));
            }
        });
    }

    // =========================================================================
    // 2. FOOTBALL MATCHES MODEL & FETCHER
    // =========================================================================

    public static final class MatchItem {
        public String id = "";
        public String homeTeam = "";
        public String awayTeam = "";
        public String homeLogo = "";
        public String awayLogo = "";
        public String homeScore = "";
        public String awayScore = "";
        public String statusText = "";
        public boolean isLive = false;
        public boolean isFinished = false;
        public boolean isFav = false;
        public String leagueName = "";
        public long startEpochMs = 0L;
    }

    private static final android.util.LruCache<String, Bitmap> sLogoCache = new android.util.LruCache<String, Bitmap>(96);
    private static final java.util.WeakHashMap<View, Runnable> sCardRefreshers = new java.util.WeakHashMap<View, Runnable>();

    public static void refreshCardsInPlace(List<View> cards) {
        if (cards == null) return;
        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            if (card == null) continue;
            Runnable r = sCardRefreshers.get(card);
            if (r != null) r.run();
        }
    }

    public static void clearImageCaches() {
        sLogoCache.evictAll();
    }

    public static void loadLogoAsync(final String url, final int sizePx, final ImageView target) {
        if (target == null) return;
        if (url == null || url.isEmpty()) {
            target.setVisibility(View.GONE);
            return;
        }
        target.setTag(url);
        Bitmap cached = sLogoCache.get(url);
        if (cached != null && !cached.isRecycled()) {
            target.setVisibility(View.VISIBLE);
            target.setImageBitmap(cached);
            return;
        }
        target.setVisibility(View.INVISIBLE);
        EXEC.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String fetchUrl = url;
                if (fetchUrl.contains("espncdn.com") && !fetchUrl.contains("scale=")) {
                    fetchUrl = fetchUrl + (fetchUrl.contains("?") ? "&" : "?") + "w=80&h=80&scale=crop";
                }
                conn = (HttpURLConnection) new URL(fetchUrl).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "tvOSLauncher/2.2");
                InputStream in = conn.getInputStream();
                Bitmap raw = BitmapFactory.decodeStream(in);
                in.close();
                if (raw != null) {
                    final Bitmap scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true);
                    if (scaled != raw && !raw.isRecycled()) raw.recycle();
                    sLogoCache.put(url, scaled);
                    MAIN.post(() -> {
                        if (url.equals(target.getTag())) {
                            target.setVisibility(View.VISIBLE);
                            target.setImageBitmap(scaled);
                        }
                    });
                }
            } catch (Throwable ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static final String[] POPULAR_TEAMS = {
        "Napoli", "Inter", "AC Milan", "Juventus", "AS Roma", "Lazio", "Atalanta", "Fiorentina", "Bologna", "Como",
        "Real Madrid", "Barcelona", "Atlético Madrid", "Real Sociedad", "Athletic Club",
        "Manchester City", "Arsenal", "Liverpool", "Chelsea", "Manchester United", "Tottenham", "Aston Villa", "Newcastle",
        "Bayern Munich", "Borussia Dortmund", "Bayer Leverkusen", "RB Leipzig",
        "PSG", "Marseille", "Monaco", "Lyon",
        "Ajax", "PSV", "Feyenoord", "Benfica", "Sporting CP", "Porto"
    };

    /** Leagues the user can pick; "all" means every competition in {@link #EU_COMPETITIONS}. */
    public static final String[][] LEAGUES = {
        { "all", "All Leagues & Cups" },
        { "uefa.champions", "UEFA Champions League" },
        { "uefa.europa", "UEFA Europa League" },
        { "uefa.europa.conf", "UEFA Conference League" },
        { "uefa.nations", "UEFA Nations League" },
        { "fifa.world", "FIFA World Cup" },
        { "ita.1", "Serie A (Italy)" },
        { "ita.coppa_italia", "Coppa Italia" },
        { "eng.1", "Premier League (England)" },
        { "esp.1", "LaLiga (Spain)" },
        { "ger.1", "Bundesliga (Germany)" },
        { "fra.1", "Ligue 1 (France)" },
        { "ned.1", "Eredivisie (Netherlands)" },
        { "por.1", "Liga Portugal" }
    };

    /**
     * European competitions shown under "All Leagues & Cups": ESPN league id (the "l:" part of an
     * event uid), short name. ESPN's all-soccer scoreboard covers the world, so it is filtered here.
     */
    private static final String[][] EU_COMPETITIONS = {
        { "775", "UCL" }, { "776", "UEL" }, { "20296", "UECL" }, { "5462", "Supercoppa UEFA" },
        { "2395", "Nations League" }, { "606", "Mondiali" }, { "786", "Qualif. Mondiali" },
        { "781", "Europei" }, { "3947", "Qualif. Europei" }, { "3922", "Amichevole" }, { "5501", "Mondiale Club" },
        { "730", "Serie A" }, { "3956", "Coppa Italia" }, { "8103", "Supercoppa" },
        { "700", "Premier League" }, { "3918", "FA Cup" }, { "3920", "Carabao Cup" },
        { "740", "LaLiga" }, { "3951", "Copa del Rey" },
        { "720", "Bundesliga" }, { "3954", "DFB-Pokal" },
        { "710", "Ligue 1" }, { "3952", "Coupe de France" },
        { "725", "Eredivisie" }, { "715", "Liga Portugal" }
    };

    private static String euCompetitionName(String leagueId) {
        if (leagueId == null) return null;
        for (String[] lg : EU_COMPETITIONS) {
            if (lg[0].equals(leagueId)) return lg[1];
        }
        return null;
    }

    /** The league id inside an ESPN event uid such as "s:600~l:775~e:401915380". */
    private static String leagueIdFromUid(String uid) {
        if (uid == null) return null;
        int i = uid.indexOf("~l:");
        if (i < 0) return null;
        int end = uid.indexOf('~', i + 3);
        return end < 0 ? uid.substring(i + 3) : uid.substring(i + 3, end);
    }

    public static Set<String> getFavoriteTeams(Context c) {
        SharedPreferences sp = prefs(c);
        Set<String> raw = sp.getStringSet(KEY_FAV_TEAMS, null);
        if (raw == null) {
            Set<String> def = new HashSet<String>();
            def.add("Napoli");
            return def;
        }
        Set<String> favs = new HashSet<String>(raw);
        // Once: drop the old non-European defaults (Turkey, Azerbaijan...) a saved set still carries.
        if (!sp.getBoolean(KEY_FAV_TEAMS_EU, false)) {
            favs.removeAll(java.util.Arrays.asList("Qarabağ", "Neftçi", "Sabah", "Zirə", "Turan Tovuz",
                    "Galatasaray", "Fenerbahçe", "Beşiktaş", "Trabzonspor", "Başakşehir",
                    "Al-Nassr", "Al-Hilal", "Al-Ittihad", "Inter Miami", "Boca Juniors", "River Plate", "Flamengo"));
            if (favs.isEmpty()) favs.add("Napoli");
            sp.edit().putStringSet(KEY_FAV_TEAMS, favs).putBoolean(KEY_FAV_TEAMS_EU, true).apply();
        }
        return favs;
    }

    public static void toggleFavoriteTeam(Context c, String team) {
        Set<String> favs = getFavoriteTeams(c);
        if (favs.contains(team)) {
            favs.remove(team);
        } else {
            favs.add(team);
        }
        prefs(c).edit().putStringSet(KEY_FAV_TEAMS, favs).apply();
    }

    public static void addCustomFavoriteTeam(Context c, String team) {
        if (team == null || team.trim().isEmpty()) return;
        Set<String> favs = getFavoriteTeams(c);
        favs.add(team.trim());
        prefs(c).edit().putStringSet(KEY_FAV_TEAMS, favs).apply();
    }

    public static String getSelectedLeague(Context c) {
        String code = prefs(c).getString(KEY_FOOTBALL_LEAGUE, "all");
        for (String[] lg : LEAGUES) {
            if (lg[0].equals(code)) return code;
        }
        return "all";
    }

    public static void setSelectedLeague(Context c, String code) {
        prefs(c).edit()
                .putString(KEY_FOOTBALL_LEAGUE, code)
                .putLong(KEY_FOOTBALL_CACHE_TS, 0L)
                .apply();
    }

    public interface FootballCallback {
        void onResult(List<MatchItem> matches);
    }

    private static List<MatchItem> sMemMatches = new ArrayList<MatchItem>();

    public static List<MatchItem> getCachedMatches(Context c) {
        if (!sMemMatches.isEmpty()) return new ArrayList<MatchItem>(sMemMatches);
        String raw = prefs(c).getString(KEY_FOOTBALL_CACHE_JSON, null);
        if (raw != null) {
            List<MatchItem> parsed = parseEspnEvents(c, raw, "", true);
            if (!parsed.isEmpty()) {
                sMemMatches = parsed;
                return parsed;
            }
        }
        return Collections.emptyList();
    }

    /** How many days of fixtures to load, today included. */
    private static final int FOOTBALL_DAYS = 7;

    /**
     * ESPN's scoreboard ignores date ranges ("dates=20261124-20261201" returns no events), so the
     * fixtures are read one day at a time.
     */
    private static List<String> scoreboardDays() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Calendar cal = Calendar.getInstance();
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < FOOTBALL_DAYS; i++) {
            out.add(fmt.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }

    public static void fetchFootballMatches(final Context c, final boolean force, final FootballCallback cb) {
        final SharedPreferences sp = prefs(c);
        long now = System.currentTimeMillis();
        long lastTs = sp.getLong(KEY_FOOTBALL_CACHE_TS, 0L);
        boolean hasLive = false;
        for (MatchItem m : sMemMatches) {
            if (m.isLive) {
                hasLive = true;
                break;
            }
        }
        long ttlMs = hasLive ? 120_000L : (10 * 60_000L);
        if (!force && !sMemMatches.isEmpty() && (now - lastTs < ttlMs)) {
            if (cb != null) cb.onResult(new ArrayList<MatchItem>(sMemMatches));
            return;
        }
        EXEC.execute(() -> {
            List<MatchItem> all = new ArrayList<MatchItem>();
            Set<String> seenIds = new HashSet<String>();
            JSONArray kept = new JSONArray();
            String league = getSelectedLeague(c);
            boolean everything = "all".equals(league);
            List<String> days = scoreboardDays();
            for (int d = 0; d < days.size(); d++) {
                try {
                    String url = "https://site.api.espn.com/apis/site/v2/sports/soccer/"
                            + (everything ? "all" : league) + "/scoreboard?dates=" + days.get(d)
                            // The all-soccer board stops at 100 events a day without it (a Saturday has ~250).
                            + "&limit=400";
                    String json = httpGet(url);
                    if (json == null) continue;
                    JSONArray events = new JSONObject(json).optJSONArray("events");
                    if (events == null) continue;
                    JSONArray dayKept = new JSONArray();
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject ev = events.optJSONObject(i);
                        if (ev == null) continue;
                        if (everything && euCompetitionName(leagueIdFromUid(ev.optString("uid"))) == null) continue;
                        if (seenIds.add(ev.optString("id", ev.optString("uid")))) {
                            dayKept.put(ev);
                            kept.put(ev);
                        }
                    }
                    JSONObject wrap = new JSONObject().put("events", dayKept);
                    all.addAll(parseEspnEvents(c, wrap.toString(), everything ? "" : league, everything));
                    // Today first: the card fills in while the rest of the week loads.
                    if (d == 0 && cb != null && !all.isEmpty()) {
                        final List<MatchItem> initial = new ArrayList<MatchItem>(all);
                        sortMatches(c, initial);
                        MAIN.post(() -> cb.onResult(initial));
                    }
                } catch (Throwable ignored) {}
            }
            if (kept.length() > 0) {
                try {
                    sp.edit()
                            .putString(KEY_FOOTBALL_CACHE_JSON, new JSONObject().put("events", kept).toString())
                            .putLong(KEY_FOOTBALL_CACHE_TS, System.currentTimeMillis())
                            .apply();
                } catch (Throwable ignored) {}
            }

            sortMatches(c, all);
            if (!all.isEmpty()) {
                sMemMatches = all;
            }
            final List<MatchItem> result = !all.isEmpty() ? all : getCachedMatches(c);
            if (cb != null) {
                MAIN.post(() -> cb.onResult(result));
            }
        });
    }

    private static void sortMatches(Context c, List<MatchItem> list) {
        final Set<String> favs = getFavoriteTeams(c);
        for (MatchItem m : list) {
            m.isFav = isTeamFav(m.homeTeam, favs) || isTeamFav(m.awayTeam, favs);
        }
        Collections.sort(list, new Comparator<MatchItem>() {
            @Override
            public int compare(MatchItem a, MatchItem b) {
                if (a.isFav != b.isFav) return a.isFav ? -1 : 1;
                if (a.isLive != b.isLive) return a.isLive ? -1 : 1;
                if (a.isFinished != b.isFinished) return a.isFinished ? 1 : -1;
                return Long.compare(a.startEpochMs, b.startEpochMs);
            }
        });
    }

    private static boolean isTeamFav(String teamName, Set<String> favs) {
        if (teamName == null || favs == null) return false;
        String lower = normalizeTeamName(teamName);
        for (String f : favs) {
            if (f != null && !f.isEmpty() && lower.contains(normalizeTeamName(f))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTeamName(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replace('ğ', 'g').replace('ə', 'e').replace('ı', 'i')
                .replace('ş', 's').replace('ç', 'c').replace('ö', 'o').replace('ü', 'u');
    }

    /** onlyEu keeps the events of {@link #EU_COMPETITIONS} and names their league from it. */
    private static List<MatchItem> parseEspnEvents(Context c, String rawJson, String fallbackLeague, boolean onlyEu) {
        List<MatchItem> out = new ArrayList<MatchItem>();
        try {
            JSONObject root = new JSONObject(rawJson);
            String leagueTitle = fallbackLeague;
            JSONArray leagues = root.optJSONArray("leagues");
            if (leagues != null && leagues.length() > 0) {
                JSONObject lg = leagues.optJSONObject(0);
                if (lg != null) leagueTitle = lg.optString("abbreviation", lg.optString("name", fallbackLeague));
            }
            JSONArray events = root.optJSONArray("events");
            if (events == null) return out;
            Set<String> favs = getFavoriteTeams(c);
            for (int i = 0; i < events.length(); i++) {
                JSONObject ev = events.optJSONObject(i);
                if (ev == null) continue;
                JSONArray comps = ev.optJSONArray("competitions");
                if (comps == null || comps.length() == 0) continue;
                JSONObject comp = comps.optJSONObject(0);
                JSONArray competitors = comp.optJSONArray("competitors");
                if (competitors == null || competitors.length() < 2) continue;

                MatchItem item = new MatchItem();
                item.id = ev.optString("id", "");
                JSONObject evLeague = ev.optJSONObject("league");
                String euName = euCompetitionName(leagueIdFromUid(ev.optString("uid")));
                if (onlyEu && euName == null) continue;
                if (euName != null) {
                    item.leagueName = euName;
                } else if (evLeague != null) {
                    item.leagueName = evLeague.optString("abbreviation", evLeague.optString("name", leagueTitle));
                } else {
                    item.leagueName = leagueTitle;
                }
                for (int j = 0; j < competitors.length(); j++) {
                    JSONObject teamEntry = competitors.optJSONObject(j);
                    if (teamEntry == null) continue;
                    String homeAway = teamEntry.optString("homeAway", j == 0 ? "home" : "away");
                    JSONObject teamObj = teamEntry.optJSONObject("team");
                    String tName = teamObj != null
                            ? teamObj.optString("shortDisplayName", teamObj.optString("displayName", "Team"))
                            : "Team";
                    String tLogo = "";
                    if (teamObj != null) {
                        tLogo = teamObj.optString("logo", "");
                        if (tLogo.isEmpty()) {
                            JSONArray logos = teamObj.optJSONArray("logos");
                            if (logos != null && logos.length() > 0) {
                                JSONObject l0 = logos.optJSONObject(0);
                                if (l0 != null) tLogo = l0.optString("href", "");
                            }
                        }
                    }
                    String score = teamEntry.optString("score", "0");
                    if ("home".equalsIgnoreCase(homeAway)) {
                        item.homeTeam = tName;
                        item.homeLogo = tLogo;
                        item.homeScore = score;
                    } else {
                        item.awayTeam = tName;
                        item.awayLogo = tLogo;
                        item.awayScore = score;
                    }
                }

                JSONObject status = comp.optJSONObject("status");
                if (status == null) status = ev.optJSONObject("status");
                if (status != null) {
                    JSONObject type = status.optJSONObject("type");
                    if (type != null) {
                        String state = type.optString("state", "pre");
                        item.isLive = "in".equalsIgnoreCase(state);
                        item.isFinished = "post".equalsIgnoreCase(state);
                        item.statusText = type.optString("shortDetail", type.optString("detail", ""));
                    }
                }

                String dateIso = ev.optString("date", "");
                if (!dateIso.isEmpty()) {
                    try {
                        String cleanIso = dateIso.length() >= 16 ? dateIso.substring(0, 16) + "Z" : dateIso;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        Date dt = sdf.parse(cleanIso);
                        if (dt != null) {
                            item.startEpochMs = dt.getTime();
                            if (!item.isLive && !item.isFinished) {
                                Calendar nowCal = Calendar.getInstance();
                                Calendar matchCal = Calendar.getInstance();
                                matchCal.setTime(dt);
                                boolean sameDay = nowCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR)
                                        && nowCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR);
                                SimpleDateFormat localFmt = new SimpleDateFormat(
                                        sameDay ? "HH:mm" : "EEE d MMM HH:mm", I18n.locale(c));
                                item.statusText = localFmt.format(dt);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                item.isFav = isTeamFav(item.homeTeam, favs) || isTeamFav(item.awayTeam, favs);
                out.add(item);
            }
        } catch (Throwable ignored) {}
        return out;
    }

    // =========================================================================
    // 3. DASHBOARD ROW UI BUILDER
    // =========================================================================

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public interface NavBridge {
        void onFocusUp();
        void onFocusDown();
        void onOpenWeather();
    }

    /**
     * Builds the 4-widget horizontal dashboard row.
     */
    public static View buildDashboardView(
            final MainActivity host,
            final MainActivity.LiquidGlass.Backdrop glassSource,
            int screenW,
            int screenH,
            float unit,
            final List<View> outCards) {
        return buildDashboardViewInternal(host, outCards, null);
    }

    public static FrameLayout buildRow(
            final MainActivity host,
            float unit,
            int screenW,
            int side,
            final List<View> outCards,
            final MainActivity.LiquidGlass.Backdrop glassSource) {
        FrameLayout wrap = new FrameLayout(host);
        wrap.setClipChildren(false);
        wrap.setClipToPadding(false);
        LinearLayout box = (LinearLayout) buildDashboardViewInternal(host, outCards, null);
        box.setPadding(side, 0, side, 0);
        wrap.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    public static View buildDashboardView(final MainActivity host, final NavBridge nav) {
        return buildDashboardViewInternal(host, null, nav);
    }

    private static View buildDashboardViewInternal(
            final MainActivity host,
            final List<View> outCards,
            final NavBridge nav) {
        LinearLayout outer = new LinearLayout(host);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setGravity(Gravity.CENTER_VERTICAL);
        outer.setClipChildren(false);
        outer.setClipToPadding(false);

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);

        final List<View> cards = new ArrayList<View>();
        final List<String> enabledIds = getEnabledWidgetOrder(host);
        for (int i = 0; i < enabledIds.size(); i++) {
            final String wid = enabledIds.get(i);
            final View card = createWidgetCardById(host, wid, nav);
            if (card == null) continue;
            card.setLongClickable(true);
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (isMovingWidget()) {
                        endMovingWidget(host);
                        return true;
                    }
                    showWidgetLongPressMenu(host, v, wid);
                    return true;
                }
            });
            if (wid.equals(sMovingWidgetId)) {
                GradientDrawable moveRing = new GradientDrawable();
                moveRing.setCornerRadius(dp(host, 15));
                moveRing.setColor(0x2238BDF8);
                moveRing.setStroke(dp(host, 2), 0xFF38BDF8);
                card.setForeground(moveRing);
                android.animation.ObjectAnimator wig = android.animation.ObjectAnimator.ofFloat(card, View.ROTATION, -1.5f, 1.5f);
                wig.setDuration(130);
                wig.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                wig.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                wig.start();
            }
            cards.add(card);
        }
        if (outCards != null) {
            outCards.clear();
            outCards.addAll(cards);
        }

        int screenW = host.getResources().getDisplayMetrics().widthPixels;
        int sidePad = dp(host, 48);
        int gap = dp(host, 14);
        int count = Math.max(1, cards.size());
        int totalAvailW = screenW - sidePad * 2 - gap * (Math.min(4, count) - 1);
        int cardW = count <= 4
                ? Math.min(dp(host, 290), Math.max(dp(host, 180), totalAvailW / count))
                : dp(host, 252);
        int cardH = dp(host, 96);

        for (int i = 0; i < cards.size(); i++) {
            final int idx = i;
            final View card = cards.get(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardW, cardH);
            if (i > 0) lp.leftMargin = gap;
            row.addView(card, lp);

            if (nav != null) {
                card.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                        if (isMovingWidget()) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                moveActiveWidget(host, -1);
                                return true;
                            }
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                moveActiveWidget(host, 1);
                                return true;
                            }
                            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                                    || keyCode == KeyEvent.KEYCODE_ENTER
                                    || keyCode == KeyEvent.KEYCODE_BACK
                                    || keyCode == KeyEvent.KEYCODE_DPAD_UP
                                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                endMovingWidget(host);
                                return true;
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            if (idx > 0) {
                                cards.get(idx - 1).requestFocus();
                            }
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (idx + 1 < cards.size()) {
                                cards.get(idx + 1).requestFocus();
                            }
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            nav.onFocusUp();
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            nav.onFocusDown();
                            return true;
                        }
                        return false;
                    }
                });
            }
        }

        if (count > 4) {
            android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(host);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            hsv.setClipChildren(false);
            hsv.setClipToPadding(false);
            int pad = dp(host, 6);
            row.setPadding(pad, pad, pad, pad);
            hsv.addView(row, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            outer.addView(hsv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }
        return row;
    }

    private static View createWidgetCardById(MainActivity host, String id, NavBridge nav) {
        View card = null;
        if (WIDGET_SOURCES.equals(id)) card = createSourcesWidgetCard(host);
        else if (WIDGET_FOOTBALL.equals(id)) card = createFootballWidgetCard(host);
        else if (WIDGET_RELEASES.equals(id)) card = createReleasesWidgetCard(host);
        else if (WIDGET_WEATHER.equals(id)) card = createWeatherWidgetCard(host, nav);
        else if (WIDGET_NEWS.equals(id)) card = createNewsWidgetCard(host);
        else if (WIDGET_TOP_MEDIA.equals(id)) card = createTopMediaWidgetCard(host);
        return card;
    }

    public static void showWidgetLongPressMenu(final MainActivity host, final View anchorCard, final String widgetId) {
        List<String> labels = new ArrayList<String>();
        List<Runnable> actions = new ArrayList<Runnable>();
        labels.add(I18n.s(host, "Move", "Taşı"));
        actions.add(new Runnable() {
            @Override
            public void run() {
                startMovingWidget(host, widgetId);
            }
        });
        labels.add(I18n.s(host, "Disable", "Kapat"));
        actions.add(new Runnable() {
            @Override
            public void run() {
                setWidgetEnabledById(host, widgetId, false);
                refreshHostDashboardAndFocus(host, null);
            }
        });
        host.showMenu(anchorCard, getWidgetTitleById(host, widgetId), labels, actions, 1);
    }

    private static FrameLayout createBaseGlassCard(final MainActivity host, final int accentColor) {
        final FrameLayout card = new FrameLayout(host) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            public void setOnClickListener(final OnClickListener l) {
                super.setOnClickListener(l == null ? null : new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isMovingWidget()) {
                            endMovingWidget(host);
                            return;
                        }
                        l.onClick(v);
                    }
                });
            }
        };
        float r = dp(host, 15);
        MainActivity.LiquidGlass glass = new MainActivity.LiquidGlass(
                r, 0x5C1E141C, host.glassSource, host.screenW, host.screenH, host.unit);
        glass.lens = dp(host, 6);
        glass.rimStrength = 0.42f;
        card.setBackground(glass);

        final GradientDrawable focusRing = new GradientDrawable();
        focusRing.setCornerRadius(r);
        focusRing.setColor(Color.TRANSPARENT);
        focusRing.setStroke(dp(host, 2), Color.WHITE);

        card.setFocusable(true);
        card.setFocusableInTouchMode(false);
        card.setPadding(dp(host, 12), dp(host, 8), dp(host, 12), dp(host, 8));
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                card.setForeground(hasFocus ? focusRing : null);
                v.animate().cancel();
                v.animate()
                        .scaleX(hasFocus ? 1.035f : 1.0f)
                        .scaleY(hasFocus ? 1.035f : 1.0f)
                        .translationY(hasFocus ? -dp(host, 2) : 0f)
                        .translationZ(hasFocus ? dp(host, 8) : 0f)
                        .setDuration(150)
                        .setInterpolator(MainActivity.EASE)
                        .start();
            }
        });
        return card;
    }

    private static void styleUnifiedWidgetHeader(MainActivity host, View dot, TextView headerTitle, String text) {
        if (dot != null) {
            dot.setVisibility(View.GONE);
        }
        headerTitle.setText(text.toUpperCase(I18n.locale(host)));
        headerTitle.setTextColor(0xE6FFFFFF);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        headerTitle.setTypeface(Ui.font(host, "semibold"));
        headerTitle.setLetterSpacing(0.06f);
        headerTitle.setSingleLine(true);
        headerTitle.setEllipsize(TextUtils.TruncateAt.END);
    }

    // -------------------------------------------------------------------------
    // WIDGET 2: FOOTBALL MATCHES CARD (WITH CLUB LOGOS & LIVE SCORES)
    // -------------------------------------------------------------------------
    private static View createFootballWidgetCard(final MainActivity host) {
        final FrameLayout card = createBaseGlassCard(host, 0xFF38BDF8);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(host);
        head.addView(dot, new LinearLayout.LayoutParams(dp(host, 6), dp(host, 6)));

        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, dot, headerTitle, I18n.t(host, "dash_football"));
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = 0;
        head.addView(headerTitle, htlp);

        final TextView badgeTv = new TextView(host);
        badgeTv.setTextColor(0xFF34D399);
        badgeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        badgeTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        head.addView(badgeTv);

        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout matchRows = new LinearLayout(host);
        matchRows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrlp.topMargin = dp(host, 4);
        col.addView(matchRows, mrlp);

        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final String[] lastMatchSig = { "" };
        final Runnable bindMatches = new Runnable() {
            @Override
            public void run() {
                List<MatchItem> list = getCachedMatches(host);
                if (list.isEmpty()) {
                    if ("EMPTY".equals(lastMatchSig[0]) && matchRows.getChildCount() > 0) return;
                    lastMatchSig[0] = "EMPTY";
                    matchRows.removeAllViews();
                    badgeTv.setText("");
                    TextView empty = new TextView(host);
                    empty.setText(I18n.t(host, "football_loading"));
                    empty.setTextColor(0x99FFFFFF);
                    empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                    matchRows.addView(empty);
                    return;
                }
                int liveCount = 0;
                for (MatchItem m : list) if (m.isLive) liveCount++;
                int showCount = Math.min(3, list.size());
                StringBuilder sigSb = new StringBuilder().append(liveCount).append('|').append(list.get(0).leagueName);
                for (int i = 0; i < showCount; i++) {
                    MatchItem m = list.get(i);
                    sigSb.append(';').append(m.isFav).append('|').append(m.homeTeam).append('|').append(m.awayTeam)
                            .append('|').append(m.homeScore).append(':').append(m.awayScore)
                            .append('|').append(m.isLive).append('|').append(m.isFinished).append('|').append(m.statusText);
                }
                String sig = sigSb.toString();
                if (sig.equals(lastMatchSig[0]) && matchRows.getChildCount() == showCount) {
                    return;
                }
                lastMatchSig[0] = sig;
                matchRows.removeAllViews();
                badgeTv.setText(liveCount > 0 ? ("● " + liveCount + " LIVE") : list.get(0).leagueName);
                badgeTv.setTextColor(liveCount > 0 ? 0xFF4ADE80 : 0x88FFFFFF);

                int logoPx = dp(host, 14);
                for (int i = 0; i < showCount; i++) {
                    MatchItem m = list.get(i);
                    LinearLayout r = new LinearLayout(host);
                    r.setOrientation(LinearLayout.HORIZONTAL);
                    r.setGravity(Gravity.CENTER_VERTICAL);
                    if (i > 0) {
                        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rlp.topMargin = dp(host, 2);
                        r.setLayoutParams(rlp);
                    }

                    if (m.homeLogo != null && !m.homeLogo.isEmpty()) {
                        ImageView hLogo = new ImageView(host);
                        hLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(logoPx, logoPx);
                        llp.rightMargin = dp(host, 4);
                        r.addView(hLogo, llp);
                        loadLogoAsync(m.homeLogo, logoPx, hLogo);
                    }

                    TextView teams = new TextView(host);
                    String prefix = m.isFav ? "★ " : "";
                    teams.setText(prefix + m.homeTeam + " - " + m.awayTeam);
                    teams.setTextColor(m.isFav ? 0xFFFDE047 : Color.WHITE);
                    teams.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.2f);
                    teams.setTypeface(Typeface.create("sans-serif-medium", i == 0 ? Typeface.BOLD : Typeface.NORMAL));
                    teams.setSingleLine(true);
                    teams.setEllipsize(TextUtils.TruncateAt.END);
                    r.addView(teams, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    if (m.awayLogo != null && !m.awayLogo.isEmpty()) {
                        ImageView aLogo = new ImageView(host);
                        aLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        LinearLayout.LayoutParams allp = new LinearLayout.LayoutParams(logoPx, logoPx);
                        allp.leftMargin = dp(host, 3);
                        r.addView(aLogo, allp);
                        loadLogoAsync(m.awayLogo, logoPx, aLogo);
                    }

                    TextView right = new TextView(host);
                    if (m.isLive || m.isFinished) {
                        right.setText(m.homeScore + ":" + m.awayScore + (m.isLive ? " '" : ""));
                        right.setTextColor(m.isLive ? 0xFF4ADE80 : 0xCCFFFFFF);
                    } else {
                        right.setText(m.statusText != null && !m.statusText.isEmpty() ? m.statusText : "VS");
                        right.setTextColor(0x99FFFFFF);
                    }
                    right.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.8f);
                    right.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    scLp.leftMargin = dp(host, 5);
                    r.addView(right, scLp);

                    matchRows.addView(r);
                }
            }
        };

        bindMatches.run();
        fetchFootballMatches(host, false, new FootballCallback() {
            @Override
            public void onResult(List<MatchItem> matches) {
                bindMatches.run();
            }
        });
        sCardRefreshers.put(card, () -> fetchFootballMatches(host, false, matches -> bindMatches.run()));

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFootballDialog(host, bindMatches, false);
            }
        });
        return card;
    }

    // -------------------------------------------------------------------------
    // WIDGET 3: LATEST RELEASES / POPULAR CARD
    // -------------------------------------------------------------------------
    private static int sReleaseIdx = 0;

    private static View createReleasesWidgetCard(final MainActivity host) {
        final FrameLayout card = createBaseGlassCard(host, 0xFFA855F7);

        LinearLayout rootRow = new LinearLayout(host);
        rootRow.setOrientation(LinearLayout.HORIZONTAL);
        rootRow.setGravity(Gravity.CENTER_VERTICAL);

        final ImageView posterView = new ImageView(host);
        posterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable posterBg = new GradientDrawable();
        posterBg.setColor(0x33FFFFFF);
        posterBg.setCornerRadius(dp(host, 8));
        posterView.setBackground(posterBg);
        posterView.setOutlineProvider(host.roundedOutline(new float[]{dp(host, 8)}));
        posterView.setClipToOutline(true);
        rootRow.addView(posterView, new LinearLayout.LayoutParams(dp(host, 50), dp(host, 74)));

        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colp.leftMargin = dp(host, 10);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(host);
        head.addView(dot, new LinearLayout.LayoutParams(dp(host, 6), dp(host, 6)));

        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, dot, headerTitle, I18n.t(host, "dash_latest_releases"));
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = 0;
        head.addView(headerTitle, htlp);

        final TextView countryBadgeTv = new TextView(host);
        countryBadgeTv.setTextColor(0x88FFFFFF);
        countryBadgeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        countryBadgeTv.setSingleLine(true);
        head.addView(countryBadgeTv);

        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView titleTv = new TextView(host);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        titleTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleTv.setMaxLines(2);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttlp.topMargin = dp(host, 3);
        col.addView(titleTv, ttlp);

        final TextView metaTv = new TextView(host);
        metaTv.setTextColor(0xB8FFFFFF);
        metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.8f);
        metaTv.setSingleLine(true);
        metaTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mtlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mtlp.topMargin = dp(host, 2);
        col.addView(metaTv, mtlp);

        rootRow.addView(col, colp);
        card.addView(rootRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final List<Suggestions.Item> itemsRef = new ArrayList<Suggestions.Item>();
        final Runnable bindItem = new Runnable() {
            @Override
            public void run() {
                List<String> effCountries = getEffectiveReleasesCountries(host);
                countryBadgeTv.setText(TextUtils.join(",", effCountries));
                if (itemsRef.isEmpty()) {
                    titleTv.setText(I18n.t(host, "releases_loading"));
                    metaTv.setText("");
                    return;
                }
                if (sReleaseIdx >= itemsRef.size()) sReleaseIdx = 0;
                final Suggestions.Item it = itemsRef.get(sReleaseIdx);
                titleTv.setText(it.title != null ? it.title : "");
                String typeBadge = "tv".equalsIgnoreCase(it.type) || "series".equalsIgnoreCase(it.type) ? "TV Series" : "Movie";
                StringBuilder sb = new StringBuilder(typeBadge);
                if (it.year != null && !it.year.isEmpty()) sb.append(" • ").append(it.year);
                if (it.rating > 0) sb.append(String.format(Locale.US, " • ★ %.1f", it.rating));
                if (it.country != null && !it.country.isEmpty() && effCountries.size() > 1) {
                    sb.append(" • ").append(it.country);
                }
                metaTv.setText(sb.toString());

                final String imgUrl = it.backdrop;
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    if (imgUrl.equals(posterView.getTag())) {
                        return;
                    }
                    final String cacheKey = "poster:" + imgUrl;
                    Bitmap cachedBmp = sLogoCache.get(cacheKey);
                    if (cachedBmp != null && !cachedBmp.isRecycled()) {
                        posterView.setTag(imgUrl);
                        posterView.setImageBitmap(cachedBmp);
                    } else {
                        posterView.setTag(imgUrl);
                        EXEC.execute(() -> {
                            final Bitmap bmp = Suggestions.image(host, imgUrl, "w342", dp(host, 140));
                            if (bmp != null) {
                                sLogoCache.put(cacheKey, bmp);
                                MAIN.post(() -> {
                                    if (imgUrl.equals(posterView.getTag())) {
                                        posterView.setImageBitmap(bmp);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        };

        List<Suggestions.Item> cached = getCachedReleases(host);
        if (cached != null && !cached.isEmpty()) {
            itemsRef.addAll(cached);
        }
        bindItem.run();
        fetchLatestReleases(host, false, fresh -> {
            if (fresh != null && !fresh.isEmpty()) {
                itemsRef.clear();
                itemsRef.addAll(fresh);
                bindItem.run();
            }
        });
        sCardRefreshers.put(card, () -> fetchLatestReleases(host, false, fresh -> {
            if (fresh != null && !fresh.isEmpty()) {
                itemsRef.clear();
                itemsRef.addAll(fresh);
                bindItem.run();
            }
        }));

        card.setOnClickListener(v -> showReleasesDialog(host, itemsRef, () -> {
            sReleaseIdx++;
            bindItem.run();
        }));
        return card;
    }

    // -------------------------------------------------------------------------
    // WIDGET 4: WEATHER CARD
    // -------------------------------------------------------------------------
    private static View createWeatherWidgetCard(final MainActivity host, final NavBridge nav) {
        final FrameLayout card = createBaseGlassCard(host, 0xFFFBBF24);

        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(host);
        head.addView(dot, new LinearLayout.LayoutParams(dp(host, 6), dp(host, 6)));

        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, dot, headerTitle, I18n.t(host, "weather"));
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = 0;
        head.addView(headerTitle, htlp);

        final TextView cityTv = new TextView(host);
        cityTv.setTextColor(0x88FFFFFF);
        cityTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        cityTv.setSingleLine(true);
        cityTv.setEllipsize(TextUtils.TruncateAt.END);
        head.addView(cityTv);

        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout mid = new LinearLayout(host);
        mid.setOrientation(LinearLayout.HORIZONTAL);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mlp.topMargin = dp(host, 4);

        // Left column: Today
        LinearLayout todayCol = new LinearLayout(host);
        todayCol.setOrientation(LinearLayout.VERTICAL);

        TextView todayLabelTv = new TextView(host);
        todayLabelTv.setText(I18n.s(host, "Today", "Bugün"));
        todayLabelTv.setTextColor(0x99FFFFFF);
        todayLabelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        todayLabelTv.setSingleLine(true);
        todayCol.addView(todayLabelTv);

        LinearLayout todayRow = new LinearLayout(host);
        todayRow.setOrientation(LinearLayout.HORIZONTAL);
        todayRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trlp.topMargin = dp(host, 2);

        final Weather.WeatherIconView iconView = new Weather.WeatherIconView(host);
        todayRow.addView(iconView, new LinearLayout.LayoutParams(dp(host, 28), dp(host, 28)));

        LinearLayout tCol = new LinearLayout(host);
        tCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tclp.leftMargin = dp(host, 6);

        final TextView tempTv = new TextView(host);
        tempTv.setTextColor(Color.WHITE);
        tempTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        tempTv.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        tempTv.setSingleLine(true);
        tCol.addView(tempTv);

        final TextView condTv = new TextView(host);
        condTv.setTextColor(0xCCFFFFFF);
        condTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        condTv.setSingleLine(true);
        condTv.setEllipsize(TextUtils.TruncateAt.END);
        tCol.addView(condTv);

        todayRow.addView(tCol, tclp);
        todayCol.addView(todayRow, trlp);
        mid.addView(todayCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Vertical divider
        final View weatherDiv = new View(host);
        weatherDiv.setBackgroundColor(0x24FFFFFF);
        LinearLayout.LayoutParams wdlp = new LinearLayout.LayoutParams(Math.max(1, dp(host, 1)), dp(host, 42));
        wdlp.leftMargin = dp(host, 6);
        wdlp.rightMargin = dp(host, 6);
        mid.addView(weatherDiv, wdlp);

        // Right column: Tomorrow
        final LinearLayout tomCol = new LinearLayout(host);
        tomCol.setOrientation(LinearLayout.VERTICAL);

        TextView tomLabelTv = new TextView(host);
        tomLabelTv.setText(I18n.t(host, "tomorrow_short"));
        tomLabelTv.setTextColor(0x99FFFFFF);
        tomLabelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        tomLabelTv.setSingleLine(true);
        tomCol.addView(tomLabelTv);

        LinearLayout tomRow = new LinearLayout(host);
        tomRow.setOrientation(LinearLayout.HORIZONTAL);
        tomRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tmrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tmrlp.topMargin = dp(host, 2);

        final Weather.WeatherIconView tomIconView = new Weather.WeatherIconView(host);
        tomRow.addView(tomIconView, new LinearLayout.LayoutParams(dp(host, 28), dp(host, 28)));

        LinearLayout tomTCol = new LinearLayout(host);
        tomTCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ttclp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ttclp.leftMargin = dp(host, 6);

        final TextView tomTempTv = new TextView(host);
        tomTempTv.setTextColor(Color.WHITE);
        tomTempTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        tomTempTv.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        tomTempTv.setSingleLine(true);
        tomTCol.addView(tomTempTv);

        final TextView tomCondTv = new TextView(host);
        tomCondTv.setTextColor(0xCCFFFFFF);
        tomCondTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
        tomCondTv.setSingleLine(true);
        tomCondTv.setEllipsize(TextUtils.TruncateAt.END);
        tomTCol.addView(tomCondTv);

        tomRow.addView(tomTCol, ttclp);
        tomCol.addView(tomRow, tmrlp);
        mid.addView(tomCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        col.addView(mid, mlp);

        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final Runnable bindWeather = new Runnable() {
            @Override
            public void run() {
                Weather.Data w = Weather.cached(host);
                if (w == null) {
                    tempTv.setText("--°");
                    condTv.setText(I18n.t(host, "weather_updating"));
                    cityTv.setText("");
                    weatherDiv.setVisibility(View.GONE);
                    tomCol.setVisibility(View.GONE);
                    return;
                }
                cityTv.setText(w.place != null ? w.place : "");
                iconView.setType(Weather.iconType(w.code));
                tempTv.setText(Weather.formatTemp(host, w.temp));
                condTv.setText(Weather.codeText(host, w.code));
                if (w.days != null && w.days.size() > 1 && w.days.get(1) != null) {
                    Weather.Day tm = w.days.get(1);
                    weatherDiv.setVisibility(View.VISIBLE);
                    tomCol.setVisibility(View.VISIBLE);
                    tomIconView.setType(Weather.iconType(tm.code));
                    tomTempTv.setText(Weather.formatTemp(host, tm.max));
                    tomCondTv.setText(Weather.codeText(host, tm.code) + " · " + Weather.formatTemp(host, tm.min));
                } else {
                    weatherDiv.setVisibility(View.GONE);
                    tomCol.setVisibility(View.GONE);
                }
            }
        };

        bindWeather.run();
        sCardRefreshers.put(card, bindWeather);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nav != null) nav.onOpenWeather();
                else host.showWeather();
            }
        });
        return card;
    }

    // -------------------------------------------------------------------------
    // WIDGET: SOURCES CARD (HDMI devices as quick picks)
    // -------------------------------------------------------------------------
    private static final int SOURCE_ON = 0xFF34C759;
    private static final int SOURCE_OFF = 0x59FFFFFF;

    /** A device picture with rounded corners, grey and dimmed while the device is off. */
    private static ImageView createSourcePicture(MainActivity host, int device, float radiusDp) {
        ImageView img = new ImageView(host);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageBitmap(Sources.picture(host, device));
        img.setOutlineProvider(host.roundedOutline(new float[]{dp(host, Math.round(radiusDp))}));
        img.setClipToOutline(true);
        img.setBackgroundColor(0x33000000);
        return img;
    }

    private static void bindSourcePicture(ImageView img, View dot, boolean on) {
        img.setColorFilter(on ? null : Sources.GRAYSCALE);
        img.setAlpha(on ? 1f : 0.45f);
        if (dot != null && dot.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) dot.getBackground()).setColor(on ? SOURCE_ON : SOURCE_OFF);
        }
    }

    private static View createSourceDot(MainActivity host, int sizeDp) {
        View dot = new View(host);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(SOURCE_OFF);
        dot.setBackground(d);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(host, sizeDp), dp(host, sizeDp)));
        return dot;
    }

    /**
     * Calls onChange while the view is attached whenever an input changes state (a console turned
     * on or off), through Android's TV input callback: no polling.
     */
    private static void followInputStates(final MainActivity host, View v, final Runnable onChange) {
        final android.media.tv.TvInputManager tm = Sources.manager(host);
        if (tm == null) return;
        final android.media.tv.TvInputManager.TvInputCallback cb = new android.media.tv.TvInputManager.TvInputCallback() {
            @Override
            public void onInputStateChanged(String inputId, int state) {
                onChange.run();
            }

            @Override
            public void onInputAdded(String inputId) {
                onChange.run();
            }

            @Override
            public void onInputRemoved(String inputId) {
                onChange.run();
            }
        };
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                try {
                    tm.registerCallback(cb, MAIN);
                } catch (Throwable ignored) {}
                onChange.run();
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                try {
                    tm.unregisterCallback(cb);
                } catch (Throwable ignored) {}
            }
        });
    }

    private static View createSourcesWidgetCard(final MainActivity host) {
        final FrameLayout card = createBaseGlassCard(host, SOURCE_ON);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, null, headerTitle, I18n.t(host, "dash_sources"));
        head.addView(headerTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView badgeTv = new TextView(host);
        badgeTv.setTextColor(0x88FFFFFF);
        badgeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        badgeTv.setSingleLine(true);
        head.addView(badgeTv);
        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout tiles = new LinearLayout(host);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        final int n = Sources.DEVICES.length;
        final ImageView[] pics = new ImageView[n];
        final View[] dots = new View[n];
        for (int i = 0; i < n; i++) {
            LinearLayout tile = new LinearLayout(host);
            tile.setOrientation(LinearLayout.VERTICAL);
            pics[i] = createSourcePicture(host, i, 7);
            tile.addView(pics[i], new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            LinearLayout name = new LinearLayout(host);
            name.setOrientation(LinearLayout.HORIZONTAL);
            name.setGravity(Gravity.CENTER_VERTICAL);
            dots[i] = createSourceDot(host, 5);
            name.addView(dots[i]);
            TextView label = new TextView(host);
            label.setText(Sources.DEVICES[i][0]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            label.setTypeface(Ui.font(host, "semibold"));
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.leftMargin = dp(host, 4);
            name.addView(label, llp);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = dp(host, 3);
            tile.addView(name, nlp);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) tlp.leftMargin = dp(host, 8);
            tiles.addView(tile, tlp);
        }
        LinearLayout.LayoutParams tilesLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        tilesLp.topMargin = dp(host, 5);
        col.addView(tiles, tilesLp);

        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final Runnable bind = new Runnable() {
            @Override
            public void run() {
                int on = 0;
                for (int i = 0; i < n; i++) {
                    boolean isOn = Sources.isOn(host, i);
                    if (isOn) on++;
                    bindSourcePicture(pics[i], dots[i], isOn);
                }
                badgeTv.setText(on == 0 ? I18n.t(host, "sources_all_off")
                        : on == 1 ? I18n.t(host, "sources_one_on")
                        : I18n.s(host, "%d on", "%d açık", on));
            }
        };
        bind.run();
        followInputStates(host, card, bind);
        sCardRefreshers.put(card, bind);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourcesDialog(host);
            }
        });
        return card;
    }

    /** The three devices in big tiles: OK switches the TV to that HDMI port. */
    private static void showSourcesDialog(final MainActivity host) {
        final Dialog d = createNativeGlassDialog(host);
        LinearLayout panel = createModalContainer(host, dp(host, 720));

        TextView title = new TextView(host);
        title.setText(I18n.t(host, "dash_sources"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        panel.addView(title);

        TextView sub = new TextView(host);
        sub.setText(I18n.t(host, "sources_hint"));
        sub.setTextColor(0x99FFFFFF);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(host, 2);
        panel.addView(sub, slp);

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(0, dp(host, 8), 0, dp(host, 8));
        final int n = Sources.DEVICES.length;
        final ImageView[] pics = new ImageView[n];
        final View[] dots = new View[n];
        final TextView[] states = new TextView[n];
        View firstOn = null;
        final View[] tiles = new View[n];
        for (int i = 0; i < n; i++) {
            final int device = i;
            final LinearLayout tile = new LinearLayout(host);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setFocusable(true);
            tile.setClickable(true);
            tile.setPadding(dp(host, 8), dp(host, 8), dp(host, 8), dp(host, 10));
            final GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(host, 16));
            bg.setColor(0x1AFFFFFF);
            bg.setStroke(dp(host, 1), 0x28FFFFFF);
            tile.setBackground(bg);

            pics[i] = createSourcePicture(host, i, 11);
            tile.addView(pics[i], new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 120)));

            LinearLayout name = new LinearLayout(host);
            name.setOrientation(LinearLayout.HORIZONTAL);
            name.setGravity(Gravity.CENTER_VERTICAL);
            dots[i] = createSourceDot(host, 8);
            name.addView(dots[i]);
            final TextView label = new TextView(host);
            label.setText(Sources.DEVICES[i][0]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.leftMargin = dp(host, 7);
            name.addView(label, llp);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = dp(host, 9);
            tile.addView(name, nlp);

            states[i] = new TextView(host);
            states[i].setTextColor(0x99FFFFFF);
            states[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            states[i].setSingleLine(true);
            LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            stlp.topMargin = dp(host, 2);
            tile.addView(states[i], stlp);

            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    bg.setColor(hasFocus ? 0x33FFFFFF : 0x1AFFFFFF);
                    bg.setStroke(dp(host, hasFocus ? 2 : 1), hasFocus ? 0xFFFFFFFF : 0x28FFFFFF);
                    v.animate().scaleX(hasFocus ? 1.04f : 1f).scaleY(hasFocus ? 1.04f : 1f)
                            .translationZ(hasFocus ? dp(host, 6) : 0f)
                            .setDuration(150).setInterpolator(MainActivity.EASE).start();
                }
            });
            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Sources.open(host, device)) {
                        Clicks.select(host);
                        d.dismiss();
                    } else {
                        host.toast(I18n.s(host, "%s isn't available on this TV", "%s bu TV'de yok",
                                Sources.portLabel(device)));
                    }
                }
            });
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) tlp.leftMargin = dp(host, 14);
            row.addView(tile, tlp);
            tiles[i] = tile;
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(host, 12);
        panel.addView(row, rlp);

        final Runnable bind = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < n; i++) {
                    boolean on = Sources.isOn(host, i);
                    bindSourcePicture(pics[i], dots[i], on);
                    states[i].setText(Sources.portLabel(i) + "  \u00b7  "
                            + I18n.t(host, on ? "source_on" : "source_off"));
                }
            }
        };
        bind.run();
        followInputStates(host, panel, bind);
        for (int i = 0; i < n; i++) {
            if (firstOn == null && Sources.isOn(host, i)) firstOn = tiles[i];
        }
        presentNativeGlassDialog(host, d, panel);
        (firstOn != null ? firstOn : tiles[0]).requestFocus();
    }

    // -------------------------------------------------------------------------
    // WIDGET 6: LIVE NEWS CARD
    // -------------------------------------------------------------------------
    public static final class NewsHeadline {
        public String title = "";
        public String source = "";
        public String pubDate = "";
    }

    private static List<NewsHeadline> sMemNews = new ArrayList<NewsHeadline>();
    private static long sNewsLastTs = 0L;
    private static String sNewsSig = "";
    private static int sNewsIdx = 0;

    public static boolean isNewsCountryAuto(Context c) {
        String saved = prefs(c).getString(KEY_NEWS_COUNTRY, "auto");
        return saved == null || saved.trim().isEmpty() || "auto".equalsIgnoreCase(saved.trim());
    }

    public static String getEffectiveNewsCountry(Context c) {
        if (isNewsCountryAuto(c)) {
            return defaultCountryForUiLang(c);
        }
        return prefs(c).getString(KEY_NEWS_COUNTRY, "US").trim().toUpperCase(Locale.US);
    }

    public static String getNewsCountryDisplayName(String code) {
        if (code == null) return "";
        for (int i = 0; i < NEWS_COUNTRIES.length; i++) {
            if (NEWS_COUNTRIES[i][0].equalsIgnoreCase(code)) {
                return NEWS_COUNTRIES[i][1];
            }
        }
        return code.toUpperCase(Locale.US);
    }

    public static void setNewsCountry(Context c, String countryCode) {
        String val = (countryCode == null || countryCode.trim().isEmpty() || "auto".equalsIgnoreCase(countryCode.trim()))
                ? "auto"
                : countryCode.trim().toUpperCase(Locale.US);
        prefs(c).edit().putString(KEY_NEWS_COUNTRY, val).apply();
        sMemNews = new ArrayList<NewsHeadline>();
        sNewsLastTs = 0L;
        sNewsSig = "";
        sNewsIdx = 0;
    }

    public static String getNewsCountrySummary(Context c) {
        if (isNewsCountryAuto(c)) {
            String cc = getEffectiveNewsCountry(c);
            return I18n.t(c, "news_country_auto") + " (" + cc + ")";
        }
        return getNewsCountryDisplayName(getEffectiveNewsCountry(c));
    }

    private static String buildNewsRssUrl(Context c) {
        String cc = getEffectiveNewsCountry(c);
        if (isNewsCountryAuto(c) && I18n.LANG_HI.equals(I18n.getLang(c))) {
            return "https://news.google.com/rss?hl=hi&gl=IN&ceid=IN:hi";
        }
        for (int i = 0; i < NEWS_COUNTRIES.length; i++) {
            if (NEWS_COUNTRIES[i][0].equalsIgnoreCase(cc)) {
                return "https://news.google.com/rss?hl=" + NEWS_COUNTRIES[i][2]
                        + "&gl=" + NEWS_COUNTRIES[i][0]
                        + "&ceid=" + NEWS_COUNTRIES[i][3];
            }
        }
        return "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en";
    }

    public static void fetchLiveNews(final Context c, final boolean force, final Runnable onDone) {
        final long now = System.currentTimeMillis();
        final String sig = I18n.getLang(c) + ":" + getEffectiveNewsCountry(c);
        if (!force && !sMemNews.isEmpty() && sig.equals(sNewsSig) && (now - sNewsLastTs < 15 * 60_000L)) {
            if (onDone != null) onDone.run();
            return;
        }
        EXEC.execute(() -> {
            List<NewsHeadline> list = new ArrayList<NewsHeadline>();
            try {
                String rssUrl = buildNewsRssUrl(c);
                String xml = httpGet(rssUrl);
                int pos = 0;
                while (list.size() < 15) {
                    int itemStart = xml.indexOf("<item>", pos);
                    if (itemStart < 0) break;
                    int itemEnd = xml.indexOf("</item>", itemStart);
                    if (itemEnd < 0) break;
                    String block = xml.substring(itemStart, itemEnd);
                    pos = itemEnd + 7;
                    String rawTitle = extractXmlTag(block, "title");
                    String source = extractXmlTag(block, "source");
                    if (rawTitle != null && !rawTitle.isEmpty()) {
                        int dashIdx = rawTitle.lastIndexOf(" - ");
                        if (dashIdx > 10) {
                            if (source == null || source.isEmpty()) {
                                source = rawTitle.substring(dashIdx + 3).trim();
                            }
                            rawTitle = rawTitle.substring(0, dashIdx).trim();
                        }
                        NewsHeadline nh = new NewsHeadline();
                        nh.title = decodeXmlEntities(rawTitle);
                        nh.source = source != null && !source.isEmpty() ? decodeXmlEntities(source) : "LIVE";
                        list.add(nh);
                    }
                }
            } catch (Throwable ignored) {}
            if (!list.isEmpty()) {
                sMemNews = list;
                sNewsLastTs = System.currentTimeMillis();
                sNewsSig = sig;
                sNewsIdx = 0;
            }
            if (onDone != null) {
                MAIN.post(onDone);
            }
        });
    }

    private static String extractXmlTag(String xml, String tag) {
        int s = xml.indexOf("<" + tag);
        if (s < 0) return "";
        int gt = xml.indexOf('>', s);
        if (gt < 0) return "";
        int e = xml.indexOf("</" + tag + ">", gt);
        if (e < 0) return "";
        String inner = xml.substring(gt + 1, e).trim();
        if (inner.startsWith("<![CDATA[") && inner.endsWith("]]>")) {
            inner = inner.substring(9, inner.length() - 3).trim();
        }
        return inner;
    }

    private static String decodeXmlEntities(String s) {
        if (s == null) return "";
        return s.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private static View createNewsWidgetCard(final MainActivity host) {
        final FrameLayout card = createBaseGlassCard(host, 0xFFF87171);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(host);
        head.addView(dot, new LinearLayout.LayoutParams(dp(host, 6), dp(host, 6)));

        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, dot, headerTitle, I18n.t(host, "dash_news"));
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = 0;
        head.addView(headerTitle, htlp);

        final TextView srcBadge = new TextView(host);
        srcBadge.setTextColor(0x88FFFFFF);
        srcBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        srcBadge.setSingleLine(true);
        head.addView(srcBadge);

        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView headlineTv = new TextView(host);
        headlineTv.setTextColor(Color.WHITE);
        headlineTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        headlineTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        headlineTv.setMaxLines(2);
        headlineTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams hllp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hllp.topMargin = dp(host, 4);
        col.addView(headlineTv, hllp);

        final View newsDivider = new View(host);
        newsDivider.setBackgroundColor(0x26FFFFFF);
        LinearLayout.LayoutParams ndlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(host, 1)));
        ndlp.topMargin = dp(host, 4);
        ndlp.bottomMargin = dp(host, 3);
        col.addView(newsDivider, ndlp);

        final TextView subTv = new TextView(host);
        subTv.setTextColor(0xAAFFFFFF);
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        subTv.setSingleLine(true);
        subTv.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(subTv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        card.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final Runnable bindNews = new Runnable() {
            @Override
            public void run() {
                if (sMemNews.isEmpty()) {
                    headlineTv.setText(I18n.s(host, "Loading top headlines…", "Gündem haberleri yükleniyor…"));
                    srcBadge.setText("● LIVE");
                    newsDivider.setVisibility(View.GONE);
                    subTv.setText("");
                    return;
                }
                if (sNewsIdx >= sMemNews.size()) sNewsIdx = 0;
                NewsHeadline nh = sMemNews.get(sNewsIdx);
                srcBadge.setText(nh.source);
                headlineTv.setText(nh.title);
                if (sMemNews.size() > 1) {
                    NewsHeadline next = sMemNews.get((sNewsIdx + 1) % sMemNews.size());
                    newsDivider.setVisibility(View.VISIBLE);
                    subTv.setText(I18n.s(host, "Next: ", "Sonraki: ") + next.title);
                } else {
                    newsDivider.setVisibility(View.GONE);
                    subTv.setText("");
                }
            }
        };

        bindNews.run();
        fetchLiveNews(host, false, bindNews);
        sCardRefreshers.put(card, () -> fetchLiveNews(host, false, bindNews));
        card.setOnClickListener(v -> showNewsDialog(host, () -> {
            sNewsIdx++;
            bindNews.run();
        }));
        return card;
    }

    // -------------------------------------------------------------------------
    // WIDGET 7: TOP MOVIES / WEB SERIES CARD
    // -------------------------------------------------------------------------
    private static List<Suggestions.Item> sMemTopMedia = new ArrayList<Suggestions.Item>();
    private static long sTopMediaLastTs = 0L;
    private static int sTopMediaIdx = 0;

    public static void fetchTopMedia(final Context c, final boolean force, final Runnable onDone) {
        long now = System.currentTimeMillis();
        if (!force && !sMemTopMedia.isEmpty() && (now - sTopMediaLastTs < 60 * 60_000L)) {
            if (onDone != null) onDone.run();
            return;
        }
        EXEC.execute(() -> {
            List<Suggestions.Item> out = new ArrayList<Suggestions.Item>();
            try {
                String[] urls = {
                    "https://v3-cinemeta.strem.io/catalog/series/top.json",
                    "https://v3-cinemeta.strem.io/catalog/movie/top.json"
                };
                for (int uIdx = 0; uIdx < urls.length; uIdx++) {
                    try {
                        String raw = httpGet(urls[uIdx]);
                        JSONObject root = new JSONObject(raw);
                        JSONArray metas = root.optJSONArray("metas");
                        if (metas == null) continue;
                        for (int i = 0; i < Math.min(12, metas.length()); i++) {
                            JSONObject m = metas.optJSONObject(i);
                            if (m == null) continue;
                            Suggestions.Item it = new Suggestions.Item();
                            it.key = m.optString("id", "");
                            it.title = m.optString("name", "");
                            it.type = uIdx == 0 ? "series" : "movie";
                            it.year = m.optString("releaseInfo", "");
                            it.backdrop = m.optString("poster", m.optString("background", ""));
                            try {
                                it.rating = Float.parseFloat(m.optString("imdbRating", "0"));
                            } catch (Throwable ignored) {}
                            if (it.title != null && !it.title.isEmpty()) {
                                out.add(it);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            if (!out.isEmpty()) {
                sMemTopMedia = out;
                sTopMediaLastTs = System.currentTimeMillis();
            }
            if (onDone != null) {
                MAIN.post(onDone);
            }
        });
    }

    private static View createTopMediaWidgetCard(final MainActivity host) {
        final FrameLayout card = createBaseGlassCard(host, 0xFFF59E0B);

        LinearLayout rootRow = new LinearLayout(host);
        rootRow.setOrientation(LinearLayout.HORIZONTAL);
        rootRow.setGravity(Gravity.CENTER_VERTICAL);

        final ImageView posterView = new ImageView(host);
        posterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable posterBg = new GradientDrawable();
        posterBg.setColor(0x33FFFFFF);
        posterBg.setCornerRadius(dp(host, 8));
        posterView.setBackground(posterBg);
        posterView.setOutlineProvider(host.roundedOutline(new float[]{dp(host, 8)}));
        posterView.setClipToOutline(true);
        rootRow.addView(posterView, new LinearLayout.LayoutParams(dp(host, 50), dp(host, 74)));

        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colp.leftMargin = dp(host, 10);

        LinearLayout head = new LinearLayout(host);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(host);
        head.addView(dot, new LinearLayout.LayoutParams(dp(host, 6), dp(host, 6)));

        TextView headerTitle = new TextView(host);
        styleUnifiedWidgetHeader(host, dot, headerTitle, I18n.t(host, "dash_top_media"));
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = 0;
        head.addView(headerTitle, htlp);

        col.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView titleTv = new TextView(host);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        titleTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleTv.setMaxLines(2);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttlp.topMargin = dp(host, 3);
        col.addView(titleTv, ttlp);

        final TextView metaTv = new TextView(host);
        metaTv.setTextColor(0xB8FFFFFF);
        metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.8f);
        metaTv.setSingleLine(true);
        metaTv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mtlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mtlp.topMargin = dp(host, 2);
        col.addView(metaTv, mtlp);

        rootRow.addView(col, colp);
        card.addView(rootRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final Runnable bindTop = new Runnable() {
            @Override
            public void run() {
                if (sMemTopMedia.isEmpty()) {
                    titleTv.setText(I18n.s(host, "Loading IMDb Top…", "IMDb En İyiler yükleniyor…"));
                    metaTv.setText("");
                    return;
                }
                if (sTopMediaIdx >= sMemTopMedia.size()) sTopMediaIdx = 0;
                final Suggestions.Item it = sMemTopMedia.get(sTopMediaIdx);
                titleTv.setText(it.title != null ? it.title : "");
                String badge = "series".equalsIgnoreCase(it.type)
                        ? I18n.s(host, "Web Series", "Dizi")
                        : I18n.s(host, "Movie", "Film");
                StringBuilder sb = new StringBuilder(badge);
                if (it.year != null && !it.year.isEmpty()) sb.append(" • ").append(it.year);
                if (it.rating > 0) sb.append(String.format(Locale.US, " • ★ %.1f", it.rating));
                metaTv.setText(sb.toString());

                final String imgUrl = it.backdrop;
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    if (imgUrl.equals(posterView.getTag())) {
                        return;
                    }
                    final String cacheKey = "poster:" + imgUrl;
                    Bitmap cachedBmp = sLogoCache.get(cacheKey);
                    if (cachedBmp != null && !cachedBmp.isRecycled()) {
                        posterView.setTag(imgUrl);
                        posterView.setImageBitmap(cachedBmp);
                    } else {
                        posterView.setTag(imgUrl);
                        EXEC.execute(() -> {
                            final Bitmap bmp = Suggestions.image(host, imgUrl, "w342", dp(host, 140));
                            if (bmp != null) {
                                sLogoCache.put(cacheKey, bmp);
                                MAIN.post(() -> {
                                    if (imgUrl.equals(posterView.getTag())) {
                                        posterView.setImageBitmap(bmp);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        };

        bindTop.run();
        fetchTopMedia(host, false, bindTop);
        sCardRefreshers.put(card, () -> fetchTopMedia(host, false, bindTop));
        card.setOnClickListener(v -> showTopMediaDialog(host, () -> {
            sTopMediaIdx++;
            bindTop.run();
        }));
        return card;
    }

    // =========================================================================
    // 4. INTERACTIVE DETAIL DIALOGS FOR WIDGETS
    // =========================================================================

    private static void showFootballDialog(final MainActivity host, final Runnable onUpdated, final boolean favOnlyFilter) {
        final Dialog d = createNativeGlassDialog(host);

        LinearLayout panel = createModalContainer(host, dp(host, 680));

        LinearLayout headerRow = new LinearLayout(host);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(host);
        title.setText(I18n.t(host, "dash_football"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        headerRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView refreshPill = createActionPill(host, "↻ " + I18n.s(host, "Refresh", "Yenile"));
        refreshPill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFootballMatches(host, true, new FootballCallback() {
                    @Override
                    public void onResult(List<MatchItem> matches) {
                        if (onUpdated != null) onUpdated.run();
                        d.dismiss();
                        showFootballDialog(host, onUpdated, favOnlyFilter);
                    }
                });
            }
        });
        headerRow.addView(refreshPill);
        panel.addView(headerRow);

        // Top actions row: [All / ★ Favorites filter] + [Competition picker] + [★ Favorite Teams]
        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(host, 10);

        final TextView filterBtn = createActionPill(host, favOnlyFilter
                ? I18n.s(host, "Filter: ★ Favorites", "Filtre: ★ Favoriler")
                : I18n.s(host, "Filter: All Matches", "Filtre: Tüm Maçlar"));
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                showFootballDialog(host, onUpdated, !favOnlyFilter);
            }
        });
        actions.addView(filterBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String curLeague = getSelectedLeague(host);
        String curLeagueLabel = LEAGUES[0][1];
        for (String[] lg : LEAGUES) {
            if (lg[0].equals(curLeague)) {
                curLeagueLabel = lg[1];
                break;
            }
        }
        final TextView leagueBtn = createActionPill(host, curLeagueLabel);
        LinearLayout.LayoutParams lgLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        lgLp.leftMargin = dp(host, 8);
        leagueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cLg = getSelectedLeague(host);
                int ni = 0;
                for (int i = 0; i < LEAGUES.length; i++) {
                    if (LEAGUES[i][0].equals(cLg)) {
                        ni = (i + 1) % LEAGUES.length;
                        break;
                    }
                }
                setSelectedLeague(host, LEAGUES[ni][0]);
                fetchFootballMatches(host, true, new FootballCallback() {
                    @Override
                    public void onResult(List<MatchItem> matches) {
                        if (onUpdated != null) onUpdated.run();
                        d.dismiss();
                        showFootballDialog(host, onUpdated, favOnlyFilter);
                    }
                });
            }
        });
        actions.addView(leagueBtn, lgLp);

        TextView favBtn = createActionPill(host, "★ " + I18n.t(host, "football_fav_teams"));
        LinearLayout.LayoutParams fblp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fblp.leftMargin = dp(host, 8);
        favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                showFavoriteTeamsDialog(host, onUpdated);
            }
        });
        actions.addView(favBtn, fblp);
        panel.addView(actions, alp);

        ScrollView sv = new ScrollView(host);
        LinearLayout listCol = new LinearLayout(host);
        listCol.setOrientation(LinearLayout.VERTICAL);

        List<MatchItem> allMatches = getCachedMatches(host);
        List<MatchItem> matches = new ArrayList<MatchItem>();
        for (MatchItem m : allMatches) {
            if (!favOnlyFilter || m.isFav) {
                matches.add(m);
            }
        }

        if (matches.isEmpty()) {
            TextView empty = new TextView(host);
            empty.setText(favOnlyFilter
                    ? I18n.s(host, "No upcoming or live matches found for your favorite teams.",
                            "Favori takımlarınız için yaklaşan veya canlı maç bulunamadı.")
                    : I18n.t(host, "football_no_matches"));
            empty.setTextColor(0x99FFFFFF);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            empty.setPadding(0, dp(host, 20), 0, dp(host, 20));
            listCol.addView(empty);
        } else {
            int logoPx = dp(host, 22);
            for (int i = 0; i < Math.min(45, matches.size()); i++) {
                final MatchItem m = matches.get(i);
                final LinearLayout row = new LinearLayout(host);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(host, 14), dp(host, 10), dp(host, 14), dp(host, 10));
                final GradientDrawable rbg = new GradientDrawable();
                rbg.setCornerRadius(dp(host, 10));
                final int baseColor = m.isFav ? 0x28FDE047 : 0x16FFFFFF;
                rbg.setColor(baseColor);
                rbg.setStroke(dp(host, 1), m.isFav ? 0x55FDE047 : 0x22FFFFFF);
                row.setBackground(rbg);
                row.setFocusable(true);
                row.setClickable(true);
                row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        rbg.setColor(hasFocus ? 0x3DFFFFFF : baseColor);
                        rbg.setStroke(dp(host, hasFocus ? 2 : 1), hasFocus ? 0xEEFFFFFF : (m.isFav ? 0x55FDE047 : 0x22FFFFFF));
                    }
                });
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.topMargin = dp(host, 6);

                if (m.leagueName != null && !m.leagueName.isEmpty()) {
                    TextView lgBadge = new TextView(host);
                    lgBadge.setText(m.leagueName);
                    lgBadge.setTextColor(0x99FFFFFF);
                    lgBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                    lgBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    LinearLayout.LayoutParams lglp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lglp.rightMargin = dp(host, 8);
                    row.addView(lgBadge, lglp);
                }

                if (m.homeLogo != null && !m.homeLogo.isEmpty()) {
                    ImageView hLogo = new ImageView(host);
                    hLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(logoPx, logoPx);
                    llp.rightMargin = dp(host, 6);
                    row.addView(hLogo, llp);
                    loadLogoAsync(m.homeLogo, logoPx, hLogo);
                }

                TextView tms = new TextView(host);
                tms.setText((m.isFav ? "★ " : "") + m.homeTeam + "  vs  " + m.awayTeam);
                tms.setTextColor(m.isFav ? 0xFFFDE047 : Color.WHITE);
                tms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                tms.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                tms.setSingleLine(true);
                tms.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(tms, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (m.awayLogo != null && !m.awayLogo.isEmpty()) {
                    ImageView aLogo = new ImageView(host);
                    aLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    LinearLayout.LayoutParams allp = new LinearLayout.LayoutParams(logoPx, logoPx);
                    allp.leftMargin = dp(host, 6);
                    allp.rightMargin = dp(host, 8);
                    row.addView(aLogo, allp);
                    loadLogoAsync(m.awayLogo, logoPx, aLogo);
                }

                TextView st = new TextView(host);
                String scoreStr = (m.isLive || m.isFinished)
                        ? (m.homeScore + " - " + m.awayScore + "  (" + m.statusText + ")")
                        : m.statusText;
                st.setText(scoreStr);
                st.setTextColor(m.isLive ? 0xFF4ADE80 : 0xCCFFFFFF);
                st.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                st.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                row.addView(st);

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFavoriteTeam(host, m.homeTeam);
                        fetchFootballMatches(host, true, new FootballCallback() {
                            @Override
                            public void onResult(List<MatchItem> matches) {
                                if (onUpdated != null) onUpdated.run();
                                d.dismiss();
                                showFootballDialog(host, onUpdated, favOnlyFilter);
                            }
                        });
                    }
                });
                listCol.addView(row, rlp);
            }
        }

        sv.addView(listCol);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 300));
        svlp.topMargin = dp(host, 12);
        panel.addView(sv, svlp);

        presentNativeGlassDialog(host, d, panel);
        filterBtn.requestFocus();
    }

    private static void showFavoriteTeamsDialog(final MainActivity host, final Runnable onUpdated) {
        final Dialog d = createNativeGlassDialog(host);

        LinearLayout panel = createModalContainer(host, dp(host, 560));

        TextView title = new TextView(host);
        title.setText("★ " + I18n.t(host, "football_fav_teams"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        panel.addView(title);

        // Custom team input row
        LinearLayout customRow = new LinearLayout(host);
        customRow.setOrientation(LinearLayout.HORIZONTAL);
        customRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams crlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        crlp.topMargin = dp(host, 10);

        final EditText input = new EditText(host);
        input.setHint(I18n.s(host, "Enter any club or national team…", "Herhangi bir kulüp veya milli takım yazın…"));
        input.setHintTextColor(0x77FFFFFF);
        input.setTextColor(Color.WHITE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        input.setSingleLine(true);
        input.setPadding(dp(host, 12), dp(host, 9), dp(host, 12), dp(host, 9));
        GradientDrawable inBg = new GradientDrawable();
        inBg.setCornerRadius(dp(host, 10));
        inBg.setColor(0x22FFFFFF);
        inBg.setStroke(dp(host, 1), 0x44FFFFFF);
        input.setBackground(inBg);
        customRow.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView addBtn = createActionPill(host, "+ " + I18n.s(host, "Add", "Ekle"));
        LinearLayout.LayoutParams ablp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ablp.leftMargin = dp(host, 8);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = input.getText() != null ? input.getText().toString().trim() : "";
                if (!t.isEmpty()) {
                    addCustomFavoriteTeam(host, t);
                    fetchFootballMatches(host, true, new FootballCallback() {
                        @Override
                        public void onResult(List<MatchItem> matches) {
                            if (onUpdated != null) onUpdated.run();
                        }
                    });
                    d.dismiss();
                    showFavoriteTeamsDialog(host, onUpdated);
                }
            }
        });
        customRow.addView(addBtn, ablp);
        panel.addView(customRow, crlp);

        ScrollView sv = new ScrollView(host);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        final Set<String> favs = getFavoriteTeams(host);
        List<String> allTeams = new ArrayList<String>();
        for (String f : favs) {
            if (!allTeams.contains(f)) allTeams.add(f);
        }
        for (String p : POPULAR_TEAMS) {
            if (!allTeams.contains(p)) allTeams.add(p);
        }

        for (final String team : allTeams) {
            final boolean isFav = favs.contains(team);
            final TextView row = createActionPill(host, (isFav ? "★  " : "☆  ") + team);
            row.setTextColor(isFav ? 0xFFFDE047 : Color.WHITE);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(host, 6);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFavoriteTeam(host, team);
                    boolean nowFav = getFavoriteTeams(host).contains(team);
                    row.setText((nowFav ? "★  " : "☆  ") + team);
                    row.setTextColor(nowFav ? 0xFFFDE047 : Color.WHITE);
                    fetchFootballMatches(host, true, new FootballCallback() {
                        @Override
                        public void onResult(List<MatchItem> matches) {
                            if (onUpdated != null) onUpdated.run();
                        }
                    });
                }
            });
            col.addView(row, rlp);
        }

        sv.addView(col);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 270));
        svlp.topMargin = dp(host, 12);
        panel.addView(sv, svlp);

        presentNativeGlassDialog(host, d, panel);
    }

    private static void showNewsDialog(final MainActivity host, final Runnable onCycle) {
        final Dialog d = createNativeGlassDialog(host);

        LinearLayout panel = createModalContainer(host, dp(host, 620));
        TextView title = new TextView(host);
        title.setText(I18n.s(host, "Live News Headlines", "Canlı Haber Başlıkları"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        panel.addView(title);

        ScrollView sv = new ScrollView(host);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < sMemNews.size(); i++) {
            final int idx = i;
            NewsHeadline nh = sMemNews.get(i);
            TextView row = createActionPill(host, "[" + nh.source + "]  " + nh.title);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(host, 6);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sNewsIdx = idx;
                    if (onCycle != null) onCycle.run();
                    d.dismiss();
                }
            });
            col.addView(row, rlp);
        }

        sv.addView(col);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 310));
        svlp.topMargin = dp(host, 12);
        panel.addView(sv, svlp);

        presentNativeGlassDialog(host, d, panel);
    }

    private static void showTopMediaDialog(final MainActivity host, final Runnable onCycle) {
        final Dialog d = createNativeGlassDialog(host);

        LinearLayout panel = createModalContainer(host, dp(host, 580));
        TextView title = new TextView(host);
        title.setText(I18n.s(host, "Top Movies & Web Series", "En İyi Filmler ve Diziler"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        panel.addView(title);

        ScrollView sv = new ScrollView(host);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < sMemTopMedia.size(); i++) {
            final int idx = i;
            final Suggestions.Item it = sMemTopMedia.get(i);
            String badge = "series".equalsIgnoreCase(it.type)
                    ? I18n.s(host, "Series", "Dizi")
                    : I18n.s(host, "Movie", "Film");
            String label = "[" + badge + "]  " + it.title
                    + (it.year != null && !it.year.isEmpty() ? (" (" + it.year + ")") : "")
                    + (it.rating > 0 ? String.format(Locale.US, "   ★ %.1f", it.rating) : "");
            TextView row = createActionPill(host, label);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(host, 6);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sTopMediaIdx = idx;
                    if (onCycle != null) onCycle.run();
                    d.dismiss();
                }
            });
            col.addView(row, rlp);
        }

        sv.addView(col);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 310));
        svlp.topMargin = dp(host, 12);
        panel.addView(sv, svlp);

        presentNativeGlassDialog(host, d, panel);
    }

    private static void showReleasesDialog(final MainActivity host, final List<Suggestions.Item> items, final Runnable onCycle) {
        final Dialog d = createNativeGlassDialog(host);

        LinearLayout panel = createModalContainer(host, dp(host, 560));

        TextView title = new TextView(host);
        title.setText(I18n.t(host, "dash_latest_releases"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        panel.addView(title);

        ScrollView sv = new ScrollView(host);
        LinearLayout col = new LinearLayout(host);
        col.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < Math.min(20, items.size()); i++) {
            final int idx = i;
            final Suggestions.Item it = items.get(i);
            String label = it.title + (it.year != null && !it.year.isEmpty() ? (" (" + it.year + ")") : "")
                    + (it.rating > 0 ? String.format(Locale.US, "  ★ %.1f", it.rating) : "");
            TextView row = createActionPill(host, label);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(host, 6);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sReleaseIdx = idx;
                    if (onCycle != null) onCycle.run();
                    d.dismiss();
                }
            });
            col.addView(row, rlp);
        }

        sv.addView(col);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(host, 310));
        svlp.topMargin = dp(host, 12);
        panel.addView(sv, svlp);

        presentNativeGlassDialog(host, d, panel);
    }

    private static LinearLayout createModalContainer(MainActivity host, int widthPx) {
        LinearLayout panel = new LinearLayout(host);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(host, 24), dp(host, 22), dp(host, 24), dp(host, 22));
        panel.setClickable(true);
        MainActivity.LiquidGlass glass = new MainActivity.LiquidGlass(
                dp(host, 24), 0xB8181A22, host.glassSource, host.screenW, host.screenH, host.unit);
        glass.lens = dp(host, 10);
        glass.rimStrength = 0.45f;
        panel.setBackground(glass);
        panel.setOutlineProvider(host.roundedOutline(new float[]{dp(host, 24)}));
        panel.setClipToOutline(true);
        panel.setElevation(dp(host, 28));
        panel.setTag(widthPx);
        panel.setLayoutParams(new FrameLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        return panel;
    }

    private static Dialog sActiveDialog;

    public static void dismissActiveDialog() {
        Dialog d = sActiveDialog;
        sActiveDialog = null;
        if (d != null) {
            try {
                if (d.isShowing()) d.dismiss();
            } catch (Throwable ignored) {}
        }
    }

    private static Dialog createNativeGlassDialog(MainActivity host) {
        Dialog d = new Dialog(host, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return d;
    }

    private static void presentNativeGlassDialog(final MainActivity host, final Dialog d, final LinearLayout panel) {
        dismissActiveDialog();
        sActiveDialog = d;
        d.setOnDismissListener(dialog -> {
            if (sActiveDialog == dialog) sActiveDialog = null;
        });
        d.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_HOME) {
                if (event.getAction() == KeyEvent.ACTION_UP) dialog.dismiss();
                return true;
            }
            return false;
        });

        FrameLayout layer = new FrameLayout(host);
        layer.setClickable(true);
        host.addBlurredBackdrop(layer, 0x66000000);
        layer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        int widthPx = (panel.getTag() instanceof Integer) ? (Integer) panel.getTag() : dp(host, 560);
        int maxW = Math.min(widthPx, Math.max(dp(host, 360), host.screenW - dp(host, 64)));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(maxW, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        plp.topMargin = dp(host, 24);
        plp.bottomMargin = dp(host, 24);
        layer.addView(panel, plp);

        d.setContentView(layer);
        d.show();

        layer.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        layer.animate().alpha(1f).setDuration(180).start();
        panel.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(MainActivity.LIFT).start();
    }

    private static TextView createActionPill(final Context c, String text) {
        final TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextColor(0xFFF4F4F6);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        tv.setFocusable(true);
        tv.setClickable(true);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(c, 13));
        bg.setColor(0x1AFFFFFF);
        bg.setStroke(dp(c, 1), 0x28FFFFFF);
        tv.setBackground(bg);
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                bg.setColor(hasFocus ? 0xF2FFFFFF : 0x1AFFFFFF);
                bg.setStroke(dp(c, 1), hasFocus ? 0xFFFFFFFF : 0x28FFFFFF);
                tv.setTextColor(hasFocus ? 0xFF111114 : 0xFFF4F4F6);
                v.animate().scaleX(hasFocus ? 1.02f : 1f).scaleY(hasFocus ? 1.02f : 1f).setDuration(150).start();
            }
        });
        return tv;
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(urlStr);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "tvOSLauncher/2.2 (Android TV)");
            conn.setRequestProperty("Accept", "application/json");
            InputStream in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
