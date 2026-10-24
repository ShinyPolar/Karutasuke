package com.example.karutasuke.control;

import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaAttempt;
import com.example.karutasuke.entity.KikiwakeConfig;
import com.example.karutasuke.entity.Layout;
import com.example.karutasuke.entity.PracticeResult;
import com.example.karutasuke.entity.PracticeType;
import com.example.karutasuke.entity.ReadingStyle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * 聞き分け practice (FR-07).
 *
 * start() lays the 札 out on the field (自陣 and 敵陣) and waits while the user memorizes them.
 * beginReading() then reads the field 札 plus karafuda (札 not on the field), shuffled:
 *   - field 札: wait until the user takes it. Taking any other 札 is an お手つき.
 *   - karafuda: once the 上の句 finishes, move on. Taking anything is an お手つき.
 * MATCH_STYLE reads the 序歌 first, and each reading's 下の句 before the next 上の句.
 * The practice ends when every field 札 has been taken (or finish() is called).
 */
public final class KikiwakeController {

    public static final String JOKA_AUDIO = "audio/joka.mp3";

    public interface Listener {
        /** The field changed: laid out at the start, or a 札 was taken. */
        void onFieldChanged(Layout field);

        void onCorrect(Fuda fuda, long reactionTimeMs);

        void onOtetsuki(Fuda taken, int totalOtetsuki);

        /** Audio for a 札 is missing. Not called for the 序歌, which is optional. */
        void onAudioMissing(String assetPath);

        void onFinished(PracticeResult result);
    }

    private enum Phase { IDLE, MEMORIZING, JOKA, SHIMONOKU, KAMINOKU, WAITING, FINISHED }

    private final FudaCatalog catalog;
    private final ReadingPlayer player;
    private final Listener listener;
    private final Random random;
    private final LongSupplier clock;

    private KikiwakeConfig config;
    private Layout field = new Layout();
    private final Set<Fuda> karafuda = new HashSet<>();
    private final Deque<Fuda> readingOrder = new ArrayDeque<>();
    private final List<FudaAttempt> attempts = new ArrayList<>();

    private Phase phase = Phase.IDLE;
    private Fuda current;           // the reading in progress
    private Fuda previous;          // the last finished reading; its 下の句 comes next in MATCH_STYLE
    private int currentOtetsuki;
    private int totalOtetsuki;
    private int fieldSizeAtStart;
    private long kaminokuStart;
    private long startTime;
    private int playToken;          // ignores callbacks from clips that were stopped
    private PracticeResult result;

    public KikiwakeController(FudaCatalog catalog, ReadingPlayer player, Listener listener,
                              Random random, LongSupplier clock) {
        this.catalog = catalog;
        this.player = player;
        this.listener = listener;
        this.random = random;
        this.clock = clock;
    }

    /** Lays out the field and enters the 暗記 period. Nothing is read until beginReading(). */
    public void start(KikiwakeConfig config) {
        stopAudio();
        this.config = config;
        karafuda.clear();
        readingOrder.clear();
        attempts.clear();
        current = null;
        previous = null;
        totalOtetsuki = 0;
        result = null;

        List<Fuda> pool = new ArrayList<>(config.getFudaSet().getFuda());
        Collections.shuffle(pool, random);
        List<Fuda> onField = new ArrayList<>(pool.subList(0, KikiwakeConfig.fieldSize(config.getFudaSet())));
        field = FieldLayoutGenerator.generate(onField, random);
        fieldSizeAtStart = field.size();

        List<Fuda> others = new ArrayList<>(catalog.sortBy(SortKey.NUMBER, SortOrder.ASC));
        others.removeAll(onField);
        Collections.shuffle(others, random);
        karafuda.addAll(others.subList(0, config.getKarafudaCount()));

        List<Fuda> readings = new ArrayList<>(onField);
        readings.addAll(karafuda);
        Collections.shuffle(readings, random);
        readingOrder.addAll(readings);

        phase = Phase.MEMORIZING;
        listener.onFieldChanged(field.copy());
    }

