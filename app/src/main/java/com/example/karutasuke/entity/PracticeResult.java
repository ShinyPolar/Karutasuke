package com.example.karutasuke.entity;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The outcome of one practice session, shown on the result overlay. Immutable. */
public final class PracticeResult {

    private final PracticeType mode;
    private final long date;                    // System.currentTimeMillis() when it ended
    private final long duration;                // ms
    @Nullable private final Integer score;      // null for practices without a score (流し)
    private final List<Fuda> mistakes;
    private final List<FudaAttempt> attempts;   // empty for 暗記 and 定置

    public PracticeResult(PracticeType mode, long date, long duration, @Nullable Integer score,
                          List<Fuda> mistakes, List<FudaAttempt> attempts) {
        this.mode = mode;
        this.date = date;
        this.duration = duration;
        this.score = score;
        this.mistakes = Collections.unmodifiableList(new ArrayList<>(mistakes));
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
    }

    public PracticeType getMode() { return mode; }
    public long getDate() { return date; }
    public long getDuration() { return duration; }
    @Nullable public Integer getScore() { return score; }
    public List<Fuda> getMistakes() { return mistakes; }
    public List<FudaAttempt> getAttempts() { return attempts; }
}
