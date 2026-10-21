package com.chapitres.podcast;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.PopupMenu;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQ_TREE = 42;
    private static final int REQ_PERMISSION = 43;

    private ListView list;
    private BaseAdapter adapter;
    private final List<Object> rows = new ArrayList<>();
    private List<Podcast> podcasts = new ArrayList<>();
    private EditText search;
    private View loadingState;
    private View emptyState;
    private TextView txtSubtitle;
    private TextView txtLoading;
    private View resumeCard;
    private TextView txtResumeTitle;
    private TextView txtResumeMeta;
    private boolean searching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.listContent);
        search = findViewById(R.id.edtSearch);
        loadingState = findViewById(R.id.loadingState);
        emptyState = findViewById(R.id.emptyState);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        txtLoading = findViewById(R.id.txtLoading);
        resumeCard = findViewById(R.id.cardResume);
        txtResumeTitle = findViewById(R.id.txtResumeTitle);
        txtResumeMeta = findViewById(R.id.txtResumeMeta);

        adapter = new RowAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onRowClicked(position);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object row = rows.get(position);
                if (row instanceof AudioItem) {
                    showChapterMenu((AudioItem) row);
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.btnAddFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFolder();
            }
        });
        findViewById(R.id.btnRescan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
        findViewById(R.id.btnOverflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflow(v);
            }
        });
        findViewById(R.id.btnResumePlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeLast();
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString());
            }
        });

        requestPermissionIfNeeded();
        startScan();
        if (!Prefs.hasSeenHint()) {
            Prefs.setSeenHint();
            Toast.makeText(this, "Touchez un podcast pour voir ses chapitres, ou ▶ pour écouter.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateResumeCard();
    }

    private void requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            startScan();
        }
    }

    // -------------------------------------------------------------- scan

    private void startScan() {
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        txtLoading.setText(R.string.scanning);
        Library.scan(this, new Library.Callback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtLoading.setText(message);
                    }
                });
            }

            @Override
            public void onResult(List<Podcast> result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        podcasts = result;
                        loadingState.setVisibility(View.GONE);
                        applyFilter(search.getText().toString());
                        updateResumeCard();
                    }
                });
            }
        });
    }

    private void applyFilter(String query) {
        String q = Names.fold(query.trim());
        rows.clear();
        searching = !q.isEmpty();

        if (!searching) {
            for (Podcast p : podcasts) rows.add(p);
            int total = 0;
            for (Podcast p : podcasts) total += p.episodeCount();
            txtSubtitle.setText(podcasts.isEmpty()
                    ? "Aucun dossier analysé"
                    : podcasts.size() + " podcast(s) · " + total + " chapitres");
        } else {
            for (Podcast p : podcasts) {
                boolean podMatch = Names.fold(p.title).contains(q)
                        || Names.fold(p.artist).contains(q);
                for (AudioItem a : p.episodes) {
                    String hay = Names.fold(a.title + " " + a.fileName + " " + p.title + " " + (a.artist == null ? "" : a.artist));
                    if (hay.contains(q)) rows.add(a);
                }
                if (podMatch && p.episodes.isEmpty()) rows.add(p);
            }
            txtSubtitle.setText(rows.size() + " résultat(s)");
        }

        emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        if (!rows.isEmpty()) emptyState.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        updateResumeCard();
    }

    private void updateResumeCard() {
        String last = Prefs.getLastKey();
        AudioItem item = last == null ? null : Library.findByKey(last);
        if (item == null) {
            resumeCard.setVisibility(View.GONE);
            return;
        }
        long pos = Prefs.getPosition(last);
        resumeCard.setVisibility(View.VISIBLE);
        txtResumeTitle.setText(item.title);
        txtResumeMeta.setText(item.displayAlbum() + " · " + Names.clock(pos) + " / " + Names.clock(item.durationMs));
    }

    private void resumeLast() {
        String last = Prefs.getLastKey();
        AudioItem item = last == null ? null : Library.findByKey(last);
        if (item == null || item.podcast == null) {
            Toast.makeText(this, "Rien à reprendre", Toast.LENGTH_SHORT).show();
            return;
        }
        List<AudioItem> q = item.podcast.episodes;
        PlaybackService.play(this, q, q.indexOf(item));
        startActivity(new Intent(this, PlayerActivity.class));
    }

    private void onRowClicked(int position) {
        Object row = rows.get(position);
        if (row instanceof Podcast) {
            Podcast p = (Podcast) row;
            Intent i = new Intent(this, EpisodesActivity.class);
            i.putExtra(EpisodesActivity.EXTRA_PODCAST_ID, p.id);
            startActivity(i);
        } else if (row instanceof AudioItem) {
            AudioItem a = (AudioItem) row;
            if (a.podcast == null) return;
            List<AudioItem> q = a.podcast.episodes;
            PlaybackService.play(this, q, q.indexOf(a));
            startActivity(new Intent(this, PlayerActivity.class));
        }
    }

    private void showChapterMenu(final AudioItem item) {
        final boolean done = item.isDone();
        String[] options = done
                ? new String[]{getString(R.string.mark_not_done), getString(R.string.reset_position)}
                : new String[]{getString(R.string.mark_done), getString(R.string.reset_position)};
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Prefs.setDone(item.key, !item.isDone());
                            adapter.notifyDataSetChanged();
                            updateResumeCard();
                        } else {
                            Prefs.setPosition(item.key, 0);
                            adapter.notifyDataSetChanged();
                            updateResumeCard();
                        }
                    }
                })
                .show();
    }

    // -------------------------------------------------------------- menus

    private void showOverflow(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Tout écouter");
        popup.getMenu().add(0, 2, 1, "Dossiers ajoutés");
        popup.getMenu().add(0, 3, 2, "Réinitialiser les positions");
        popup.getMenu().add(0, 4, 3, "À propos");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        playEverything();
                        return true;
                    case 2:
                        showTrees();
                        return true;
                    case 3:
                        Prefs.clearAllPositions();
                        adapter.notifyDataSetChanged();
                        updateResumeCard();
                        Toast.makeText(MainActivity.this, "Positions réinitialisées", Toast.LENGTH_SHORT).show();
                        return true;
                    case 4:
                        showAbout();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void playEverything() {
        List<AudioItem> all = new ArrayList<>();
        for (Podcast p : podcasts) {
            all.addAll(p.episodes);
        }
        if (all.isEmpty()) {
            Toast.makeText(this, "Aucun chapitre", Toast.LENGTH_SHORT).show();
            return;
        }
        PlaybackService.play(this, all, 0);
        startActivity(new Intent(this, PlayerActivity.class));
    }

    private void showTrees() {
        final Set<String> trees = Prefs.getTrees();
        if (trees.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Dossiers ajoutés")
                    .setMessage("Aucun dossier ajouté. Utilisez « Ajouter un dossier » pour choisir un emplacement précis (stockage interne, carte SD, etc.).")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        final String[] items = trees.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Dossiers ajoutés")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.removeTree(items[which]);
                        try {
                            getContentResolver().releasePersistableUriPermission(Uri.parse(items[which]),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Throwable ignored) {
                        }
                        startScan();
                    }
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Chapitres")
                .setMessage("Lecteur de podcasts par chapitres.\n\n"
                        + "• Analyse le dossier « Podcasts » du stockage interne\n"
                        + "• Reprise de lecture automatique\n"
                        + "• Lecture en arrière-plan, écran verrouillé et casque\n"
                        + "• Vitesse de lecture et minuterie\n\n"
                        + "Version 1.0")
                .setPositiveButton("Fermer", null)
                .show();
    }

    // -------------------------------------------------------------- SAF

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        Uri.parse("content://com.android.externalstorage.documents/root%3APodcasts"));
            } catch (Throwable ignored) {
            }
        }
        try {
            startActivityForResult(intent, REQ_TREE);
        } catch (Throwable t) {
            Toast.makeText(this, "Sélecteur de dossier indisponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_TREE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        Prefs.addTree(uri.toString());
        Toast.makeText(this, "Dossier ajouté", Toast.LENGTH_SHORT).show();
        startScan();
    }

    // -------------------------------------------------------------- adaptateur

    private class RowAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof Podcast ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object row = rows.get(position);
            if (row instanceof Podcast) return podcastView((Podcast) row, convertView, parent);
            return chapterView((AudioItem) row, convertView, parent);
        }

        private View podcastView(Podcast p, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_podcast, parent, false);
            }
            TextView title = v.findViewById(R.id.txtTitle);
            TextView meta = v.findViewById(R.id.txtMeta);
            TextView progress = v.findViewById(R.id.txtProgress);
            ImageView cover = v.findViewById(R.id.imgCover);
            ImageButton play = v.findViewById(R.id.btnPlay);

            title.setText(p.title);
            String artist = p.artist != null && p.artist.length() > 0 ? p.artist : "Podcast";
            meta.setText(artist + " · " + p.episodeCount() + " chapitres · " + Names.duration(p.totalDurationMs));

            Bitmap art = ArtCache.get(p.id, p.art, 256);
            if (art != null) {
                cover.setImageBitmap(art);
                cover.setPadding(0, 0, 0, 0);
                cover.setImageTintList(null);
            } else {
                cover.setImageResource(R.drawable.ic_headphones);
                cover.setPadding(14, 14, 14, 14);
                cover.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.subtext)));
            }

            // Progression : chapitre en cours
            String prog = null;
            for (AudioItem a : p.episodes) {
                long pos = Prefs.getPosition(a.key);
                if (pos > 1500) {
                    prog = "Reprise : " + a.title + " (" + Names.clock(pos) + ")";
                    break;
                }
            }
            if (prog != null) {
                progress.setVisibility(View.VISIBLE);
                progress.setText(prog);
            } else {
                progress.setVisibility(View.GONE);
            }

            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlaybackService.play(MainActivity.this, p.episodes, 0);
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                }
            });
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(MainActivity.this, EpisodesActivity.class);
                    i.putExtra(EpisodesActivity.EXTRA_PODCAST_ID, p.id);
                    startActivity(i);
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Intent i = new Intent(MainActivity.this, EpisodesActivity.class);
                    i.putExtra(EpisodesActivity.EXTRA_PODCAST_ID, p.id);
                    startActivity(i);
                    return true;
                }
            });
            return v;
        }

        private View chapterView(AudioItem a, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_chapter, parent, false);
            }
            TextView badge = v.findViewById(R.id.txtBadge);
            TextView title = v.findViewById(R.id.txtTitle);
            TextView meta = v.findViewById(R.id.txtMeta);
            ImageView done = v.findViewById(R.id.imgDone);
            ProgressBar bar = v.findViewById(R.id.barProgress);

            badge.setText(a.index >= 0 ? String.valueOf(a.index) : "•");
            title.setText(a.title);
            StringBuilder sb = new StringBuilder();
            if (a.podcast != null) sb.append(a.podcast.title);
            sb.append(" · ").append(Names.duration(a.durationMs));
            if (a.sizeBytes > 0) sb.append(" · ").append(Names.size(a.sizeBytes));
            meta.setText(sb.toString());

            boolean isDone = a.isDone();
            done.setVisibility(isDone ? View.VISIBLE : View.GONE);

            long pos = Prefs.getPosition(a.key);
            if (pos > 1500 && a.durationMs > 0) {
                bar.setVisibility(View.VISIBLE);
                bar.setProgress((int) (pos * 1000 / a.durationMs));
            } else {
                bar.setVisibility(View.GONE);
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (a.podcast == null) return;
                    List<AudioItem> q = a.podcast.episodes;
                    PlaybackService.play(MainActivity.this, q, q.indexOf(a));
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                }
            });
            return v;
        }
    }
}
