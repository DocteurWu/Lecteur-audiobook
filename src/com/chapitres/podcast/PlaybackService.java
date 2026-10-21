package com.chapitres.podcast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service de lecture : fond, notification, écran verrouillé, commandes
 * casque, reprise de position, minuterie et vitesse de lecture.
 */
public class PlaybackService extends Service {

    public static final String ACTION_PLAY_QUEUE = "com.chapitres.podcast.action.PLAY_QUEUE";
    public static final String ACTION_TOGGLE = "com.chapitres.podcast.action.TOGGLE";
    public static final String ACTION_NEXT = "com.chapitres.podcast.action.NEXT";
    public static final String ACTION_PREV = "com.chapitres.podcast.action.PREV";
    public static final String ACTION_SEEK = "com.chapitres.podcast.action.SEEK";
    public static final String ACTION_STOP = "com.chapitres.podcast.action.STOP";
    public static final String EXTRA_POSITION = "position";

    private static final String CHANNEL_ID = "chapitres_lecture";
    private static final int NOTIF_ID = 4711;

    public interface Listener {
        void onPlaybackChanged();
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile PlaybackService instance;
    private static volatile ArrayList<AudioItem> pendingQueue;
    private static volatile int pendingIndex = -1;

    private MediaPlayer player;
    private MediaSession session;
    private NotificationManager notificationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<AudioItem> queue = new ArrayList<>();
    private int index = -1;
    private boolean prepared;
    private float speed = 1f;
    private long sleepUntil;
    private boolean sleepAtChapterEnd;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean resumeOnFocusGain;
    private int lastSecond = -1;
    private long lastSave;
    private boolean foreground;
    private boolean noisyRegistered;

    private final AudioManager.OnAudioFocusChangeListener focusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    resumeOnFocusGain = true;
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (player != null) player.setVolume(0.25f, 0.25f);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (player != null) player.setVolume(1f, 1f);
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false;
                        resume();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // ------------------------------------------------------------ statique

