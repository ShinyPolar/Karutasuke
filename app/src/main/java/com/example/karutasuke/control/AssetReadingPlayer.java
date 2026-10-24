package com.example.karutasuke.control;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

/** Plays reading audio from app/src/main/assets with MediaPlayer. */
public final class AssetReadingPlayer implements ReadingPlayer {

    private static final String TAG = "AssetReadingPlayer";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Runnable pendingDone;   // the onDone of the clip currently playing

    public AssetReadingPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean play(String assetPath, Runnable onDone) {
        stop();
        pendingDone = onDone;

        AssetFileDescriptor fd;
        try {
            fd = context.getAssets().openFd(assetPath);
        } catch (IOException e) {
            Log.w(TAG, "Missing audio: " + assetPath);
            mainHandler.post(() -> finishClip(onDone));
            return false;
        }

        MediaPlayer mp = new MediaPlayer();
        player = mp;
        try (AssetFileDescriptor afd = fd) {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setOnCompletionListener(m -> finishClip(onDone));
            mp.setOnErrorListener((m, what, extra) -> {
                Log.w(TAG, "Playback error " + what + "/" + extra + " for " + assetPath);
                finishClip(onDone);
                return true;
            });
            mp.prepare();   // local file, so this is quick
            mp.start();
            return true;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Could not play " + assetPath, e);
            mainHandler.post(() -> finishClip(onDone));
            return false;
        }
    }

    /** Runs onDone only if it still belongs to the current clip (not stopped or replaced). */
    private void finishClip(Runnable onDone) {
        if (pendingDone != onDone) return;
        pendingDone = null;
        releasePlayer();
        onDone.run();
    }

    @Override
    public void stop() {
        pendingDone = null;
        releasePlayer();
    }

    @Override
    public void pause() {
        if (player != null && player.isPlaying()) player.pause();
    }

    @Override
    public void resume() {
        if (player != null && pendingDone != null && !player.isPlaying()) player.start();
    }

    @Override
    public void release() {
        stop();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
