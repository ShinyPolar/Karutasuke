package com.example.karutasuke.control;

/**
 * Plays reading audio. An interface so KikiwakeController can be tested without Android.
 * Only one clip plays at a time: play() stops whatever was playing before.
 */
public interface ReadingPlayer {

    /**
     * Starts playing an asset (e.g. "audio/kaminoku/001.mp3"). onDone runs on the main thread
     * when the clip ends. If the file doesn't exist, returns false and onDone still runs (soon,
     * not immediately), so a missing file never stalls a practice.
     */
    boolean play(String assetPath, Runnable onDone);

    /** Stops playback. The pending onDone will not run. */
    void stop();

    void pause();

    void resume();

    /** Frees the player. Call when the screen is destroyed. */
    void release();
}
