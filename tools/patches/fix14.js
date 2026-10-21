// Patch : fond avec pochette dans le lecteur
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

edit('PlayerActivity.java', [
  ['    private ImageView imgArt;', '    private ImageView imgArt;\n    private ImageView imgBackdrop;'],
  ['        imgArt = findViewById(R.id.imgArt);', '        imgArt = findViewById(R.id.imgArt);\n        imgBackdrop = findViewById(R.id.imgBackdrop);'],
  [`        Bitmap art = null;
        if (item.podcast != null) art = ArtCache.get(item.podcast.id, item.podcast.art, 720);
        if (art != null) {`,
   `        Bitmap art = null;
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
        if (art != null) {`],
]);
