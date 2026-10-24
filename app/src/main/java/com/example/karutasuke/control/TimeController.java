package com.example.karutasuke.control;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.function.IntConsumer;

/** Countdown timer for the 暗記 period. Callbacks run on the main thread. */
public final class TimeController {

    private static final long TICK_MS = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tick;

    /**
     * @param onTick   called with the whole seconds left (rounded up), about 4 times a second
     * @param onFinish called once when time runs out (not called if stop() is called first)
     */
    public void startCountdown(int seconds, IntConsumer onTick, Runnable onFinish) {
        stop();
        long end = SystemClock.elapsedRealtime() + seconds * 1000L;
        tick = new Runnable() {
            @Override
            public void run() {
                long left = end - SystemClock.elapsedRealtime();
                if (left <= 0) {
                    tick = null;
                    onTick.accept(0);
                    onFinish.run();
                    return;
                }
                onTick.accept((int) ((left + 999) / 1000));
                handler.postDelayed(this, TICK_MS);
            }
        };
        tick.run();
    }

    public void stop() {
        if (tick != null) handler.removeCallbacks(tick);
        tick = null;
    }
}
