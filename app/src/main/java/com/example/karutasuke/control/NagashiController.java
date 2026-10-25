package com.example.karutasuke.control;

import androidx.annotation.Nullable;

import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaAttempt;
import com.example.karutasuke.entity.NagashiConfig;
import com.example.karutasuke.entity.PracticeResult;
import com.example.karutasuke.entity.PracticeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * 流し (FR-01 – FR-05). Shows the 札 of a set one at a time in random order; each swipe
 * moves to the next. Records how long each 札 was on screen before it was swiped.
 *
 * Timing of RECOGNITION mode (hiding the 札) is left to the screen, so this class has no
 * Android dependencies and can be tested on its own.
 */
public final class NagashiController {

    public interface Listener {
        /** Show this 札, upside down if upsideDown (an 敵陣 札). index is 0-based. */
        void onShow(Fuda fuda, boolean upsideDown, int index, int total);

        void onFinished(PracticeResult result);
    }

    private final Listener listener;
    private final Random random;
    private final LongSupplier clock;

    private NagashiConfig config;
    private final List<Fuda> order = new ArrayList<>();
    private final List<FudaAttempt> attempts = new ArrayList<>();
    private int index;
    private long shownAt;
    private long startTime;
    private boolean finished = true;
    private PracticeResult result;

    public NagashiController(Listener listener, Random random, LongSupplier clock) {
        this.listener = listener;
        this.random = random;
        this.clock = clock;
    }

    /** Shuffles the set and shows the first 札. */
    public void start(NagashiConfig config) {
        this.config = config;
        order.clear();
        order.addAll(config.getFudaSet().getFuda());
        Collections.shuffle(order, random);
        attempts.clear();
        index = 0;
        result = null;
        finished = false;
        startTime = clock.getAsLong();
        show();
    }

    /**
     * The user swiped the current 札 away. Shows the next one, or finishes after the last.
     * @return the 札 that was swiped (so the screen can show its 決まり字), or null if finished
     */
    @Nullable
    public Fuda onSwipeFuda() {
        if (finished) return null;
        Fuda swiped = order.get(index);
        attempts.add(new FudaAttempt(swiped, clock.getAsLong() - shownAt, 0, false));
        index++;
        if (index >= order.size()) finish();
        else show();
        return swiped;
    }

    /** Ends now (also called automatically after the last 札). */
    public PracticeResult finish() {
        if (finished && result != null) return result;
        finished = true;
        result = new PracticeResult(PracticeType.NAGASHI, System.currentTimeMillis(),
                clock.getAsLong() - startTime, null, Collections.emptyList(), attempts);
        listener.onFinished(result);
        return result;
    }

    public boolean isFinished() { return finished; }

    private void show() {
        shownAt = clock.getAsLong();
        boolean upsideDown = config.isMixUpsideDown() && random.nextBoolean();   // 50/50 自陣 or 敵陣
        listener.onShow(order.get(index), upsideDown, index, order.size());
    }
}
