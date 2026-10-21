// Patch : dictionnaire d'accents complet, ponctuation finale, apostrophes élidées (casse-insensible)
const fs = require('fs');
const path = require('path');
const p = path.join(process.argv[2], 'Names.java');
let s = fs.readFileSync(p, 'utf8');

// 1. entrées manquantes / corrigées
const AJOUTS = [
  ['{"POUVRETE", "POUVRETÉ"}', '{"PAUVRETE", "POUVRETÉ"}, {"ANTHROPOCENE", "ANTHROPOCÈNE"}, {"DEPARAILLE", "DÉPARÉILLÉ"}, {"DONNEES", "DONNÉES"}, {"GATEAU", "GÂTEAU"}, {"TELECHARGER", "TÉLÉCHARGER"}, {"SORCIERES", "SORCIÈRES"}, {"CONNAIS", "CONNAIS"}'],
];
for (const [from, to] of AJOUTS) {
  if (s.includes(from)) { s = s.replace(from, to); console.log('ok remplace', from.slice(0, 20)); }
  else console.error('!! absent', from);
}

// 2. règle des apostrophes : insensible à la casse, on évite T_ et U_
const OLD_APO = `        // apostrophes élidées : L_ANTHROPOCENE -> L'ANTHROPOCENE
        String t = s.replaceAll("(?<=[SLNDCJMQTU])_(?=[AEIOUY])", "’");`;
const NEW_APO = `        // apostrophes élidées : L_ANTHROPOCENE -> L'ANTHROPOCENE
        // (on exclut T et U : "PLAIT_Y_A_T_IL" n'est pas une élision)
        String t = s.replaceAll("QUELQU_", "QUELQU\\u2019").replaceAll("QU_", "QU\\u2019")
                .replaceAll("(?<=[SLNDCJMslnDCJm])_(?=[AEIOUYaeiouy])", "\\u2019");`;
if (!s.includes(OLD_APO)) { console.error('!! règle apostrophes introuvable'); process.exit(1); }
s = s.replace(OLD_APO, NEW_APO);

// 3. restoreToken : tenir compte de la ponctuation finale
const OLD_TOK = `    private static String restoreToken(String token) {
        StringBuilder sb = new StringBuilder();
        String[] parts = token.split("-", -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) sb.append('-');
            if (part.isEmpty()) continue;
            String up = part.toUpperCase(Locale.FRANCE);
            String acc = ACCENTS.get(up);
            if (acc != null) {
                boolean allUpper = part.equals(up);
                sb.append(allUpper ? acc : acc.toLowerCase(Locale.FRANCE));
            } else {
                sb.append(part);
            }
        }
        return sb.toString();
    }`;
const NEW_TOK = `    private static String restoreToken(String token) {
        StringBuilder sb = new StringBuilder();
        String[] parts = token.split("-", -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) sb.append('-');
            if (part.isEmpty()) continue;
            // on isole la ponctuation finale ("REVOLUTION?" -> "REVOLUTION" + "?")
            int k = part.length();
            while (k > 0 && !Character.isLetterOrDigit(part.charAt(k - 1))) k--;
            String core = part.substring(0, k);
            String tail = part.substring(k);
            if (core.isEmpty()) {
                sb.append(part);
                continue;
            }
            String up = core.toUpperCase(Locale.FRANCE);
            String acc = ACCENTS.get(up);
            if (acc != null) {
                boolean allUpper = core.equals(up);
                sb.append(allUpper ? acc : acc.toLowerCase(Locale.FRANCE));
            } else {
                sb.append(core);
            }
            sb.append(tail);
        }
        return sb.toString();
    }`;
if (!s.includes(OLD_TOK)) { console.error('!! restoreToken introuvable'); process.exit(1); }
s = s.replace(OLD_TOK, NEW_TOK);

fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java');
