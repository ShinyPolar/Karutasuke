package com.example.karutasuke.boundary;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karutasuke.R;
import com.example.karutasuke.control.FudaCatalog;
import com.example.karutasuke.control.SortKey;
import com.example.karutasuke.control.SortOrder;

/** 札一覧: lists all 100 取り札 and lets the user sort them (FR-09, FR-10). */
public class FudaPage extends AppCompatActivity {

    private static final int CARD_WIDTH_DP = 90;
    private static final int MIN_COLUMNS = 3;

    private FudaCatalog catalog;
    private FudaAdapter adapter;
    private RecyclerView list;
    private Button orderButton;

    private SortKey sortKey = SortKey.NUMBER;
    private SortOrder sortOrder = SortOrder.ASC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fuda_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fuda_page), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        catalog = FudaCatalog.getInstance(this);

        adapter = new FudaAdapter(f -> startActivity(FudaDetailPage.newIntent(this, f.getNumber())));
        list = findViewById(R.id.fuda_list);
        // As many cards per row as fit at about CARD_WIDTH_DP each (more in landscape).
        int columns = Math.max(MIN_COLUMNS, getResources().getConfiguration().screenWidthDp / CARD_WIDTH_DP);
        list.setLayoutManager(new GridLayoutManager(this, columns));
        list.setAdapter(adapter);

        Spinner sortSpinner = findViewById(R.id.sort_key);
        ArrayAdapter<CharSequence> sortOptions = ArrayAdapter.createFromResource(
                this, R.array.sort_keys, android.R.layout.simple_spinner_item);
        sortOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortOptions);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // R.array.sort_keys is in the same order as the SortKey enum.
                sortKey = SortKey.values()[position];
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        orderButton = findViewById(R.id.sort_order);
        orderButton.setOnClickListener(v -> {
            sortOrder = (sortOrder == SortOrder.ASC) ? SortOrder.DESC : SortOrder.ASC;
            refresh();
        });

        refresh();
    }

    private void refresh() {
        orderButton.setText(sortOrder == SortOrder.ASC ? R.string.sort_asc : R.string.sort_desc);
        adapter.submit(catalog.sortBy(sortKey, sortOrder));
        list.scrollToPosition(0);
    }
}
