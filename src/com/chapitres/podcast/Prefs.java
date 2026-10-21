package com.chapitres.podcast;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Préférences persistantes : positions de reprise, vitesse, dossiers ajoutés. */
public class Prefs {

    private static final String NAME = "chapitres";
    private static final String K_POS = "pos:";
    private static final String K_DONE = "done:";
    private static final String K_LAST = "last";
    private static final String K_SPEED = "speed";
    private static final String K_TREES = "trees";

    private static SharedPreferences sp;

    public static void init(Context ctx) {
        if (sp == null) {
            sp = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    private static SharedPreferences sp() {
        return sp;
    }

    public static long getPosition(String key) {
        if (sp == null || key == null) return 0;
        return sp.getLong(K_POS + key, 0);
    }

    public static void setPosition(String key, long ms) {
        if (sp == null || key == null) return;
        if (ms <= 1500) {
            sp.edit().remove(K_POS + key).commit();
        } else {
            sp.edit().putLong(K_POS + key, ms).commit();
        }
    }

    public static boolean isDone(String key) {
        if (sp == null || key == null) return false;
        return sp.getBoolean(K_DONE + key, false);
    }

    public static void setDone(String key, boolean done) {
        if (sp == null || key == null) return;
        sp.edit().putBoolean(K_DONE + key, done).apply();
        if (done) setPosition(key, 0);
    }

    public static void clearAllPositions() {
        if (sp == null) return;
        SharedPreferences.Editor e = sp.edit();
        for (String k : sp.getAll().keySet()) {
            if (k.startsWith(K_POS) || k.startsWith(K_DONE)) e.remove(k);
        }
        e.apply();
    }

    public static String getLastKey() {
        return sp == null ? null : sp.getString(K_LAST, null);
    }

    public static void setLastKey(String key) {
        if (sp == null) return;
        sp.edit().putString(K_LAST, key).commit();
    }

    public static float getSpeed() {
        return sp == null ? 1f : sp.getFloat(K_SPEED, 1f);
    }

    public static void setSpeed(float s) {
        if (sp == null) return;
        sp.edit().putFloat(K_SPEED, s).apply();
    }

    private static final String K_HINT = "hint_seen";

    public static boolean hasSeenHint() {
        return sp != null && sp.getBoolean(K_HINT, false);
    }

    public static void setSeenHint() {
        if (sp == null) return;
        sp.edit().putBoolean(K_HINT, true).apply();
    }

    public static Set<String> getTrees() {
        if (sp == null) return new HashSet<>();
        return sp.getStringSet(K_TREES, new HashSet<String>());
    }

    public static void addTree(String uri) {
        if (sp == null) return;
        Set<String> set = new HashSet<>(getTrees());
        set.add(uri);
        sp.edit().putStringSet(K_TREES, set).apply();
    }

    public static void removeTree(String uri) {
        if (sp == null) return;
        Set<String> set = new HashSet<>(getTrees());
        set.remove(uri);
        sp.edit().putStringSet(K_TREES, set).apply();
    }
}
