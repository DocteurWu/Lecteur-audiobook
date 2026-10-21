// Patch : capitalisation après l'apostrophe ("L’équation" -> "L’Équation")
const fs = require('fs');
const path = require('path');
const p = path.join(process.argv[2], 'Names.java');
let s = fs.readFileSync(p, 'utf8');

const ANCIEN = `            String cased;
            if (i > 0 && isSmall(w)) {
                cased = w;
            } else {
                cased = Character.toUpperCase(w.charAt(0)) + w.substring(1);
            }
            cased = restoreAccents(cased);`;

const NOUVEAU = `            String cased;
            if (i > 0 && isSmall(w)) {
                cased = w;
            } else {
                cased = Character.toUpperCase(w.charAt(0)) + w.substring(1);
            }
            // capitalise la lettre qui suit une apostrophe : "l'equation" -> "l'Équation"
            StringBuilder cap = new StringBuilder();
            for (int k = 0; k < cased.length(); k++) {
                char c = cased.charAt(k);
                if (k > 0 && (cased.charAt(k - 1) == '\\u2019' || cased.charAt(k - 1) == '\\'') && Character.isLetter(c)) {
                    c = Character.toUpperCase(c);
                }
                cap.append(c);
            }
            cased = cap.toString();
            cased = restoreAccents(cased);`;

if (!s.includes(ANCIEN)) { console.error('!! bloc titleCase introuvable'); process.exit(1); }
s = s.replace(ANCIEN, NOUVEAU);
fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java (capitalisation après apostrophe)');
