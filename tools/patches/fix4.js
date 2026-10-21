// Patch : titres de chapitres nettoyés + numéro de chapitre + tri naturel robuste
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

const OLD_TITLE = `    /** Nettoie un nom de fichier pour en faire un titre. */
    public static String titleFromFileName(String fileName) {
        String n = fileName;
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        n = n.replace('_', ' ');
        // Retire le préfixe numérique "12 - " ou "12. "
        String stripped = n.replaceFirst("^\\\\s*\\\\d{1,4}\\\\s*[-.)\\\\]]\\\\s*", "");
        if (stripped.length() > 2) n = stripped;
        n = n.replaceAll("\\\\s+", " ").trim();
        n = n.replace(" : ", " : ");
        return titleCase(n);
    }`;

const NEW_TITLE = `    /**
     * Nettoie un nom de fichier pour en faire un titre lisible.
     * "28 - 10 - AGE D'OR"      -> "Age D'or"
     * "01 - Chapitre 1 - Le nouvel ordre du jour humain" -> "Chapitre 1 : Le Nouvel Ordre du Jour Humain"
     */
    public static String titleFromFileName(String fileName) {
        String n = baseName(fileName).replace('_', ' ');

        // 1. retire l'index de fichier ("12 - ")
        String s = n.replaceFirst("^\\\\s*\\\\d{1,4}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (s.length() >= 3) n = s;

        // 2. retire le numéro de chapitre redondant ("2 - ")
        s = n.replaceFirst("^\\\\s*\\\\d{1,3}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (s.length() >= 3) n = s;

        // 3. "Chapitre 1 - " devient "Chapitre 1 : "
        n = n.replaceFirst("^\\\\s*(Chapitre\\\\s+\\\\d+)\\\\s*[-.–—]\\\\s*", "$1 : ");

        n = n.replaceAll("\\\\s+", " ").trim();
        return titleCase(n);
    }

    /** Nom de fichier sans extension. */
    private static String baseName(String fileName) {
        String n = fileName;
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        return n;
    }

    /**
     * Numéro de chapitre tel qu'il apparaît dans le livre (et non l'index de fichier) :
     * "28 - 10 - AGE D'OR" -> 10, "01 - Chapitre 1 - ..." -> 1, sinon -1.
     */
    public static int chapterNumber(String fileName) {
        String n = baseName(fileName);
        n = n.replaceFirst("^\\\\s*\\\\d{1,4}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\\\s*(?:Chapitre\\\\s+)?(\\\\d{1,3})\\\\b").matcher(n);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }`;

edit('Names.java', [
  [OLD_TITLE, NEW_TITLE],
  // tie-break du tri : comparaison par blocs de chiffres (2 avant 10)
  [`            int c = stripDigits(a).compareToIgnoreCase(stripDigits(b));
            if (c != 0) return c;
            return stripToDigits(a) - stripToDigits(b);`,
   `            return naturalCompare(a, b);`],
]);

// Ajout de naturalCompare dans Names.java
{
  const p = path.join(dir, 'Names.java');
  let s = fs.readFileSync(p, 'utf8');
  if (!s.includes('private static int naturalCompare')) {
    const helper = `
    /** Comparaison "naturelle" : les blocs de chiffres sont comparés numériquement. */
    private static int naturalCompare(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                String na = a.substring(si, i).replaceFirst("^0+(?=.)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=.)", "");
                if (na.length() != nb.length()) return na.length() - nb.length();
                int c = na.compareTo(nb);
                if (c != 0) return c;
            } else {
                if (ca != cb) {
                    int c = Character.toLowerCase(ca) - Character.toLowerCase(cb);
                    if (c != 0) return c;
                }
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }
`;
    s = s.replace('    /** Nettoie un nom de dossier pour l\'affichage', helper + '\n    /** Nettoie un nom de dossier pour l\'affichage');
    fs.writeFileSync(p, s, 'utf8');
    console.log('ok Names.java (naturalCompare)');
  }
}

// Numéro de chapitre dans la liste des chapitres
edit('EpisodesActivity.java', [
  ['            meta.setText(Names.duration(a.durationMs) + " · " + Names.size(a.sizeBytes));',
   `            String metaTxt = Names.duration(a.durationMs) + " · " + Names.size(a.sizeBytes);
            int chap = Names.chapterNumber(a.fileName);
            if (chap > 0) metaTxt += " · chap. " + chap;
            meta.setText(metaTxt);`],
]);