    /** Ends the 暗記 period and starts reading. Does nothing if reading has already begun. */
    public void beginReading() {
        if (phase != Phase.MEMORIZING) return;
        startTime = clock.getAsLong();
        if (config.getReadingStyle() == ReadingStyle.MATCH_STYLE) {
            phase = Phase.JOKA;
            play(JOKA_AUDIO, this::readNext);
        } else {
            readNext();
        }
    }

    /**
     * The user took (swiped or tapped) a 札 on the field.
     * @return true if it was the 札 being read
     */
    public boolean onSwipeFuda(Fuda fuda) {
        // Only the 上の句, or the wait after it, counts. 暗記, the 序歌 and 下の句 are ignored.
        if (phase != Phase.KAMINOKU && phase != Phase.WAITING) return false;
        if (!field.contains(fuda)) return false;

        if (!karafuda.contains(current) && current.equals(fuda)) {
            long reaction = clock.getAsLong() - kaminokuStart;
            stopAudio();
            field.remove(fuda);
            attempts.add(new FudaAttempt(current, reaction, currentOtetsuki, false));
            previous = current;
            listener.onCorrect(fuda, reaction);
            listener.onFieldChanged(field.copy());
            readNext();
            return true;
        }

        currentOtetsuki++;
        totalOtetsuki++;
        listener.onOtetsuki(fuda, totalOtetsuki);
        return false;
    }

    /** Ends the practice now (also called automatically when the field is empty). */
    public PracticeResult finish() {
        if (phase == Phase.FINISHED) return result;
        stopAudio();
        boolean neverRead = phase == Phase.MEMORIZING || phase == Phase.IDLE;
        phase = Phase.FINISHED;

        int clean = 0;
        List<Fuda> mistakes = new ArrayList<>();
        for (FudaAttempt a : attempts) {
            if (a.getOtetsukiCount() > 0) mistakes.add(a.getFuda());
            else if (!a.isKarafuda()) clean++;
        }
        long duration = neverRead ? 0 : clock.getAsLong() - startTime;
        result = new PracticeResult(PracticeType.KIKIWAKE, System.currentTimeMillis(),
                duration, clean, mistakes, attempts);
        listener.onFinished(result);
        return result;
    }

    public void pause() { player.pause(); }

    public void resume() { player.resume(); }

    public boolean isMemorizing() { return phase == Phase.MEMORIZING; }

    public boolean isFinished() { return phase == Phase.FINISHED; }

    public int getRemaining() { return field.size(); }

    public int getFieldSizeAtStart() { return fieldSizeAtStart; }

    public int getTotalOtetsuki() { return totalOtetsuki; }

    // --- reading sequence ---

    private void readNext() {
        if (field.size() == 0 || readingOrder.isEmpty()) {
            finish();
            return;
        }
        current = readingOrder.poll();
        currentOtetsuki = 0;

        if (config.getReadingStyle() == ReadingStyle.MATCH_STYLE && previous != null) {
            phase = Phase.SHIMONOKU;
            play(previous.getShimonokuAudio(), this::readKaminoku);
        } else {
            readKaminoku();
        }
    }

    private void readKaminoku() {
        phase = Phase.KAMINOKU;
        kaminokuStart = clock.getAsLong();
        play(current.getKaminokuAudio(), this::onKaminokuFinished);
    }

    private void onKaminokuFinished() {
        if (karafuda.contains(current)) {
            attempts.add(new FudaAttempt(current, FudaAttempt.NO_REACTION, currentOtetsuki, true));
            previous = current;
            readNext();
        } else {
            phase = Phase.WAITING;
        }
    }

    private void play(String path, Runnable onDone) {
        int token = ++playToken;
        boolean found = player.play(path, () -> {
            if (token == playToken && phase != Phase.FINISHED) onDone.run();
        });
        if (!found && !path.equals(JOKA_AUDIO)) listener.onAudioMissing(path);
    }

    private void stopAudio() {
        playToken++;
        player.stop();
    }
}
