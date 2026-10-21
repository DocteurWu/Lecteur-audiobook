package com.chapitres.podcast;

import android.net.Uri;

/** Un fichier audio (chapitre) et ses métadonnées. */
public class AudioItem {

    /** Identifiant stable (chemin absolu ou URI) utilisé pour la reprise de lecture. */
    public String key;
    /** Chemin absolu si le fichier est accessible directement, sinon null. */
    public String path;
    /** URI content:// (MediaStore ou SAF), sinon null. */
    public Uri uri;
    /** Nom de fichier d'origine, extension comprise. */
    public String fileName;
    /** Titre lisible, dérivé du nom de fichier. */
    public String title;
    /** Numéro de chapitre extrait du nom de fichier, -1 si absent. */
    public int index = -1;
    /** Durée en millisecondes. */
    public long durationMs;
    /** Taille en octets. */
    public long sizeBytes;
    /** Pochette du podcast (uniquement sur le premier chapitre). */
    public byte[] art;
    /** Groupe (podcast) auquel appartient ce chapitre. */
    public Podcast podcast;

    public String album;
    public String artist;

    /** Position de reprise en millisecondes. */
    public long resumeAt() {
        return Prefs.getPosition(key);
    }

    public boolean isDone() {
        return Prefs.isDone(key);
    }

    public String displayAlbum() {
        if (podcast != null && podcast.title != null && podcast.title.length() > 0) return podcast.title;
        return album != null ? album : "Podcast";
    }

    public String displayArtist() {
        if (artist != null && artist.length() > 0) return artist;
        return "";
    }
}
