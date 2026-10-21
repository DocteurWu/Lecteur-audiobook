package com.chapitres.podcast;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;

/** Utilitaires de texte : titres lisibles, durées, tri naturel. */
public class Names {

    private static final String[] SMALL_WORDS = {
            "de", "du", "des", "la", "le", "les", "et", "a", "au", "aux", "en", "pour", "dans",
            "sur", "que", "qui", "un", "une", "sur", "avec", "sans", "chez", "par", "d", "l", "n"
    };

    private static boolean isSmall(String w) {
        for (String s : SMALL_WORDS) {
            if (s.equals(w)) return true;
        }
        return false;
    }

    /** "12:34" ou "1:02:03". */
    public static String clock(long ms) {
        if (ms < 0) ms = 0;
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    /** Durée lisible : "14 min", "2 h 30", "45 s". */
    public static String duration(long ms) {
        if (ms <= 0) return "—";
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        if (h > 0) return h + " h " + (m > 0 ? String.format(Locale.US, "%02d", m) : "00");
        if (m > 0) return m + " min";
        return total + " s";
    }

    /** Taille de fichier lisible. */
    public static String size(long bytes) {
        if (bytes <= 0) return "";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 100) return Math.round(mb) + " Mo";
        if (mb >= 1) return String.format(Locale.US, "%.1f Mo", mb);
        return Math.round(bytes / 1024.0) + " Ko";
    }

