// Patch : accents/apostrophes français dans les titres + persistance synchrone de la position
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

// ---------------------------------------------------------------- Names.java

const ACCENTS = `    /**
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
                {"POUVRETE", "POUVRETÉ"}, {"PREFERENCE", "PRÉFÉRENCE"}, {"PREMIERE", "PREMIÈRE"},
                {"PREPARE", "PRÉPARÉ"}, {"PRES", "PRÈS"}, {"PRESENCE", "PRÉSENCE"},
                {"PRESENT", "PRÉSENT"}, {"PROBLEME", "PROBLÈME"}, {"PROBLEMES", "PROBLÈMES"},
                {"PROBABILITE", "PROBABILITÉ"}, {"PROPRETE", "PROPRETÉ"}, {"PROTECTION", "PROTECTION"},
                {"REALITE", "RÉALITÉ"}, {"REALISER", "RÉALISER"}, {"REFERENCE", "RÉFÉRENCE"},
                {"REGIME", "RÉGIME"}, {"REGION", "RÉGION"}, {"REPARTITION", "RÉPARTITION"},
                {"REPETITION", "RÉPÉTITION"}, {"REPRESENTE", "REPRÉSENTE"},
                {"RESISTANCE", "RÉSISTANCE"}, {"RESOLUTION", "RÉSOLUTION"},
                {"REVELATION", "RÉVÉLATION"}, {"REVOLUTION", "RÉVOLUTION"},
                {"REVOLUTIONS", "RÉVOLUTIONS"}, {"SACRE", "SACRÉ"}, {"SECURITE", "SÉCURITÉ"},
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
        // apostrophes élidées : L_ANTHROPOCENE -> L'ANTHROPOCENE
        String t = s.replaceAll("(?<=[SLNDCJMQTU])_(?=[AEIOUY])", "’");
        StringBuilder out = new StringBuilder();
        String[] tokens = t.split(" ", -1);
        for (int i = 0; i < tokens.length; i++) {
            out.append(restoreToken(tokens[i]));
            if (i < tokens.length - 1) out.append(' ');
        }
        return out.toString();
    }

    private static String restoreToken(String token) {
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
    }
`;

const OLD_TITLE = `    /**
     * Nettoie un nom de fichier pour en faire un titre lisible.
     * "28 - 10 - AGE D'OR"      -> "Age D'or"
     * "01 - Chapitre 1 - Le nouvel ordre du jour humain" -> "Chapitre 1 : Le Nouvel Ordre Du Jour Humain"
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
    }`;

const NEW_TITLE = `    /**
     * Nettoie un nom de fichier pour en faire un titre lisible.
     * "28 - 10 - AGE D'OR"      -> "Age D'or"
     * "01 - Chapitre 1 - Le nouvel ordre du jour humain" -> "Chapitre 1 : Le Nouvel Ordre du Jour Humain"
     */
    public static String titleFromFileName(String fileName) {
        String n = baseName(fileName);

        // apostrophes élidées avant de transformer les "_" en espaces
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

edit('Names.java', [
  [OLD_TITLE, NEW_TITLE],
  ['    /** Nettoie un nom de dossier pour l\'affichage', ACCENTS + '\n    /** Nettoie un nom de dossier pour l\'affichage'],
]);

// ------------------------------------------------------------- Prefs.java
// apply() est asynchrone : une fermeture brutale du processus perd la position.
// On commit() pour garantir la reprise.
edit('Prefs.java', [
  [`        if (ms <= 1500) {
            sp.edit().remove(K_POS + key).apply();
        } else {
            sp.edit().putLong(K_POS + key, ms).apply();
        }`,
   `        if (ms <= 1500) {
            sp.edit().remove(K_POS + key).commit();
        } else {
            sp.edit().putLong(K_POS + key, ms).commit();
        }`],
  [`    public static void setLastKey(String key) {
        if (sp == null) return;
        sp.edit().putString(K_LAST, key).apply();
    }`,
   `    public static void setLastKey(String key) {
        if (sp == null) return;
        sp.edit().putString(K_LAST, key).commit();
    }`],
]);
