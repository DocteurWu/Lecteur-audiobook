// Patch : ordre correct — élisions d'abord (sur le nom brut), accents ensuite
const fs = require('fs');
const path = require('path');
const p = path.join(process.argv[2], 'Names.java');
let s = fs.readFileSync(p, 'utf8');

// ---------------------------------------------------------------- 1. méthode
function replaceMethod(src, signature, newBody) {
  const start = src.indexOf(signature);
  if (start < 0) { console.error('!! méthode introuvable :', signature); process.exit(1); }
  let i = src.indexOf('{', start);
  let depth = 0, end = -1;
  for (let k = i; k < src.length; k++) {
    if (src[k] === '{') depth++;
    else if (src[k] === '}') { depth--; if (depth === 0) { end = k + 1; break; } }
  }
  return src.slice(0, start) + newBody + src.slice(end);
}

s = replaceMethod(s, '    public static String titleFromFileName(String fileName)', `    public static String titleFromFileName(String fileName) {
        String n = baseName(fileName);

        // 1. apostrophes élidées, sur le nom brut (les "_" n'ont pas encore été transformés)
        n = restoreElisions(n).replace('_', ' ');

        // 2. retire l'index de fichier ("12 - ")
        String t = n.replaceFirst("^\\\\s*\\\\d{1,4}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (t.length() >= 3) n = t;

        // 3. retire le numéro de chapitre redondant ("2 - ")
        t = n.replaceFirst("^\\\\s*\\\\d{1,3}\\\\s*[-.–—)\\\\]]\\\\s*", "");
        if (t.length() >= 3) n = t;

        // 4. "Chapitre 1 - " devient "Chapitre 1 : "
        n = n.replaceFirst("^\\\\s*(Chapitre\\\\s+\\\\d+)\\\\s*[-.–—]\\\\s*", "$1 : ");

        n = n.replaceAll("\\\\s+", " ").trim();

        // 5. accents et ponctuation
        n = restoreFrench(n);
        return titleCase(n);
    }`);

// -------------------------------------------------------- 2. restoreFrench
s = replaceMethod(s, '    public static String restoreFrench(String s)', `    public static String restoreFrench(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        String[] tokens = s.split(" ", -1);
        for (int i = 0; i < tokens.length; i++) {
            out.append(restoreToken(tokens[i]));
            if (i < tokens.length - 1) out.append(' ');
        }
        return out.toString();
    }

    /**
     * Rétablit les apostrophes élidées dans un nom de fichier brut :
     * "L_ANTHROPOCENE" -> "L\\u2019ANTHROPOCENE", "QUELQU_UN" -> "QUELQU\\u2019UN".
     * On exclut T et U : "PLAIT_Y_A_T_IL" n'est pas une élision.
     */
    private static String restoreElisions(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("QUELQU_", "QUELQU\\u2019")
                .replaceAll("QU_", "QU\\u2019")
                .replaceAll("(?<=[SLNDCJMslnDCJm])_(?=[AEIOUYaeiouy])", "\\u2019");
    }`);

// ------------------------------------------------------- 3. restoreToken
s = replaceMethod(s, '    private static String restoreToken(String token)', `    private static String restoreToken(String token) {
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
            // une élision en tête ("L\\u2019EQUATION") ne fait pas partie du mot à traduire
            String prefix = "";
            String body = core;
            if (body.length() > 2 && body.charAt(1) == '\\u2019' && "LSNDCJMQ".indexOf(body.charAt(0)) >= 0) {
                prefix = body.substring(0, 2);
                body = body.substring(2);
            }
            String up = body.toUpperCase(Locale.FRANCE);
            String acc = ACCENTS.get(up);
            sb.append(prefix);
            if (acc != null) {
                boolean allUpper = body.equals(up);
                sb.append(allUpper ? acc : acc.toLowerCase(Locale.FRANCE));
            } else {
                sb.append(body);
            }
            sb.append(tail);
        }
        return sb.toString();
    }`);

fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java (ordre élisions/accentuation corrigé)');
