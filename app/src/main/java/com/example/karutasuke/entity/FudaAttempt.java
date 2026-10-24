package com.example.karutasuke.entity;

/** What happened during one reading in a practice. Immutable. */
public final class FudaAttempt {

    /** reactionTime value for readings with no reaction, e.g. a karafuda. */
    public static final long NO_REACTION = -1;

    private final Fuda fuda;            // the 札 that was read
    private final long reactionTime;    // ms from the 上の句 starting to the correct swipe
    private final int otetsukiCount;
    private final boolean karafuda;     // read, but not on the board

    public FudaAttempt(Fuda fuda, long reactionTime, int otetsukiCount, boolean karafuda) {
        this.fuda = fuda;
        this.reactionTime = reactionTime;
        this.otetsukiCount = otetsukiCount;
        this.karafuda = karafuda;
    }

    public Fuda getFuda() { return fuda; }
    public long getReactionTime() { return reactionTime; }
    public int getOtetsukiCount() { return otetsukiCount; }
    public boolean isKarafuda() { return karafuda; }
    public boolean hasReaction() { return reactionTime != NO_REACTION; }
}
