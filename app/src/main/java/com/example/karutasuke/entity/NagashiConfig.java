package com.example.karutasuke.entity;

/** Settings chosen in the 流し setup step. Immutable. */
public final class NagashiConfig {

    private final FudaSet fudaSet;
    private final NagashiMode mode;
    private final int displayDurationMs;    // RECOGNITION only
    private final boolean showKimariji;     // briefly show the 決まり字 of each 札 after it is swiped
    private final boolean mixUpsideDown;    // randomly show 札 upside down, like the 敵陣

    public NagashiConfig(FudaSet fudaSet, NagashiMode mode, int displayDurationMs, boolean showKimariji,
                         boolean mixUpsideDown) {
        if (fudaSet.size() == 0) throw new IllegalArgumentException("fudaSet is empty");
        if (displayDurationMs <= 0) throw new IllegalArgumentException("displayDurationMs must be > 0");
        this.fudaSet = fudaSet;
        this.mode = mode;
        this.displayDurationMs = displayDurationMs;
        this.showKimariji = showKimariji;
        this.mixUpsideDown = mixUpsideDown;
    }

    public FudaSet getFudaSet() { return fudaSet; }
    public NagashiMode getMode() { return mode; }
    public int getDisplayDurationMs() { return displayDurationMs; }
    public boolean isShowKimariji() { return showKimariji; }
    public boolean isMixUpsideDown() { return mixUpsideDown; }
}
