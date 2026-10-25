package com.example.karutasuke.entity;

/** How 流し shows each 札. */
public enum NagashiMode {
    /** The 札 stays until the user swipes it away. */
    MANUAL,
    /** The 札 is shown for displayDuration, then hidden until the user swipes (recognition drill). */
    RECOGNITION
}