    public static void play(Context ctx, List<AudioItem> queue, int index) {
        if (queue == null || queue.isEmpty()) return;
        pendingQueue = new ArrayList<>(queue);
        pendingIndex = index;
        Intent i = new Intent(ctx, PlaybackService.class).setAction(ACTION_PLAY_QUEUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void toggle(Context ctx) {
        send(ctx, ACTION_TOGGLE);
    }

    public static void next(Context ctx) {
        send(ctx, ACTION_NEXT);
    }

    public static void previous(Context ctx) {
        send(ctx, ACTION_PREV);
    }

    public static void seekTo(Context ctx, int positionMs) {
        if (instance == null) return;
        Intent i = new Intent(ctx, PlaybackService.class).setAction(ACTION_SEEK).putExtra(EXTRA_POSITION, positionMs);
        ctx.startService(i);
    }

    public static void setSpeed(Context ctx, float s) {
        Prefs.setSpeed(s);
        if (instance == null) return;
        instance.applySpeed(s);
    }

    public static void sleepInMinutes(Context ctx, int minutes) {
        if (instance == null) return;
        instance.sleepUntil = System.currentTimeMillis() + minutes * 60_000L;
        instance.sleepAtChapterEnd = false;
        instance.notifyListeners();
    }

    public static void sleepAtChapterEnd(Context ctx) {
        if (instance == null) return;
        instance.sleepAtChapterEnd = true;
        instance.sleepUntil = 0;
        instance.notifyListeners();
    }

    public static void cancelSleep(Context ctx) {
        if (instance == null) return;
        instance.sleepUntil = 0;
        instance.sleepAtChapterEnd = false;
        instance.notifyListeners();
    }

    public static long sleepRemaining() {
        if (instance == null) return 0;
        if (instance.sleepAtChapterEnd) return -1;
        long r = instance.sleepUntil - System.currentTimeMillis();
        return r > 0 ? r : 0;
    }

    public static boolean isSleepAtChapterEnd() {
        return instance != null && instance.sleepAtChapterEnd;
    }

    private static void send(Context ctx, String action) {
        if (instance == null) return;
        Intent i = new Intent(ctx, PlaybackService.class).setAction(action);
        ctx.startService(i);
    }

    public static void addListener(Listener l) {
        if (l != null && !LISTENERS.contains(l)) LISTENERS.add(l);
    }

    public static void removeListener(Listener l) {
        LISTENERS.remove(l);
    }

    private void notifyListeners() {
        for (Listener l : LISTENERS) {
            l.onPlaybackChanged();
        }
    }

    public static AudioItem current() {
        if (instance == null) return null;
        synchronized (instance.queue) {
            if (instance.index < 0 || instance.index >= instance.queue.size()) return null;
            return instance.queue.get(instance.index);
        }
    }

    public static List<AudioItem> currentQueue() {
        if (instance == null) return new ArrayList<>();
        synchronized (instance.queue) {
            return new ArrayList<>(instance.queue);
        }
    }

    public static int currentIndex() {
        return instance == null ? -1 : instance.index;
    }

    public static boolean isPlaying() {
        if (instance == null || instance.player == null) return false;
        try {
            return instance.player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static int position() {
        if (instance == null || instance.player == null) return 0;
        try {
            return instance.player.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public static int duration() {
        if (instance == null || instance.player == null) return 0;
        try {
            int d = instance.player.getDuration();
            if (d > 0) return d;
        } catch (IllegalStateException ignored) {
        }
        AudioItem item = current();
        return item == null ? 0 : (int) item.durationMs;
    }

    public static float speed() {
        return instance == null ? Prefs.getSpeed() : instance.speed;
    }

    // ------------------------------------------------------------- cycle de vie

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Prefs.init(this);
        speed = Prefs.getSpeed();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createChannel();
        createSession();
        registerNoisy();
        handler.post(ticker);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) action = ACTION_PLAY_QUEUE;

        switch (action) {
            case ACTION_PLAY_QUEUE:
                ensureForeground();
                if (pendingQueue != null && pendingIndex >= 0 && pendingIndex < pendingQueue.size()) {
                    queue.clear();
                    queue.addAll(pendingQueue);
                    index = pendingIndex;
                    load(0, true);
                }
                break;
            case ACTION_TOGGLE:
                if (isPlaying()) pause();
                else resume();
                break;
            case ACTION_NEXT:
                skip(1);
                break;
            case ACTION_PREV:
                skip(-1);
                break;
            case ACTION_SEEK: {
                int pos = intent.getIntExtra(EXTRA_POSITION, 0);
                seekTo(pos);
                break;
            }
            case ACTION_STOP:
                stopEverything();
                break;
            default:
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        savePosition();
        releasePlayer();
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        instance = null;
        super.onDestroy();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Lecture", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Contrôles de lecture du podcast");
            ch.setShowBadge(false);
            notificationManager.createNotificationChannel(ch);
        }
    }

    private void createSession() {
        session = new MediaSession(this, "ChapitresSession");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onStop() {
                stopEverything();
            }

            @Override
            public void onSkipToNext() {
                skip(1);
            }

            @Override
            public void onSkipToPrevious() {
                skip(-1);
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setSessionActivity(playerIntent());
        session.setActive(true);
    }

    private void registerNoisy() {
        if (noisyRegistered) return;
        try {
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        pause();
                    }
                }
            }, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            noisyRegistered = true;
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------- lecture

    private void load(long startPos, boolean play) {
        if (index < 0 || index >= queue.size()) return;
        releasePlayer();
        AudioItem item = queue.get(index);
        Prefs.setLastKey(item.key);
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            if (item.uri != null) {
                mp.setDataSource(this, item.uri);
            } else {
                mp.setDataSource(item.path);
            }
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    long pos = startPos > 0 ? startPos : Prefs.getPosition(item.key);
                    if (pos > 0 && pos < mp.getDuration()) {
                        mp.seekTo((int) pos);
                    }
                    if (play) {
                        requestFocus();
                        mp.start();
                        applySpeed(speed);
                        acquireWakeLock();
                    }
                    updateSession();
                    updateNotification(true);
                    notifyListeners();
                }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    onChapterFinished();
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    releasePlayer();
                    updateNotification(true);
                    notifyListeners();
                    return true;
                }
            });
            player = mp;
            mp.prepareAsync();
            updateSession();
            notifyListeners();
        } catch (Throwable t) {
            releasePlayer();
            notifyListeners();
        }
    }

    private void onChapterFinished() {
        AudioItem item = current();
        if (item != null) {
            Prefs.setPosition(item.key, 0);
            Prefs.setDone(item.key, true);
        }
        if (sleepAtChapterEnd) {
            sleepAtChapterEnd = false;
            pause();
            return;
        }
        if (index + 1 < queue.size()) {
            index++;
            load(0, true);
        } else {
            pause();
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.setOnPreparedListener(null);
                player.setOnCompletionListener(null);
                player.setOnErrorListener(null);
                if (player.isPlaying()) player.stop();
            } catch (Throwable ignored) {
            }
            try {
                player.release();
            } catch (Throwable ignored) {
            }
            player = null;
        }
        prepared = false;
    }

    private void resume() {
        if (player == null) {
            // Recharge le dernier chapitre si la file est vide
            if (index >= 0 && index < queue.size()) load(Prefs.getPosition(queue.get(index).key), true);
            return;
        }
        if (!prepared) return;
        try {
            requestFocus();
            player.start();
            applySpeed(speed);
            acquireWakeLock();
        } catch (Throwable ignored) {
        }
        updateNotification(true);
        updateSession();
        notifyListeners();
    }

    private void pause() {
        if (player == null) return;
        try {
            if (player.isPlaying()) player.pause();
        } catch (Throwable ignored) {
        }
        savePosition();
        releaseWakeLock();
        updateNotification(true);
        updateSession();
        notifyListeners();
    }

    private void stopEverything() {
        savePosition();
        releasePlayer();
        if (foreground) {
            stopForeground(true);
            foreground = false;
        }
        updateSession();
        notifyListeners();
        stopSelf();
    }

    private void skip(int delta) {
        if (queue.isEmpty()) return;
        if (delta > 0) {
            if (index + 1 < queue.size()) {
                savePosition();
                index++;
                load(0, true);
            } else {
                // On repasse au début du dernier chapitre
                seekTo(0);
            }
        } else {
            // Un retour arrière de moins de 3 secondes revient au début du chapitre
            if (position() > 3000) {
                seekTo(0);
            } else if (index - 1 >= 0) {
                savePosition();
                index--;
                load(Prefs.getPosition(queue.get(index).key), true);
            } else {
                seekTo(0);
            }
        }
    }

    private void seekTo(int positionMs) {
        if (player == null || !prepared) return;
        try {
            player.seekTo(positionMs);
        } catch (Throwable ignored) {
        }
        updateSession();
        notifyListeners();
    }

    private void applySpeed(float s) {
        speed = s;
        Prefs.setSpeed(s);
        if (player == null || !prepared) return;
        try {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(s);
            player.setPlaybackParams(params);
        } catch (Throwable ignored) {
        }
    }

    private void savePosition() {
        AudioItem item = current();
        if (item == null || player == null) return;
        try {
            if (player.getCurrentPosition() > 1500) {
                Prefs.setPosition(item.key, player.getCurrentPosition());
            }
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- minuterie

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();
                if (sleepUntil > 0 && now >= sleepUntil) {
                    sleepUntil = 0;
                    pause();
                    notifyListeners();
                }
                if (now - lastSave > 2000) {
                    lastSave = now;
                    savePosition();
                }
                int sec = position() / 1000;
                if (sec != lastSecond) {
                    lastSecond = sec;
                    updateNotification(false);
                    updateSession();
                }
                notifyListeners();
            } catch (Throwable ignored) {
            }
            handler.postDelayed(this, 500);
        }
    };

    // ---------------------------------------------------------------- audio focus

    private void requestFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (focusRequest == null) {
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build())
                            .setOnAudioFocusChangeListener(focusListener)
                            .build();
                }
                audioManager.requestAudioFocus(focusRequest);
            } else {
                audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        } catch (Throwable ignored) {
        }
    }

    private void acquireWakeLock() {
        try {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "chapitres:lecture");
                wakeLock.setReferenceCounted(false);
            }
            if (!wakeLock.isHeld()) wakeLock.acquire();
        } catch (Throwable ignored) {
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------- notification

    private PendingIntent playerIntent() {
        Intent i = new Intent(this, PlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, i, pendingFlags());
    }

    private int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }

    private PendingIntent serviceIntent(String action, int requestCode) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, i, pendingFlags());
    }

    private void ensureForeground() {
        Notification n = buildNotification();
        if (!foreground) {
            startForeground(NOTIF_ID, n);
            foreground = true;
        } else {
            notificationManager.notify(NOTIF_ID, n);
        }
    }

    private void updateNotification(boolean force) {
        if (!foreground) return;
        notificationManager.notify(NOTIF_ID, buildNotification());
    }

    private Notification buildNotification() {
        AudioItem item = current();
        String title = item == null ? "Chapitres" : item.title;
        String text = item == null ? "Aucun podcast" : item.displayAlbum();

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(R.drawable.ic_stat_podcast)
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(item == null ? "" : "Chapitre " + (index + 1))
                .setContentIntent(playerIntent())
                .setOngoing(isPlaying())
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        Bitmap art = null;
        if (item != null && item.podcast != null) {
            art = ArtCache.get(item.podcast.id, item.podcast.art, 512);
        }
        if (art != null) b.setLargeIcon(art);

        b.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_skip_prev),
                "Précédent",
                serviceIntent(ACTION_PREV, 1)).build());
        b.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play),
                isPlaying() ? "Pause" : "Lecture",
                serviceIntent(ACTION_TOGGLE, 2)).build());
        b.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_skip_next),
                "Suivant",
                serviceIntent(ACTION_NEXT, 3)).build());

        Notification.MediaStyle style = new Notification.MediaStyle();
        style.setMediaSession(session.getSessionToken());
        style.setShowActionsInCompactView(0, 1, 2);
        b.setStyle(style);
        return b.build();
    }

    private void updateSession() {
        if (session == null) return;
        AudioItem item = current();
        MediaMetadata.Builder meta = new MediaMetadata.Builder();
        if (item != null) {
            meta.putString(MediaMetadata.METADATA_KEY_TITLE, item.title);
            meta.putString(MediaMetadata.METADATA_KEY_ALBUM, item.displayAlbum());
            meta.putString(MediaMetadata.METADATA_KEY_ARTIST, item.displayArtist());
            meta.putLong(MediaMetadata.METADATA_KEY_DURATION, item.durationMs);
            if (item.podcast != null) {
                Bitmap art = ArtCache.get(item.podcast.id, item.podcast.art, 512);
                if (art != null) meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art);
            }
        }
        session.setMetadata(meta.build());

        int state = isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_STOP;
        PlaybackState ps = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, position(), speed)
                .build();
        session.setPlaybackState(ps);
    }
}
