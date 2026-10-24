package com.example.karutasuke.control;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.karutasuke.entity.Fuda;

/**
 * Reads and saves the user's note for each 札.
 *
 * fuda.json lives in the app's assets, which are read-only once the app is installed,
 * so notes the user writes are saved in SharedPreferences instead. A saved note
 * replaces the built-in note from fuda.json; until then the built-in one is shown.
 */
public final class FudaNoteManager {

    private static final String PREFS_NAME = "fuda_notes";
    private static final String KEY_PREFIX = "note_";

    private final SharedPreferences prefs;

    public FudaNoteManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The note to show: the user's saved note if there is one, otherwise the built-in note. */
    public String getNote(Fuda fuda) {
        return prefs.getString(key(fuda), fuda.getNote());
    }

    /**
     * Saves the user's note. Saving text identical to the built-in note removes the saved copy,
     * so later changes to fuda.json still show up. Saving "" keeps an empty note, even if the
     * built-in one isn't empty.
     */
    public void saveNote(Fuda fuda, String text) {
        SharedPreferences.Editor editor = prefs.edit();
        if (text.equals(fuda.getNote())) {
            editor.remove(key(fuda));
        } else {
            editor.putString(key(fuda), text);
        }
        editor.apply();
    }

    private static String key(Fuda fuda) {
        return KEY_PREFIX + fuda.getNumber();
    }
}
