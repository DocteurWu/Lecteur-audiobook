package com.chapitres.podcast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Écran « En cours » : pochette, barre de progression, transports, vitesse, minuterie. */
public class PlayerActivity extends Activity implements PlaybackService.Listener {

    private ImageView imgArt;
    private ImageView imgBackdrop;
    private TextView txtTitle;
    private TextView txtAlbum;
    private TextView txtPosition;
    private TextView txtDuration;
    private TextView txtSleepStatus;
    private ImageButton btnPlayPause;
    private Button btnSpeed;
    private SeekBar seekBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean userSeeking;

    private final float[] SPEEDS = {0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imgArt = findViewById(R.id.imgArt);
        imgBackdrop = findViewById(R.id.imgBackdrop);
        txtTitle = findViewById(R.id.txtTitle);
        txtAlbum = findViewById(R.id.txtAlbum);
        txtPosition = findViewById(R.id.txtPosition);
        txtDuration = findViewById(R.id.txtDuration);
        txtSleepStatus = findViewById(R.id.txtSleepStatus);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnSpeed = findViewById(R.id.btnSpeed);
        seekBar = findViewById(R.id.seekBar);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnPrevChapter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackService.previous(PlayerActivity.this);
            }
        });
        findViewById(R.id.btnNextChapter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackService.next(PlayerActivity.this);
            }
        });
        findViewById(R.id.btnBack15).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackService.seekTo(PlayerActivity.this, Math.max(0, PlaybackService.position() - 15000));
            }
        });
        findViewById(R.id.btnFwd15).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int d = PlaybackService.duration();
                int p = PlaybackService.position() + 15000;
                PlaybackService.seekTo(PlayerActivity.this, d > 0 ? Math.min(d, p) : p);
            }
        });
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackService.toggle(PlayerActivity.this);
            }
        });
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeedDialog();
            }
        });
        findViewById(R.id.btnSleep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSleepDialog();
            }
        });
        findViewById(R.id.btnChapters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioItem item = PlaybackService.current();
                if (item == null || item.podcast == null) return;
                android.content.Intent i = new android.content.Intent(PlayerActivity.this, EpisodesActivity.class);
                i.putExtra(EpisodesActivity.EXTRA_PODCAST_ID, item.podcast.id);
                startActivity(i);
            }
        });
        findViewById(R.id.btnOverflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflow(v);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int dur = PlaybackService.duration();
                if (dur > 0) {
                    txtPosition.setText(Names.clock((long) dur * progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                int dur = PlaybackService.duration();
                int pos = dur > 0 ? (int) ((long) dur * seekBar.getProgress() / 1000) : 0;
                PlaybackService.seekTo(PlayerActivity.this, pos);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlaybackService.addListener(this);
        handler.post(tick);
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackService.removeListener(this);
        handler.removeCallbacks(tick);
    }

    @Override
    public void onPlaybackChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    private void refresh() {
        AudioItem item = PlaybackService.current();
        if (item == null) {
            txtTitle.setText("Aucune lecture");
            txtAlbum.setText("");
            return;
        }
        txtTitle.setText(item.title);
        StringBuilder sb = new StringBuilder(item.displayAlbum());
        String artist = item.displayArtist();
        if (artist.length() > 0) sb.append(" · ").append(artist);
        int idx = PlaybackService.currentIndex();
        if (idx >= 0) sb.append(" · ").append(idx + 1).append("/").append(PlaybackService.currentQueue().size());
        txtAlbum.setText(sb.toString());

        Bitmap art = null;
        if (item.podcast != null) {
            art = ArtCache.get(item.podcast.id, item.podcast.art, 720);
            Bitmap bg = ArtCache.get(item.podcast.id + ":bg", item.podcast.art, 720);
            if (bg != null) {
                imgBackdrop.setImageBitmap(bg);
                imgBackdrop.setVisibility(View.VISIBLE);
            } else {
                imgBackdrop.setVisibility(View.GONE);
            }
        }
        if (art != null) {
            imgArt.setImageBitmap(art);
            imgArt.setPadding(0, 0, 0, 0);
            imgArt.setImageTintList(null);
        } else {
            imgArt.setImageResource(R.drawable.ic_headphones);
            imgArt.setPadding(40, 40, 40, 40);
            imgArt.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.subtext)));
        }

        boolean playing = PlaybackService.isPlaying();
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);

        int dur = PlaybackService.duration();
        int pos = PlaybackService.position();
        txtPosition.setText(Names.clock(pos));
        txtDuration.setText(Names.clock(dur));
        if (!userSeeking && dur > 0) {
            seekBar.setProgress((int) ((long) pos * 1000 / dur));
        }

        float s = PlaybackService.speed();
        btnSpeed.setText(formatSpeed(s) + "x");

        long remaining = PlaybackService.sleepRemaining();
        if (PlaybackService.isSleepAtChapterEnd()) {
            txtSleepStatus.setVisibility(View.VISIBLE);
            txtSleepStatus.setText("Minuterie : à la fin du chapitre");
        } else if (remaining > 0) {
            txtSleepStatus.setVisibility(View.VISIBLE);
            txtSleepStatus.setText("Minuterie : arrêt dans " + Names.duration(remaining));
        } else {
            txtSleepStatus.setVisibility(View.GONE);
        }
    }

    private static String formatSpeed(float s) {
        String v = String.valueOf(s);
        return v.replace(".", ",");
    }

    private void showSpeedDialog() {
        String[] labels = new String[SPEEDS.length];
        int checked = 0;
        float current = PlaybackService.speed();
        for (int i = 0; i < SPEEDS.length; i++) {
            labels[i] = formatSpeed(SPEEDS[i]) + "x";
            if (Math.abs(SPEEDS[i] - current) < 0.01) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Vitesse de lecture")
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PlaybackService.setSpeed(PlayerActivity.this, SPEEDS[which]);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showSleepDialog() {
        String[] options = {
                getString(R.string.sleep_off),
                getString(R.string.sleep_15),
                getString(R.string.sleep_30),
                getString(R.string.sleep_45),
                getString(R.string.sleep_60),
                getString(R.string.sleep_chapter)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.sleep_timer)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                PlaybackService.cancelSleep(PlayerActivity.this);
                                break;
                            case 1:
                                PlaybackService.sleepInMinutes(PlayerActivity.this, 15);
                                break;
                            case 2:
                                PlaybackService.sleepInMinutes(PlayerActivity.this, 30);
                                break;
                            case 3:
                                PlaybackService.sleepInMinutes(PlayerActivity.this, 45);
                                break;
                            case 4:
                                PlaybackService.sleepInMinutes(PlayerActivity.this, 60);
                                break;
                            case 5:
                                PlaybackService.sleepAtChapterEnd(PlayerActivity.this);
                                break;
                            default:
                                break;
                        }
                    }
                })
                .show();
    }

    private void showOverflow(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.mark_done));
        popup.getMenu().add(0, 2, 1, getString(R.string.reset_position));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AudioItem current = PlaybackService.current();
                if (current == null) return false;
                if (item.getItemId() == 1) {
                    Prefs.setDone(current.key, !current.isDone());
                    Toast.makeText(PlayerActivity.this,
                            current.isDone() ? "Marqué comme écouté" : "Marquage annulé", Toast.LENGTH_SHORT).show();
                } else if (item.getItemId() == 2) {
                    Prefs.setPosition(current.key, 0);
                    PlaybackService.seekTo(PlayerActivity.this, 0);
                    Toast.makeText(PlayerActivity.this, "Position réinitialisée", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        popup.show();
    }
}
