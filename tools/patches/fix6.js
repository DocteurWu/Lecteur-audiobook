// Remplacement ciblé de titleFromFileName (par index, pas par correspondance exacte)
const fs = require('fs');
const path = require('path');
const p = path.join(process.argv[2], 'Names.java');
let s = fs.readFileSync(p, 'utf8');

const start = s.indexOf('    public static String titleFromFileName(String fileName) {');
if (start < 0) { console.error('!! méthode introuvable'); process.exit(1); }

// trouve la fin de la méthode ( accolade fermante à l'indentation 4 )
let i = s.indexOf('{', start);
let depth = 0;
let end = -1;
for (let k = i; k < s.length; k++) {
  if (s[k] === '{') depth++;
  else if (s[k] === '}') { depth--; if (depth === 0) { end = k + 1; break; } }
}
if (end < 0) { console.error('!! fin de méthode introuvable'); process.exit(1); }

const replacement = `    public static String titleFromFileName(String fileName) {
        String n = baseName(fileName);

        // apostrophes élidées, avant de transformer les "_" en espaces
        n = n.replaceAll("(?<=[SLNDCJMQTU])_(?=[AEIOUY])", "’").replace('_', ' ');

        // 1. retire l'index de fichier ("12 - ")
        String s = n.replaceFirst("^\\\\s*\\\\d{1,4}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (s.length() >= 3) n = s;

        // 2. retire le numéro de chapitre redondant ("2 - ")
        s = n.replaceFirst("^\\\\s*\\\\d{1,3}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (s.length() >= 3) n = s;

        // 3. "Chapitre 1 - " devient "Chapitre 1 : "
        n = n.replaceFirst("^\\\\s*(Chapitre\\\\s+\\\\d+)\\\\s*[-.–—]\\\\s*", "$1 : ");

        n = n.replaceAll("\\\\s+", " ").trim();
        n = restoreFrench(n);
        return titleCase(n);
    }`;

s = s.slice(0, start) + replacement + s.slice(end);
fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java (titleFromFileName réécrit)');
