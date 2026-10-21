// Patch final : élision sur lettre isolée, casse conservée sur « où/être », majuscule après « : »
const fs = require('fs');
const path = require('path');
const p = path.join(process.argv[2], 'Names.java');
let s = fs.readFileSync(p, 'utf8');

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

// 1. restoreElisions : élision seulement si la lettre est ISOLÉE ("S_IL" oui, "CONNAIS_TOI" non)
s = replaceMethod(s, '    private static String restoreElisions(String s)', `    private static String restoreElisions(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("QUELQU_", "QUELQU\\u2019")
                .replaceAll("QU_", "QU\\u2019")
                .replaceAll("(^|\\\\s)([LDNCJMSldncjms])_", "$1$2\\u2019");
    }`);

// 2. restoreAccents : conserve la casse initiale ("Où" reste "Où")
s = replaceMethod(s, '    private static String restoreAccents(String w)', `    private static String restoreAccents(String w) {
        String lw = w.toLowerCase(Locale.FRANCE);
        String res = null;
        if (lw.equals("ou")) res = "où";
        else if (lw.equals("ca")) res = "ça";
        else if (lw.equals("etre")) res = "être";
        else if (lw.equals("meme")) res = "même";
        else if (lw.equals("tres")) res = "très";
        else if (lw.equals("apres")) res = "après";
        else if (lw.equals("deja")) res = "déjà";
        else if (lw.equals("probleme")) res = "problème";
        else if (lw.equals("systeme")) res = "système";
        else if (lw.equals("etude")) res = "étude";
        else if (lw.equals("ere")) res = "ère";
        if (res == null) return w;
        if (w.length() > 0 && Character.isUpperCase(w.charAt(0))) {
            return Character.toUpperCase(res.charAt(0)) + res.substring(1);
        }
        return res;
    }`);

// 3. titleCase : le mot suivant un ":" isolé garde la majuscule
s = s.replace(`            if (w.isEmpty()) {
                out.append(punct).append(cap).append(' ');
                continue;
            }`,
`            if (w.isEmpty()) {
                out.append(punct).append(cap).append(' ');
                afterColon = afterColon || punct.indexOf(':') >= 0 || cap.indexOf(':') >= 0;
                continue;
            }`);

fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java');
