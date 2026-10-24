package com.example.karutasuke.boundary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.karutasuke.R;
import com.example.karutasuke.control.FudaCatalog;
import com.example.karutasuke.control.FudaNoteManager;
import com.example.karutasuke.entity.Fuda;

/** Shows one 札 in full: the card, its 決まり字, the whole poem and the poet. Opened from FudaPage. */
public class FudaDetailPage extends AppCompatActivity {

    private static final String EXTRA_NUMBER = "fuda_number";

    private Fuda fuda;
    private FudaNoteManager noteManager;
    private EditText noteInput;

    /** Use this to open the page, so callers don't need to know the extra's name. */
    public static Intent newIntent(Context context, int fudaNumber) {
        return new Intent(context, FudaDetailPage.class).putExtra(EXTRA_NUMBER, fudaNumber);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fuda_detail_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fuda_detail_page), (v, insets) -> {
            // Include the keyboard (ime) so the note box can scroll above it.
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        try {
            fuda = FudaCatalog.getInstance(this).getByNumber(getIntent().getIntExtra(EXTRA_NUMBER, -1));
        } catch (IllegalArgumentException e) {
            finish();   // opened without a valid number; nothing to show
            return;
        }

        TorifudaView card = findViewById(R.id.detail_card);
        card.setLines(fuda.getTorifuda());

        setText(R.id.detail_number, getString(R.string.detail_number, fuda.getNumber()));
        setText(R.id.detail_kimariji, fuda.getKimariji());
        setText(R.id.detail_kimariji_length, getString(R.string.detail_kimariji_length, fuda.getKimarijiLength()));
        setText(R.id.detail_kaminoku, fuda.getKaminoku());
        setText(R.id.detail_kaminoku_hiragana, fuda.getKaminokuHiragana());
        setText(R.id.detail_shimonoku, fuda.getShimonoku());
        setText(R.id.detail_shimonoku_hiragana, fuda.getShimonokuHiragana());
        setText(R.id.detail_poet, fuda.getPoet());

        noteManager = new FudaNoteManager(this);
        noteInput = findViewById(R.id.detail_note);
        if (savedInstanceState == null) {
            // On rotation Android restores what was typed, so only load the saved note the first time.
            noteInput.setText(noteManager.getNote(fuda));
        }
    }

    /** Saves automatically whenever the user leaves the screen (back, home, switching apps, rotating). */
    @Override
    protected void onPause() {
        super.onPause();
        if (fuda != null && noteInput != null) {
            Editable text = noteInput.getText();
            noteManager.saveNote(fuda, text == null ? "" : text.toString());
        }
    }

    private void setText(int viewId, CharSequence text) {
        ((TextView) findViewById(viewId)).setText(text);
    }
}
