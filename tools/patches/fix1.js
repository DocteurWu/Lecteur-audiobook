// Corrections mécaniques de compilation (UTF-8 sûr)
const fs = require('fs');
const path = require('path');
const dir = process.argv[2];

function edit(file, pairs) {
  const p = path.join(dir, file);
  let s = fs.readFileSync(p, 'utf8');
  for (const [from, to] of pairs) {
    if (!s.includes(from)) {
      console.error('!! introuvable dans', file, ':', JSON.stringify(from.slice(0, 60)));
      continue;
    }
    s = s.split(from).join(to);
  }
  fs.writeFileSync(p, s, 'utf8');
  console.log('ok', file);
}

edit('AudioItem.java', [
  ['public boolean isFinished() {', 'public boolean isDone() {'],
]);

edit('EpisodesActivity.java', [
  ['import android.content.Intent;', 'import android.content.Intent;\nimport android.content.res.ColorStateList;'],
  ['imgCover.setTint(null);', 'imgCover.setImageTintList(null);'],
  ['item.isDone()', 'item.isDone()'],
]);

edit('MainActivity.java', [
  ['import android.content.Intent;', 'import android.content.Intent;\nimport android.content.res.ColorStateList;'],
  ['cover.setTint(null);', 'cover.setImageTintList(null);'],
  ['cover.setTint(getResources().getColor(R.color.subtext));',
   'cover.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.subtext)));'],
]);

edit('PlayerActivity.java', [
  ['import android.content.DialogInterface;', 'import android.content.DialogInterface;\nimport android.content.res.ColorStateList;'],
  ['imgArt.setTint(null);', 'imgArt.setImageTintList(null);'],
  ['imgArt.setTint(getResources().getColor(R.color.subtext));',
   'imgArt.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.subtext)));'],
]);

edit('MediaButtonReceiver.java', [
  ['AudioManager.ACTION_MEDIA_BUTTON', 'Intent.ACTION_MEDIA_BUTTON'],
]);

edit('PlaybackService.java', [
  ['notifyAll()', 'notifyListeners()'],
  ['private void notifyListeners()', 'private void notifyListeners()'],
  ['if (now - lastSave > 5000) {', 'if (now - lastSave > 2000) {'],
]);
