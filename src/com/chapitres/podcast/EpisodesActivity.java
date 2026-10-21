package com.chapitres.podcast;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Liste des chapitres d'un podcast. */
public class EpisodesActivity extends Activity {

    public static final String EXTRA_PODCAST_ID = "podcast_id";

    private Podcast podcast;
    private final List<AudioItem> visible = new ArrayList<>();
    private static final int SORT_ORDER = 0;
    private static final int SORT_TITLE = 1;
    private static final int SORT_DURATION = 2;
    private int sortMode = SORT_ORDER;
    private ListView list;
    private BaseAdapter adapter;
    private TextView txtTitle;
    private TextView txtMeta;
    private ImageView imgCover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        setContentView(R.layout.activity_episodes);

        list = findViewById(R.id.listChapters);
        txtTitle = findViewById(R.id.txtPodcastTitle);
        txtMeta = findViewById(R.id.txtPodcastMeta);
        imgCover = findViewById(R.id.imgCoverSmall);

        String id = getIntent().getStringExtra(EXTRA_PODCAST_ID);
        podcast = Library.byId(id);
        if (podcast == null) {
            finish();
            return;
        }

        txtTitle.setText(podcast.title);
        String artist = podcast.artist != null && podcast.artist.length() > 0 ? podcast.artist : "Podcast";
        txtMeta.setText(artist + " · " + podcast.episodeCount() + " chapitres · " + Names.duration(podcast.totalDurationMs));

        Bitmap art = ArtCache.get(podcast.id, podcast.art, 256);
        if (art != null) {
            imgCover.setImageBitmap(art);
            imgCover.setPadding(0, 0, 0, 0);
            imgCover.setImageTintList(null);
        }

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        EditText search = findViewById(R.id.edtSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        adapter = new ChapterAdapter();
        list.setAdapter(adapter);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AudioItem item = visible.get(position);
                Prefs.setDone(item.key, !item.isDone());
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        findViewById(R.id.btnOverflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        filter("");
    }

    private void filter(String query) {
        String q = Names.fold(query.trim());
        visible.clear();
        for (AudioItem a : podcast.episodes) {
            if (q.isEmpty() || Names.fold(a.title + " " + a.fileName).contains(q)) {
                visible.add(a);
            }
        }
        sort();
        adapter.notifyDataSetChanged();
    }

    /** Tri : ordre du livre (numéro de fichier), titre, ou durée. */
    private void sort() {
        java.util.Collections.sort(visible, new java.util.Comparator<AudioItem>() {
            @Override
            public int compare(AudioItem a, AudioItem b) {
                switch (sortMode) {
                    case SORT_TITLE:
                        return a.title.compareToIgnoreCase(b.title);
                    case SORT_DURATION:
                        return Long.compare(a.durationMs, b.durationMs);
                    default:
                        if (a.index != b.index) {
                            if (a.index < 0) return 1;
                            if (b.index < 0) return -1;
                            return Integer.compare(a.index, b.index);
                        }
                        return Names.NATURAL.compare(a.fileName, b.fileName);
                }
            }
        });
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Ordre du livre");
        popup.getMenu().add(0, 2, 1, "Par titre");
        popup.getMenu().add(0, 3, 2, "Par durée");
        popup.getMenu().add(0, 4, 3, "Tout écouter");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 1:
                        sortMode = SORT_ORDER;
                        filter(searchQuery());
                        return true;
                    case 2:
                        sortMode = SORT_TITLE;
                        filter(searchQuery());
                        return true;
                    case 3:
                        sortMode = SORT_DURATION;
                        filter(searchQuery());
                        return true;
                    case 4:
                        PlaybackService.play(EpisodesActivity.this, podcast.episodes, 0);
                        startActivity(new Intent(EpisodesActivity.this, PlayerActivity.class));
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private String searchQuery() {
        EditText e = findViewById(R.id.edtSearch);
        return e == null ? "" : e.getText().toString();
    }

    private class ChapterAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return visible.size();
        }

        @Override
        public Object getItem(int position) {
            return visible.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(EpisodesActivity.this).inflate(R.layout.item_chapter, parent, false);
            }
            AudioItem a = visible.get(position);
            TextView badge = v.findViewById(R.id.txtBadge);
            TextView title = v.findViewById(R.id.txtTitle);
            TextView meta = v.findViewById(R.id.txtMeta);
            ImageView done = v.findViewById(R.id.imgDone);
            ProgressBar bar = v.findViewById(R.id.barProgress);

            badge.setText(a.index >= 0 ? String.valueOf(a.index) : "•");
            title.setText(a.title);
            String metaTxt = Names.duration(a.durationMs) + " · " + Names.size(a.sizeBytes);
            int chap = Names.chapterNumber(a.fileName);
            if (chap > 0) metaTxt += " · chap. " + chap;
            meta.setText(metaTxt);

            done.setVisibility(a.isDone() ? View.VISIBLE : View.GONE);

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
                    int idx = podcast.episodes.indexOf(a);
                    if (idx < 0) return;
                    PlaybackService.play(EpisodesActivity.this, podcast.episodes, idx);
                    startActivity(new Intent(EpisodesActivity.this, PlayerActivity.class));
                }
            });
            return v;
        }
    }
}