    /**
     * Nettoie un nom de fichier pour en faire un titre lisible.
     * "28 - 10 - AGE D'OR"      -> "Age D'or"
     * "01 - Chapitre 1 - Le nouvel ordre du jour humain" -> "Chapitre 1 : Le Nouvel Ordre du Jour Humain"
     */
    public static String titleFromFileName(String fileName) {
        String n = baseName(fileName);

        // 1. apostrophes élidées, sur le nom brut (les "_" n'ont pas encore été transformés)
        n = restoreElisions(n).replace('_', ' ');

        // 2. retire l'index de fichier ("12 - ")
        String t = n.replaceFirst("^\\s*\\d{1,4}\\s*[-.–—)\\]]\\s*", "");
        if (t.length() >= 3) n = t;

        // 3. retire le numéro de chapitre redondant ("2 - ")
        t = n.replaceFirst("^\\s*\\d{1,3}\\s*[-.–—)\\]]\\s*", "");
        if (t.length() >= 3) n = t;

        // 4. "Chapitre 1 - " devient "Chapitre 1 : "
        n = n.replaceFirst("^\\s*(Chapitre\\s+\\d+)\\s*[-.–—]\\s*", "$1 : ");

        n = n.replaceAll("\\s+", " ").trim();

        // 5. accents et ponctuation
        n = restoreFrench(n);
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
        n = n.replaceFirst("^\\s*\\d{1,4}\\s*[-.–—)\\]]\\s*", "");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s*(?:Chapitre\\s+)?(\\d{1,3})\\b").matcher(n);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    /** Numéro de chapitre au début du nom de fichier, -1 sinon. */
    public static int indexFromFileName(String fileName) {
        String n = fileName;
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*(\\d{1,4})").matcher(n);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    /** Passe une chaîne en "Title Case" français (les mots courts restent en minuscules). */
    public static String titleCase(String s) {
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
                afterColon = afterColon || punct.indexOf(':') >= 0 || cap.indexOf(':') >= 0;
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
                if (k > 0 && (cased.charAt(k - 1) == '\u2019' || cased.charAt(k - 1) == '\'') && Character.isLetter(c)) {
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
    }

    /** Corrige quelques accents perdus dans les noms de fichiers. */
    private static String restoreAccents(String w) {
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
    }

    /**
     * Compare deux noms de fichiers de façon "naturelle".
     * Le numero de fichier ("66 - 2 - ABREGE...") prime : c'est lui qui donne
     * l'ordre des chapitres, pas l'alphabet.
     */
    public static final Comparator<String> NATURAL = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            int na = leadingNumber(a);
            int nb = leadingNumber(b);
            if (na != nb) return Integer.compare(na, nb);
            return naturalCompare(a, b);
        }
    };

    /** Premier nombre du nom de fichier, Integer.MAX_VALUE si absent. */
    private static int leadingNumber(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*(\\d{1,4})").matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Nettoie un tag ID3 brut : "Philippe_Sollier" -> "Philippe Sollier". */
    public static String cleanTag(String s) {
        if (s == null) return null;
        String n = s.replace('_', ' ').replaceAll("\\s+", " ").trim();
        return n.isEmpty() ? null : n;
    }

    private static String stripDigits(String s) {
        return s.replaceAll("\\d+", "");
    }

    private static int stripToDigits(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        int v = 0;
        if (m.find()) {
            try {
                v = Integer.parseInt(m.group());
            } catch (Exception ignored) {
                v = 0;
            }
        }
        return v;
    }


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

    /**
     * Les noms de fichiers de ce genre sont en majuscules SANS accents :
     * on les remet dans la bonne orthographe. Dictionnaire limité aux mots
     * courants, appliqué mot par mot.
     */
    private static final java.util.Map<String, String> ACCENTS = new java.util.HashMap<>();

    static {
        String[][] d = {
                {"ABREGE", "ABRÈGE"}, {"AGE", "ÂGE"}, {"AGES", "ÂGES"}, {"APRES", "APRÈS"},
                {"BREVE", "BRÈVE"}, {"CA", "ÇA"}, {"COTE", "CÔTÉ"}, {"COTES", "CÔTES"},
                {"CREATION", "CRÉATION"}, {"CREER", "CRÉER"}, {"DECOUPLAGE", "DÉCOUPLAGE"},
                {"DEJA", "DÉJÀ"}, {"DEMON", "DÉMON"}, {"DEMONS", "DÉMONS"}, {"DEPART", "DÉPART"},
                {"DESIR", "DÉSIR"}, {"DEUXIEME", "DEUXIÈME"}, {"DIFFERENT", "DIFFÉRENT"},
                {"DIFFERENTS", "DIFFÉRENTS"}, {"ECLIPSE", "ÉCLIPSE"}, {"ECRIRE", "ÉCRIRE"},
                {"ECRIT", "ÉCRIT"}, {"ECRITURE", "ÉCRITURE"}, {"ECRITURES", "ÉCRITURES"},
                {"ELECTRICITE", "ÉLECTRICITÉ"}, {"ELEMENTS", "ÉLÉMENTS"}, {"ENERGIE", "ÉNERGIE"},
                {"ENQUETE", "ENQUÊTE"}, {"ENTREE", "ENTRÉE"}, {"EPOPEE", "ÉPOPÉE"},
                {"EQUATION", "ÉQUATION"}, {"EQUILIBRE", "ÉQUILIBRE"}, {"ERE", "ÈRE"},
                {"ETINCELLE", "ÉTINCELLE"}, {"ETUDE", "ÉTUDE"}, {"EVENEMENT", "ÉVÉNEMENT"},
                {"EVOLUTION", "ÉVOLUTION"}, {"EXPERIENCE", "EXPÉRIENCE"}, {"EXTREME", "EXTRÊME"},
                {"FEMININ", "FÉMININ"}, {"GENETIQUE", "GÉNÉTIQUE"}, {"GENIE", "GÉNIE"},
                {"HOPITAL", "HÔPITAL"}, {"HUMANITE", "HUMANITÉ"}, {"IDEAL", "IDÉAL"},
                {"INEGALITE", "INÉGALITÉ"}, {"INSTITUTION", "INSTITUTION"}, {"INTERET", "INTÉRÊT"},
                {"INTERPRETATION", "INTERPRÉTATION"}, {"LABORATOIRE", "LABORATOIRE"},
                {"LEGITIME", "LÉGITIME"}, {"LIBERTE", "LIBERTÉ"}, {"MEME", "MÊME"},
                {"MEMOIRE", "MÉMOIRE"}, {"METAMORPHOSE", "MÉTAMORPHOSE"}, {"METHODE", "MÉTHODE"},
                {"MODELES", "MODÈLES"}, {"MORTALITE", "MORTALITÉ"}, {"MYSTERE", "MYSTÈRE"},
                {"NECESSAIRE", "NÉCESSAIRE"}, {"NUMERIQUE", "NUMÉRIQUE"}, {"OCEAN", "OCÉAN"},
                {"PERIL", "PÉRIL"}, {"PERIODE", "PÉRIODE"}, {"PIECE", "PIÈCE"},
                {"PIEGE", "PIÈGE"}, {"PLANETE", "PLANÈTE"}, {"PLAIT", "PLAÎT"},
                {"PAUVRETE", "POUVRETÉ"}, {"ANTHROPOCENE", "ANTHROPOCÈNE"}, {"DEPARAILLE", "DÉPARÉILLÉ"}, {"DONNEES", "DONNÉES"}, {"GATEAU", "GÂTEAU"}, {"TELECHARGER", "TÉLÉCHARGER"}, {"SORCIERES", "SORCIÈRES"}, {"CONNAIS", "CONNAIS"}, {"PREFERENCE", "PRÉFÉRENCE"}, {"PREMIERE", "PREMIÈRE"},
                {"PREPARE", "PRÉPARÉ"}, {"PRES", "PRÈS"}, {"PRESENCE", "PRÉSENCE"},
                {"PRESENT", "PRÉSENT"}, {"PROBLEME", "PROBLÈME"}, {"PROBLEMES", "PROBLÈMES"},
                {"PROBABILITE", "PROBABILITÉ"}, {"PROPRETE", "PROPRETÉ"}, {"PROTECTION", "PROTECTION"},
                {"REALITE", "RÉALITÉ"}, {"REALISER", "RÉALISER"}, {"REFERENCE", "RÉFÉRENCE"},
                {"REGIME", "RÉGIME"}, {"REGION", "RÉGION"}, {"REPARTITION", "RÉPARTITION"},
                {"REPETITION", "RÉPÉTITION"}, {"REPRESENTE", "REPRÉSENTE"},
                {"RESISTANCE", "RÉSISTANCE"}, {"RESOLUTION", "RÉSOLUTION"},
                {"REVELATION", "RÉVÉLATION"}, {"REVOLUTION", "RÉVOLUTION"},
                {"REVOLUTIONS", "RÉVOLUTIONS"}, {"SACRE", "SACRÉ"}, {"SECURITE", "SÉCURITÉ"},
                {"PASSE", "PASSÉ"}, {"POSE", "POSÉ"}, {"PENSE", "PENSÉ"}, {"SENS", "SENS"},
                {"SEPARER", "SÉPARER"}, {"SERIEUX", "SÉRIEUX"}, {"SIECLE", "SIÈCLE"},
                {"SOCIETE", "SOCIÉTÉ"}, {"SOEUR", "SŒUR"}, {"SOUFFRANCE", "SOUFFRANCE"},
                {"SPECIFIQUE", "SPÉCIFIQUE"}, {"STRATEGIE", "STRATÉGIE"}, {"SUCCES", "SUCCÈS"},
                {"SYSTEME", "SYSTÈME"}, {"THEORIE", "THÉORIE"}, {"TROISIEME", "TROISIÈME"},
                {"UNITE", "UNITÉ"}, {"VAUT-IL", "VAUT-IL"}, {"VERITE", "VÉRITÉ"},
                {"VITALITE", "VITALITÉ"}, {"ECONOMIE", "ÉCONOMIE"}, {"EDUCATION", "ÉDUCATION"},
                {"MEDECINE", "MÉDECINE"}, {"PREHISTOIRE", "PRÉHISTOIRE"},
                {"PREHISTORIQUE", "PRÉHISTORIQUE"}, {"NEOLITHIQUE", "NÉOLITHIQUE"},
                {"PALEO", "PALÉO"}, {"PRET", "PRÊT"}, {"REPERE", "REPÈRE"}, {"TETE", "TÊTE"},
                {"ETERNEL", "ÉTERNEL"}, {"ETERNITE", "ÉTERNITÉ"}, {"IMMORTALITE", "IMMORTALITÉ"},
                {"MOUSTACHE", "MOUSTACHE"}, {"FRERES", "FRÈRES"}, {"PERE", "PÈRE"},
                {"MERE", "MÈRE"}, {"VOLE", "VOLÉ"}, {"VOLEUR", "VOLEUR"}, {"COEUR", "CŒUR"}
        };
        for (String[] e : d) ACCENTS.put(e[0], e[1]);
    }

    /**
     * Rétablit les accents et les apostrophes d'un texte capitalisé sans accents :
     * "S_IL VOUS PLAIT_Y_A_T_IL" -> "S'il vous plaît, y a-t-il"
     * "PAUVRETE BIOLOGIQUE"      -> "Pauvreté Biologique"
     */
    public static String restoreFrench(String s) {
        if (s == null || s.isEmpty()) return "";
        String[] tokens = s.split(" ", -1);
        for (int i = 0; i < tokens.length; i++) {
            // "A" seul : "à" s'il est préposition (à l'..., à Paris), sinon "a" (il a peur)
            if ("AÀ".equals(tokens[i]) || "aà".equals(tokens[i])) {
                String next = (i + 1 < tokens.length) ? tokens[i + 1] : "";
                String n = next.toUpperCase(Locale.FRANCE);
                boolean prep = n.startsWith("L\u2019") || n.startsWith("L'")
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
    }

    /**
     * Rétablit les apostrophes élidées dans un nom de fichier brut :
     * "L_ANTHROPOCENE" -> "L\u2019ANTHROPOCENE", "QUELQU_UN" -> "QUELQU\u2019UN".
     * On exclut T et U : "PLAIT_Y_A_T_IL" n'est pas une élision.
     */
    private static String restoreElisions(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("QUELQU_", "QUELQU\u2019")
                .replaceAll("QU_", "QU\u2019")
                .replaceAll("(^|\\s)([LDNCJMSldncjms])_", "$1$2\u2019");
    }

    private static String restoreToken(String token) {
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
            // une élision en tête ("L\u2019EQUATION") ne fait pas partie du mot à traduire
            String prefix = "";
            String body = core;
            if (body.length() > 2 && body.charAt(1) == '\u2019' && "LSNDCJMQ".indexOf(body.charAt(0)) >= 0) {
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
    }

    /** Nettoie un nom de dossier pour l'affichage : retire "[mp3 64kbps]" etc. */
    public static String cleanFolderName(String name) {
        String n = name.replaceAll("\\[[^\\]]*\\]", "").replaceAll("\\([^)]*\\)", " ").trim();
        n = n.replaceAll("\\s+", " ");
        return n.isEmpty() ? name : n;
    }

    /** Minuscules sans accents, pour la recherche. */
    public static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.FRANCE), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}
