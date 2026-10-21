package com.chapitres.podcast;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Analyse de la carte SD : dossier "Podcasts" (accès direct), MediaStore et
 * arborescences ajoutées par l'utilisateur (SAF).
 */
public class Library {

    public interface Callback {
        void onProgress(String message);

        void onResult(List<Podcast> podcasts);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "library-scan");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    });

    private static volatile List<Podcast> cache = new ArrayList<>();

    private Library() {
    }

    public static List<Podcast> cached() {
        return cache;
    }

    public static Podcast byId(String id) {
        for (Podcast p : cache) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public static AudioItem findByKey(String key) {
        if (key == null) return null;
        for (Podcast p : cache) {
            for (AudioItem a : p.episodes) {
                if (key.equals(a.key)) return a;
            }
        }
        return null;
    }

    public static void scan(final Context ctx, final Callback cb) {
        final Context app = ctx.getApplicationContext();
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                List<Podcast> result;
                try {
                    result = doScan(app, cb);
                } catch (Throwable t) {
                    result = new ArrayList<>();
                }
                cache = result;
                cb.onResult(result);
            }
        });
    }

    // ---------------------------------------------------------------- scan

    private static class Entry {
        String absPath;
        Uri uri;
        String groupKey;
        String groupTitle;
        String fileName;
        long size;
    }

    private static List<Podcast> doScan(Context ctx, Callback cb) {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        cb.onProgress("Recherche des dossiers…");
        for (File root : defaultRoots()) {
            List<File> files = new ArrayList<>();
            collect(root, files, 0, 4);
            for (File f : files) {
                String key = f.getAbsolutePath();
                if (!seen.add(key)) continue;
                Entry e = new Entry();
                e.absPath = key;
                e.groupKey = f.getParent();
                e.groupTitle = f.getParentFile() == null ? "Podcast" : f.getParentFile().getName();
                e.fileName = f.getName();
                e.size = f.length();
                entries.add(e);
            }
        }

        scanMediaStore(ctx, entries, seen);

        Set<String> trees = Prefs.getTrees();
        for (String tree : trees) {
            try {
                scanTree(ctx, Uri.parse(tree), entries, seen, 0);
            } catch (Throwable ignored) {
            }
        }

        // Regroupement par dossier
        Map<String, Podcast> groups = new LinkedHashMap<>();
        for (Entry e : entries) {
            Podcast p = groups.get(e.groupKey);
            if (p == null) {
                p = new Podcast();
                p.id = e.groupKey;
                p.title = Names.cleanFolderName(e.groupTitle);
                groups.put(e.groupKey, p);
            }
            AudioItem item = new AudioItem();
            item.key = e.absPath != null ? e.absPath : String.valueOf(e.uri);
            item.path = e.absPath;
            item.uri = e.uri;
            item.fileName = e.fileName;
            item.title = Names.titleFromFileName(e.fileName);
            item.index = Names.indexFromFileName(e.fileName);
            item.sizeBytes = e.size;
            item.podcast = p;
            p.episodes.add(item);
        }

        List<Podcast> podcasts = new ArrayList<>(groups.values());
        for (Podcast p : podcasts) {
            cb.onProgress("Lecture des métadonnées : " + p.title);
            Collections.sort(p.episodes, new java.util.Comparator<AudioItem>() {
                @Override
                public int compare(AudioItem a, AudioItem b) {
                    return Names.NATURAL.compare(a.fileName, b.fileName);
                }
            });
            for (AudioItem it : p.episodes) {
                it.durationMs = durationOf(ctx, it);
                p.totalDurationMs += it.durationMs;
            }
            if (!p.episodes.isEmpty()) {
                AudioItem first = p.episodes.get(0);
                readTags(ctx, first);
                for (AudioItem it : p.episodes) {
                    it.album = first.album;
                    it.artist = first.artist;
                }
                p.artist = first.artist;
                p.art = first.art;
                first.art = first.art;
            }
        }

        Collections.sort(podcasts, new java.util.Comparator<Podcast>() {
            @Override
            public int compare(Podcast a, Podcast b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });
        return podcasts;
    }

    private static File[] defaultRoots() {
        File sd = Environment.getExternalStorageDirectory();
        return new File[]{
                new File(sd, "Podcasts"),
                new File(sd, "Podcast"),
                new File(sd, "Music/Podcasts"),
                new File(sd, "Music/Podcast"),
                new File(sd, "Download/Podcasts"),
                new File(sd, "Audiobooks")
        };
    }

    private static void collect(File dir, List<File> out, int depth, int maxDepth) {
        if (dir == null || !dir.isDirectory()) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) {
                if (depth < maxDepth) collect(f, out, depth + 1, maxDepth);
            } else if (isAudio(f.getName())) {
                out.add(f);
            }
        }
    }

    public static boolean isAudio(String name) {
        String n = name.toLowerCase(java.util.Locale.US);
        return n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".m4b") || n.endsWith(".aac")
                || n.endsWith(".ogg") || n.endsWith(".opus") || n.endsWith(".wav") || n.endsWith(".flac")
                || n.endsWith(".wma") || n.endsWith(".amr") || n.endsWith(".3gp") || n.endsWith(".mka");
    }

    // ------------------------------------------------------------ MediaStore

    private static void scanMediaStore(Context ctx, List<Entry> entries, Set<String> seen) {
        Cursor c = null;
        try {
            String[] proj = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.SIZE
            };
            String sel = MediaStore.Audio.Media.DATA + " LIKE ? OR " + MediaStore.Audio.Media.DATA + " LIKE ?";
            String[] args = {"%/Podcasts/%", "%/Podcast/%"};
            c = ctx.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, args, null);
            if (c == null) return;
            int idCol = c.getColumnIndex(MediaStore.Audio.Media._ID);
            int dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA);
            int nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE);
            while (c.moveToNext()) {
                long id = c.getLong(idCol);
                String data = dataCol >= 0 ? c.getString(dataCol) : null;
                String name = nameCol >= 0 ? c.getString(nameCol) : null;
                long size = sizeCol >= 0 ? c.getLong(sizeCol) : 0;
                if (data != null && !seen.add(data)) continue;
                if (name == null) continue;
                File parent = data != null ? new File(data).getParentFile() : null;
                Entry e = new Entry();
                e.absPath = data;
                e.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                e.groupKey = parent != null ? parent.getAbsolutePath() : "media";
                e.groupTitle = parent != null ? parent.getName() : "Podcasts";
                e.fileName = name;
                e.size = size;
                entries.add(e);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------ SAF

    private static void scanTree(Context ctx, Uri tree, List<Entry> entries, Set<String> seen, int depth) {
        String treeId = DocumentsContract.getTreeDocumentId(tree);
        if (treeId == null) return;
        scanTree(ctx, tree, treeId, entries, seen, depth);
    }

    private static void scanTree(Context ctx, Uri tree, String docId, List<Entry> entries, Set<String> seen, int depth) {
        if (depth > 6 || docId == null) return;
        final String treeId = DocumentsContract.getTreeDocumentId(tree);
        if (treeId == null) return;
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = null;
        try {
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
            String[] cols = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
            };
            c = cr.query(childrenUri, cols, null, null, null);
            if (c == null) return;
            while (c.moveToNext()) {
                String childId = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);
                long size = c.getLong(3);
                if (childId == null || name == null) continue;
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(tree, childId);
                if (mime != null && mime.startsWith("vnd.android.document/directory")) {
                    scanTree(ctx, tree, childId, entries, seen, depth + 1);
                    continue;
                }
                if (!isAudio(name)) continue;
                String key = "saf:" + treeId + "/" + childId;
                if (!seen.add(key)) continue;
                Entry e = new Entry();
                e.uri = childUri;
                e.groupKey = "saf:" + treeId + ":" + parentName(childId);
                e.groupTitle = parentName(childId);
                e.fileName = name;
                e.size = size;
                entries.add(e);
            }
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String parentName(String docId) {
        if (docId == null) return "Podcast";
        int i = docId.lastIndexOf(':');
        String last = i >= 0 ? docId.substring(i + 1) : docId;
        int j = last.lastIndexOf('/');
        if (j >= 0) last = last.substring(j + 1);
        if (last.isEmpty()) return "Podcast";
        return last;
    }

    // ------------------------------------------------------------ métadonnées

    private static long durationOf(Context ctx, AudioItem item) {
        if (item.path != null && item.fileName.toLowerCase(java.util.Locale.US).endsWith(".mp3")) {
            long d = Mp3Info.durationMs(new File(item.path));
            if (d > 0) return d;
        }
        MediaMetadataRetriever r = null;
        try {
            r = new MediaMetadataRetriever();
            if (item.uri != null) {
                r.setDataSource(ctx, item.uri);
            } else {
                r.setDataSource(item.path);
            }
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) {
                try {
                    return Long.parseLong(d);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (r != null) {
                try {
                    r.release();
                } catch (Throwable ignored) {
                }
            }
        }
        return 0;
    }

    private static void readTags(Context ctx, AudioItem item) {
        MediaMetadataRetriever r = null;
        try {
            r = new MediaMetadataRetriever();
            if (item.uri != null) {
                r.setDataSource(ctx, item.uri);
            } else {
                r.setDataSource(item.path);
            }
            item.album = Names.cleanTag(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            item.artist = Names.cleanTag(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            if (item.artist == null) {
                item.artist = Names.cleanTag(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
            }
            byte[] pic = r.getEmbeddedPicture();
            if (pic != null && pic.length > 0) item.art = pic;
        } catch (Throwable ignored) {
        } finally {
            if (r != null) {
                try {
                    r.release();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
