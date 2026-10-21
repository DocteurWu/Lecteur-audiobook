// Patch : clics de lignes explicites (fiables), écran allumé pendant la lecture, tri des chapitres
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

// ---------------------------------------------------------------- MainActivity
// Le clic est posé sur la ligne elle-même : plus fiable que le OnItemClickListener du ListView
// (MIUI peut ne pas dispatcher l'événement si un enfant de la ligne est focusable).
edit('MainActivity.java', [
  [`            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlaybackService.play(MainActivity.this, p.episodes, 0);
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                }
            });
            return v;`,
   `            play.setOnClickListener(new View.OnClickListener() {
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
            return v;`],
  [`            if (pos > 1500 && a.durationMs > 0) {
                bar.setVisibility(View.VISIBLE);
                bar.setProgress((int) (pos * 1000 / a.durationMs));
            } else {
                bar.setVisibility(View.GONE);
            }
            return v;`,
   `            if (pos > 1500 && a.durationMs > 0) {
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
            return v;`],
]);

// ------------------------------------------------------------- EpisodesActivity
edit('EpisodesActivity.java', [
  [`        adapter = new ChapterAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioItem item = visible.get(position);
                int idx = podcast.episodes.indexOf(item);
                PlaybackService.play(EpisodesActivity.this, podcast.episodes, idx);
                startActivity(new Intent(EpisodesActivity.this, PlayerActivity.class));
            }
        });`,
   `        adapter = new ChapterAdapter();
        list.setAdapter(adapter);`],
  [`            long pos = Prefs.getPosition(a.key);
            if (pos > 1500 && a.durationMs > 0) {
                bar.setVisibility(View.VISIBLE);
                bar.setProgress((int) (pos * 1000 / a.durationMs));
            } else {
                bar.setVisibility(View.GONE);
            }
            return v;`,
   `            long pos = Prefs.getPosition(a.key);
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
            return v;`],
]);

// -------------------------------------------------------------- PlayerActivity
// l'écran reste allumé pendant la lecture
edit('PlayerActivity.java', [
  [`        Prefs.init(this);
        setContentView(R.layout.activity_player);`,
   `        Prefs.init(this);
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);`],
  ['import android.os.Bundle;', 'import android.os.Bundle;\nimport android.view.WindowManager;'],
]);
