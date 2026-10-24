package com.example.karutasuke.entity;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * One of the 100 poems of the Ogura Hyakunin Isshu.
 *
 * Immutable. Two Fuda are equal when they have the same poem number,
 * so a Fuda can safely be used as a key in Layout / Teichi maps.
 */
public final class Fuda {

    private final int number;               // 歌番号, 1–100
    private final String poet;
    private final String kaminoku;          // 上の句 (kanji and kana)
    private final String kaminokuHiragana;
    private final String shimonoku;         // 下の句 (kanji and kana)
    private final String shimonokuHiragana;
    private final String kimariji;          // pronounced, e.g. "あさぼらけう"
    private final List<String> torifuda;    // the 3 lines printed on the 取り札
    private final String kaminokuAudio;     // asset path, e.g. "audio/kaminoku/001.mp3"
    private final String shimonokuAudio;
    private final String note;              // built-in note from fuda.json ("" if none)

    public Fuda(int number, String poet,
                String kaminoku, String kaminokuHiragana,
                String shimonoku, String shimonokuHiragana,
                String kimariji, List<String> torifuda,
                String kaminokuAudio, String shimonokuAudio,
                String note) {
        this.number = number;
        this.poet = poet;
        this.kaminoku = kaminoku;
        this.kaminokuHiragana = kaminokuHiragana;
        this.shimonoku = shimonoku;
        this.shimonokuHiragana = shimonokuHiragana;
        this.kimariji = kimariji;
        this.torifuda = Collections.unmodifiableList(torifuda);
        this.kaminokuAudio = kaminokuAudio;
        this.shimonokuAudio = shimonokuAudio;
        this.note = note;
    }

    public int getNumber() { return number; }
    public String getPoet() { return poet; }
    public String getKaminoku() { return kaminoku; }
    public String getKaminokuHiragana() { return kaminokuHiragana; }
    public String getShimonoku() { return shimonoku; }
    public String getShimonokuHiragana() { return shimonokuHiragana; }
    public String getKimariji() { return kimariji; }
    public List<String> getTorifuda() { return torifuda; }
    public String getKaminokuAudio() { return kaminokuAudio; }
    public String getShimonokuAudio() { return shimonokuAudio; }

    /**
     * The built-in note shipped in fuda.json. Notes the user writes in the app are stored
     * separately; use FudaNoteManager.getNote() to get the note that should be shown.
     */
    public String getNote() { return note; }

    /** 1 for 一字決まり, up to 6 for 大山札. Derived, so it can never disagree with kimariji. */
    public int getKimarijiLength() { return kimariji.length(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fuda)) return false;
        return number == ((Fuda) o).number;
    }

    @Override
    public int hashCode() { return Integer.hashCode(number); }

    @NonNull
    @Override
    public String toString() { return "Fuda{" + number + " " + kimariji + "}"; }
}
