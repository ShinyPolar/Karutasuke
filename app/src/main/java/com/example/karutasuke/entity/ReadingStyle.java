package com.example.karutasuke.entity;

/** How 聞き分け reads each 札. */
public enum ReadingStyle {
    /** Only the 上の句 of each 札. Faster drilling. */
    KAMINOKU_ONLY,
    /** Like a real match: 序歌 first, then each 札's 下の句 before the next 上の句. */
    MATCH_STYLE
}
