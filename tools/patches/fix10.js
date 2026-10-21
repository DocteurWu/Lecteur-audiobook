// Patch final titres : « la » capitalisé en début de titre, « A » contextuel (« à » vs « a »), majuscule après « : »
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

// 1. restoreAccents : plus de "la"/"des"/"du" en minuscules, plus de "a"->"à" inconditionnel
s = s.replace(`        if (lw.equals("a")) return "à";
`, '');
s = s.replace(`        if (lw.equals("des")) return "des";
        if (lw.equals("du")) return "du";
        if (lw.equals("la")) return "la";
`, '');

// 2. restoreFrench : règle contextuelle sur "A"
s = replaceMethod(s, '    public static String restoreFrench(String s)', `    public static String restoreFrench(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] tokens = s.split(" ", -1);
        for (int i = 0; i < tokens.length; i++) {
            // "A" seul : "à" s'il est préposition (à l'..., à Paris), sinon "a" (il a peur)
            if ("AÀ".equals(tokens[i]) || "aà".equals(tokens[i])) {
                String next = (i + 1 < tokens.length) ? tokens[i + 1] : "";
                String n = next.toUpperCase(Locale.FRANCE);
                boolean prep = n.startsWith("L\\u2019") || n.startsWith("L'")
                        || "AEIOUYÀÂÉÈÊÎÔÛ".indexOf(n.isEmpty() ? ' ' : n.charAt(0)) >= 0;
                tokens[i] = prep ? (tokens[i].equals("a") ? "à" : "à") : (tokens[i].equals("A") ? "a" : "a");
            }
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            out.append(restoreToken(tokens[i]));
            if (i < tokens.length - 1) out.append(' ');
        }
        return out.toString();
    }`);

// 3. titleCase : un mot après « : » garde sa majuscule
s = replaceMethod(s, '    public static String titleCase(String s)', `    public static String titleCase(String s) {
        if (s == null || s.isEmpty()) return "";
        String lower = s.toLowerCase(Locale.FRANCE);
        StringBuilder out = new StringBuilder();
        String[] words = lower.split(" ", -1);
        boolean afterColon = false;
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.isEmpty()) {
                out.append(' ');
                continue;
            }
            String punct = "";
            while (w.length() > 0 && !Character.isLetterOrDigit(w.charAt(0))) {
                punct += w.charAt(0);
                w = w.substring(1);
            }
            String cap = "";
            while (w.length() > 0 && !Character.isLetterOrDigit(w.charAt(w.length() - 1))) {
                cap = w.charAt(w.length() - 1) + cap;
                w = w.substring(0, w.length() - 1);
            }
            if (w.isEmpty()) {
                out.append(punct).append(cap).append(' ');
                continue;
            }
            String cased;
            if (i > 0 && isSmall(w) && !afterColon) {
                cased = w;
            } else {
                cased = Character.toUpperCase(w.charAt(0)) + w.substring(1);
            }
            // capitalise la lettre qui suit une apostrophe : "l'equation" -> "l'Équation"
            StringBuilder capBuf = new StringBuilder();
            for (int k = 0; k < cased.length(); k++) {
                char c = cased.charAt(k);
                if (k > 0 && (cased.charAt(k - 1) == '\\u2019' || cased.charAt(k - 1) == '\\'') && Character.isLetter(c)) {
                    c = Character.toUpperCase(c);
                }
                capBuf.append(c);
            }
            cased = capBuf.toString();
            cased = restoreAccents(cased);
            out.append(punct).append(cased).append(cap);
            afterColon = punct.indexOf(':') >= 0 || cap.indexOf(':') >= 0;
            if (i < words.length - 1) out.append(' ');
        }
        return out.toString().trim();
    }`);

fs.writeFileSync(p, s, 'utf8');
console.log('ok Names.java (titres finaux)');
