// Patch : tri des chapitres + menu, fond pochette dans le lecteur, aide au premier lancement
const fs = require('fs');
const path = require('path');
const dir = process.argv[2];

function edit(file, pairs) {
  const p = path.join(dir, file);
  let s = fs.readFileSync(p, 'utf8');
  for (const [from, to] of pairs) {
    if (!s.includes(from)) { console.error('!! introuvable', file, ':', JSON.stringify(from.slice(0, 70))); continue; }
    s = s.split(from).join(to);
  }
  fs.writeFileSync(p, s, 'utf8');
  console.log('ok', file);
}

// ------------------------------------------------------------- EpisodesActivity
edit('EpisodesActivity.java', [
  ['import android.view.LayoutInflater;', 'import android.view.LayoutInflater;\nimport android.widget.PopupMenu;\nimport android.view.MenuItem;'],
  [`    private final List<AudioItem> visible = new ArrayList<>();`,
   `    private final List<AudioItem> visible = new ArrayList<>();
    private static final int SORT_ORDER = 0;
    private static final int SORT_TITLE = 1;
    private static final int SORT_DURATION = 2;
    private int sortMode = SORT_ORDER;`],
  [`        filter("");`,
   `        findViewById(R.id.btnOverflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        filter("");`],
  [`    private void filter(String query) {
        String q = Names.fold(query.trim());
        visible.clear();
        for (AudioItem a : podcast.episodes) {
            if (q.isEmpty() || Names.fold(a.title + " " + a.fileName).contains(q)) {
                visible.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }`,
   `    private void filter(String query) {
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
    }`],
]);

// --------------------------------------------------------------- MainActivity
// une ligne d'aide au premier lancement
edit('MainActivity.java', [
  [`        requestPermissionIfNeeded();
        startScan();`,
   `        requestPermissionIfNeeded();
        startScan();
        if (!Prefs.hasSeenHint()) {
            Prefs.setSeenHint();
            Toast.makeText(this, "Touchez un podcast pour voir ses chapitres, ou ▶ pour écouter.",
                    Toast.LENGTH_LONG).show();
        }`],
]);

// ------------------------------------------------------------------ Prefs
edit('Prefs.java', [
  [`    public static Set<String> getTrees() {`,
   `    private static final String K_HINT = "hint_seen";

    public static boolean hasSeenHint() {
        return sp != null && sp.getBoolean(K_HINT, false);
    }

    public static void setSeenHint() {
        if (sp == null) return;
        sp.edit().putBoolean(K_HINT, true).apply();
    }

    public static Set<String> getTrees() {`],
]);
