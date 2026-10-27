package com.example.karutasuke.boundary;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karutasuke.R;
import com.example.karutasuke.control.FudaCatalog;
import com.example.karutasuke.control.FudaSetManager;
import com.example.karutasuke.control.SortKey;
import com.example.karutasuke.control.SortOrder;
import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 札セット編集 (FR-02). Two panels:
 *  - the list of sets: tap a set to edit it, long-press for rename / duplicate / delete.
 *    Built-in sets can't be edited, only copied.
 *  - the editor: all 100 札; tap a card to add it to or remove it from the set.
 *    Changes are saved automatically.
 */
public class SetEditorPage extends AppCompatActivity {

    private static final String STATE_EDITING = "editing_set";
    private static final String STATE_SELECTED = "selected_fuda";
    private static final int CARD_WIDTH_DP = 80;
    private static final int MIN_COLUMNS = 4;

    private FudaSetManager manager;
    private FudaCatalog catalog;

    private View listPanel;
    private View editPanel;
    private SetListAdapter setAdapter;
    private FudaAdapter fudaAdapter;
    private TextView editTitle;
    private TextView editCount;
    private RecyclerView fudaGrid;
    private OnBackPressedCallback backToList;

    @Nullable private FudaSet editing;
    private final Set<Fuda> selected = new HashSet<>();
    private SortKey sortKey = SortKey.KIMARIJI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_editor_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.set_editor_page), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        manager = FudaSetManager.getInstance(this);
        catalog = FudaCatalog.getInstance(this);
        listPanel = findViewById(R.id.list_panel);
        editPanel = findViewById(R.id.edit_panel);
        editTitle = findViewById(R.id.edit_title);
        editCount = findViewById(R.id.edit_count);

        // Set list
        RecyclerView setList = findViewById(R.id.set_list);
        setList.setLayoutManager(new LinearLayoutManager(this));
        setAdapter = new SetListAdapter(this::onSetClick, this::onSetLongClick);
        setList.setAdapter(setAdapter);
        findViewById(R.id.new_set_button).setOnClickListener(v ->
                askName(R.string.set_new, "", name -> openEditor(manager.createSet(name, new HashSet<>()))));

        // Editor
        fudaGrid = findViewById(R.id.fuda_grid);
        int columns = Math.max(MIN_COLUMNS, getResources().getConfiguration().screenWidthDp / CARD_WIDTH_DP);
        fudaGrid.setLayoutManager(new GridLayoutManager(this, columns));
        fudaAdapter = new FudaAdapter(this::toggle);
        fudaAdapter.setSelection(selected);
        fudaGrid.setAdapter(fudaAdapter);

        Spinner sortSpinner = findViewById(R.id.edit_sort);
        ArrayAdapter<CharSequence> sortOptions = ArrayAdapter.createFromResource(
                this, R.array.sort_keys, android.R.layout.simple_spinner_item);
        sortOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortOptions);
        sortSpinner.setSelection(sortKey.ordinal());   // 決まり字 order keeps 友札 next to each other
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortKey = SortKey.values()[position];
                fudaAdapter.submit(catalog.sortBy(sortKey, SortOrder.ASC));
                fudaGrid.scrollToPosition(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        findViewById(R.id.edit_done).setOnClickListener(v -> closeEditor());
        findViewById(R.id.edit_rename).setOnClickListener(v -> {
            if (editing == null) return;
            askName(R.string.set_rename, editing.getName(), name -> {
                saveEditing();
                editing = manager.renameSet(editing, name);
                updateHeader();
            });
        });
        findViewById(R.id.edit_clear).setOnClickListener(v -> {
            selected.clear();
            fudaAdapter.setSelection(selected);
            updateHeader();
        });

        backToList = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() { closeEditor(); }
        };
        getOnBackPressedDispatcher().addCallback(this, backToList);

        refreshList();
        restoreEditor(savedInstanceState);
    }

    // --- set list ---

    private void refreshList() {
        // The user's own sets first: they're the ones being edited.
        List<FudaSet> sets = manager.getUserSets();
        sets.addAll(manager.getBuiltIn());
        setAdapter.submit(sets);
    }

    private void onSetClick(FudaSet set) {
        if (set.isBuiltIn()) offerCopy(set);
        else openEditor(set);
    }

    private void onSetLongClick(FudaSet set) {
        if (set.isBuiltIn()) {
            offerCopy(set);
            return;
        }
        String[] actions = {getString(R.string.set_rename), getString(R.string.set_duplicate), getString(R.string.set_delete)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(set.getName())
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        askName(R.string.set_rename, set.getName(), name -> {
                            manager.renameSet(set, name);
                            refreshList();
                        });
                    } else if (which == 1) {
                        manager.duplicateSet(set);
                        refreshList();
                    } else {
                        confirmDelete(set);
                    }
                })
                .show();
    }

    private void offerCopy(FudaSet set) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(set.getName())
                .setMessage(R.string.set_builtin_message)
                .setPositiveButton(R.string.set_copy_and_edit, (d, w) -> openEditor(manager.duplicateSet(set)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmDelete(FudaSet set) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.set_delete_confirm, set.getName()))
                .setPositiveButton(R.string.set_delete, (d, w) -> {
                    manager.deleteSet(set);
                    refreshList();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** A dialog with one text field. onOk only runs for a non-blank name. */
    private void askName(int titleRes, String initial, Consumer<String> onOk) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setHint(R.string.set_name_hint);
        input.setText(initial);
        input.selectAll();
        FrameLayout box = new FrameLayout(this);
        int pad = Math.round(24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.set_name_empty, Toast.LENGTH_SHORT).show();
                    } else {
                        onOk.accept(name);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        input.requestFocus();
    }

    // --- editor ---

    private void openEditor(FudaSet set) {
        editing = set;
        selected.clear();
        selected.addAll(set.getFuda());
        listPanel.setVisibility(View.GONE);
        editPanel.setVisibility(View.VISIBLE);
        backToList.setEnabled(true);
        fudaAdapter.setSelection(selected);
        updateHeader();
    }

    private void closeEditor() {
        saveEditing();
        editing = null;
        backToList.setEnabled(false);
        editPanel.setVisibility(View.GONE);
        listPanel.setVisibility(View.VISIBLE);
        refreshList();
    }

    private void toggle(Fuda fuda) {
        if (!selected.remove(fuda)) selected.add(fuda);
        fudaAdapter.refresh(fuda);
        updateHeader();
    }

    private void updateHeader() {
        if (editing == null) return;
        editTitle.setText(editing.getName());
        editCount.setText(getString(R.string.set_selected_count, selected.size()));
    }

    private void saveEditing() {
        if (editing != null) editing = manager.setFuda(editing, selected);
    }

    private void restoreEditor(@Nullable Bundle state) {
        if (state == null) return;
        String id = state.getString(STATE_EDITING);
        FudaSet set = id == null ? null : manager.getById(id);
        if (set == null || set.isBuiltIn()) return;
        openEditor(set);
        int[] numbers = state.getIntArray(STATE_SELECTED);
        if (numbers != null) {
            selected.clear();
            for (int n : numbers) selected.add(catalog.getByNumber(n));
            fudaAdapter.setSelection(selected);
            updateHeader();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (editing == null) return;
        out.putString(STATE_EDITING, editing.getId());
        int[] numbers = new int[selected.size()];
        int i = 0;
        for (Fuda f : selected) numbers[i++] = f.getNumber();
        out.putIntArray(STATE_SELECTED, numbers);
    }

    /** Saves whenever the user leaves the screen (home, switching apps, rotating). */
    @Override
    protected void onPause() {
        super.onPause();
        saveEditing();
    }
}
