package com.chapitres.podcast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

/** Cache mémoire des pochettes, décodées à la bonne taille. */
public class ArtCache {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private ArtCache() {
    }

    public static Bitmap get(String key, byte[] data, int maxSize) {
        if (data == null || data.length == 0 || key == null) return null;
        Bitmap cached = CACHE.get(key);
        if (cached != null) return cached;
        Bitmap bmp = decode(data, maxSize);
        if (bmp != null) CACHE.put(key, bmp);
        return bmp;
    }

    public static void clear() {
        CACHE.evictAll();
    }

    private static Bitmap decode(byte[] data, int maxSize) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            int sample = 1;
            int max = Math.max(opts.outWidth, opts.outHeight);
            while (max / sample > maxSize && sample < 32) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o2);
        } catch (Throwable t) {
            return null;
        }
    }
}
