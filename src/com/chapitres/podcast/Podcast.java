package com.chapitres.podcast;

import java.util.ArrayList;
import java.util.List;

/** Un podcast = un dossier (ou un album) contenant plusieurs chapitres. */
public class Podcast {

    public String id;
    public String title;
    public String artist;
    public String folder;
    public byte[] art;
    public final List<AudioItem> episodes = new ArrayList<>();
    public long totalDurationMs;

    public int episodeCount() {
        return episodes.size();
    }
}
