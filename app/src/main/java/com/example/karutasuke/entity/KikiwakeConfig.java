package com.example.karutasuke.entity;

/** Settings chosen in the 聞き分け setup step. Immutable. */
public final class KikiwakeConfig {

    /** The field holds 25 札 per 陣. */
    public static final int MAX_ON_FIELD = 2 * Layout.MAX_PER_JIN;

    private final FudaSet fudaSet;              // 札 for the field; if more than 50, 50 are picked at random
    private final ReadingStyle readingStyle;
    private final int karafudaCount;            // 0 = none; max = 100 − 札 on the field
    private final int memorizationSeconds;      // 暗記 time before reading starts; 0 = start at once

    public KikiwakeConfig(FudaSet fudaSet, ReadingStyle readingStyle, int karafudaCount, int memorizationSeconds) {
        int max = maxKarafuda(fudaSet);
        if (karafudaCount < 0 || karafudaCount > max) {
            throw new IllegalArgumentException("karafudaCount must be 0–" + max + ", was " + karafudaCount);
        }
        if (memorizationSeconds < 0) throw new IllegalArgumentException("memorizationSeconds must be >= 0");
        this.fudaSet = fudaSet;
        this.readingStyle = readingStyle;
        this.karafudaCount = karafudaCount;
        this.memorizationSeconds = memorizationSeconds;
    }

    /** How many 札 go on the field for this set. */
    public static int fieldSize(FudaSet set) { return Math.min(set.size(), MAX_ON_FIELD); }

    /** Karafuda can only come from 札 that aren't on the field. */
    public static int maxKarafuda(FudaSet set) { return 100 - fieldSize(set); }

    public FudaSet getFudaSet() { return fudaSet; }
    public ReadingStyle getReadingStyle() { return readingStyle; }
    public int getKarafudaCount() { return karafudaCount; }
    public int getMemorizationSeconds() { return memorizationSeconds; }
}
