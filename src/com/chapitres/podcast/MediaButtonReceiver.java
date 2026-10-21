package com.chapitres.podcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/** Reçoit les boutons multimédias du casque et de l'écran verrouillé. */
public class MediaButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        try {
            am.dispatchMediaKeyEvent(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
        } catch (Throwable ignored) {
        }
    }
}
