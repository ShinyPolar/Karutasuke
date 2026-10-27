package com.example.karutasuke.boundary;

import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.example.karutasuke.control.FudaSetManager;
import com.example.karutasuke.entity.FudaSet;

import java.util.ArrayList;
import java.util.List;

/** The 札 set dropdown shared by the practice setup steps. */
final class SetPicker {

    private SetPicker() { }

    /** Every set that can be practised: built-in and user sets, skipping empty ones. */
    static List<FudaSet> loadSets(Context context) {
        List<FudaSet> sets = new ArrayList<>();
        for (FudaSet s : FudaSetManager.getInstance(context).getAllSets()) {
            if (s.size() > 0) sets.add(s);
        }
        return sets;
    }

    /** The id of the set currently chosen in the spinner, or null if none. */
    @Nullable
    static String selectedId(Spinner spinner, List<FudaSet> sets) {
        int p = spinner.getSelectedItemPosition();
        return p >= 0 && p < sets.size() ? sets.get(p).getId() : null;
    }

    /**
     * Fills the spinner and selects the set with keepId (e.g. the one chosen before going to
     * the set editor), or defaultIndex if it no longer exists.
     * @return the selected position
     */
    static int bind(Spinner spinner, List<FudaSet> sets, @Nullable String keepId, int defaultIndex) {
        List<String> names = new ArrayList<>();
        int position = Math.min(defaultIndex, sets.size() - 1);
        for (int i = 0; i < sets.size(); i++) {
            names.add(sets.get(i).getName());
            if (sets.get(i).getId().equals(keepId)) position = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(),
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(position);
        return position;
    }

    static void openEditor(Context context) {
        context.startActivity(new Intent(context, SetEditorPage.class));
    }
}
