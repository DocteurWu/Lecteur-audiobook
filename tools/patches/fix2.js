const fs = require('fs');
const path = require('path');
const dir = process.argv[2];

function edit(file, pairs) {
  const p = path.join(dir, file);
  let s = fs.readFileSync(p, 'utf8');
  for (const [from, to] of pairs) {
    if (!s.includes(from)) { console.error('!! introuvable', file, JSON.stringify(from.slice(0, 50))); continue; }
    s = s.split(from).join(to);
  }
  fs.writeFileSync(p, s, 'utf8');
  console.log('ok', file);
}

edit('Library.java', [
  ['    private static void scanTree(Context ctx, Uri tree, String docId, List<Entry> entries, Set<String> seen, int depth) {\n        if (depth > 6 || docId == null) return;',
   '    private static void scanTree(Context ctx, Uri tree, String docId, List<Entry> entries, Set<String> seen, int depth) {\n        if (depth > 6 || docId == null) return;\n        final String treeId = DocumentsContract.getTreeDocumentId(tree);\n        if (treeId == null) return;'],
]);

edit('MainActivity.java', [
  ['import android.app.PopupMenu;', 'import android.widget.PopupMenu;'],
  ['final boolean done = item.isFinished();', 'final boolean done = item.isDone();'],
]);
