package com.example.karutasuke.boundary;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.karutasuke.R;
import com.example.karutasuke.control.AssetReadingPlayer;
import com.example.karutasuke.control.FudaCatalog;
import com.example.karutasuke.control.FudaSetManager;
import com.example.karutasuke.control.KikiwakeController;
import com.example.karutasuke.control.TimeController;
import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaSet;
import com.example.karutasuke.entity.KikiwakeConfig;
import com.example.karutasuke.entity.Layout;
import com.example.karutasuke.entity.PracticeResult;
import com.example.karutasuke.entity.ReadingStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 聞き分け (FR-07). Setup step → 暗記 period → reading → result overlay.
 * The field shows the 敵陣 upside down at the top and the 自陣 upright at the bottom.
 * Swipe (or tap) a 札 to take it.
 */
public class KikiwakePage extends AppCompatActivity implements KikiwakeController.Listener {

    private List<FudaSet> sets;
    private int[] memorizationSeconds;
    private AssetReadingPlayer player;
    private KikiwakeController controller;
    private final TimeController timer = new TimeController();
    private KikiwakeConfig lastConfig;
    private boolean warnedMissingAudio;
    private boolean practicing;

    private View setupPanel;
    private View practicePanel;
    private Spinner setSpinner;
    private TextView setInfo;
    private RadioGroup readingStyle;
    private TextView karafudaLabel;
    private SeekBar karafudaSeek;
    private Spinner memorizationSpinner;
    private FieldView fieldView;
    private TextView status;
    private Button actionButton;
    private String countdownText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kikiwake_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.kikiwake_page), (v, insets) -> {
            // Bars are hidden during practice, so this mostly keeps clear of a camera notch then.
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        FudaCatalog catalog = FudaCatalog.getInstance(this);
        sets = FudaSetManager.getBuiltInSets(catalog);
        memorizationSeconds = getResources().getIntArray(R.array.memorization_seconds);
        player = new AssetReadingPlayer(this);
        controller = new KikiwakeController(catalog, player, this, new Random(), SystemClock::elapsedRealtime);

        setupPanel = findViewById(R.id.setup_panel);
        practicePanel = findViewById(R.id.practice_panel);
        setSpinner = findViewById(R.id.set_spinner);
        setInfo = findViewById(R.id.set_info);
        readingStyle = findViewById(R.id.reading_style);
        karafudaLabel = findViewById(R.id.karafuda_label);
        karafudaSeek = findViewById(R.id.karafuda_seek);
        memorizationSpinner = findViewById(R.id.memorization_spinner);
        fieldView = findViewById(R.id.field);
        status = findViewById(R.id.status);
        actionButton = findViewById(R.id.action_button);

        setUpSetupStep();
        fieldView.setOnTakeListener(this::take);
        findViewById(R.id.start_button).setOnClickListener(v -> startPractice(readConfig()));
        actionButton.setOnClickListener(v -> {
            if (controller.isMemorizing()) beginReading();
            else controller.finish();
        });
    }

    // --- setup step ---

    private void setUpSetupStep() {
        List<String> names = new ArrayList<>();
        for (FudaSet s : sets) names.add(s.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setSpinner.setAdapter(adapter);
        setSpinner.setSelection(1);   // 一字決まり: a small field to start with
        setSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FudaSet set = sets.get(position);
                int onField = KikiwakeConfig.fieldSize(set);
                setInfo.setText(onField < set.size()
                        ? getString(R.string.kikiwake_set_size_limited, set.size(), onField)
                        : getString(R.string.kikiwake_set_size, set.size()));
                karafudaSeek.setMax(KikiwakeConfig.maxKarafuda(set));
                updateKarafudaLabel();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        karafudaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateKarafudaLabel(); }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        ArrayAdapter<CharSequence> times = ArrayAdapter.createFromResource(
                this, R.array.memorization_labels, android.R.layout.simple_spinner_item);
        times.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        memorizationSpinner.setAdapter(times);
        memorizationSpinner.setSelection(3);   // 2分
    }

    private void updateKarafudaLabel() {
        karafudaLabel.setText(getString(R.string.kikiwake_karafuda_count, karafudaSeek.getProgress()));
    }

    private KikiwakeConfig readConfig() {
        FudaSet set = sets.get(setSpinner.getSelectedItemPosition());
        ReadingStyle style = readingStyle.getCheckedRadioButtonId() == R.id.style_match
                ? ReadingStyle.MATCH_STYLE : ReadingStyle.KAMINOKU_ONLY;
        int karafuda = Math.min(karafudaSeek.getProgress(), KikiwakeConfig.maxKarafuda(set));
        int seconds = memorizationSeconds[memorizationSpinner.getSelectedItemPosition()];
        return new KikiwakeConfig(set, style, karafuda, seconds);
    }

    // --- practice ---

    private void startPractice(KikiwakeConfig config) {
        lastConfig = config;
        warnedMissingAudio = false;
        practicing = true;
        setupPanel.setVisibility(View.GONE);
        practicePanel.setVisibility(View.VISIBLE);
        setFullScreen(true);

        controller.start(config);      // lays out the field (onFieldChanged)
        fieldView.setInteractive(false);
        actionButton.setText(R.string.kikiwake_memorize_done);

        if (config.getMemorizationSeconds() == 0) {
            beginReading();
        } else {
            timer.startCountdown(config.getMemorizationSeconds(), secondsLeft -> {
                countdownText = getString(R.string.kikiwake_memorizing, secondsLeft / 60, secondsLeft % 60);
                updateStatus();
            }, this::beginReading);
        }
    }

    private void beginReading() {
        timer.stop();
        countdownText = "";
        actionButton.setText(R.string.kikiwake_stop);
        fieldView.setInteractive(true);
        updateStatus();
        controller.beginReading();
    }

    private void take(Fuda fuda) {
        if (practicing) controller.onSwipeFuda(fuda);
    }

    private void updateStatus() {
        if (controller.isMemorizing()) {
            status.setText(countdownText);
        } else {
            status.setText(getString(R.string.kikiwake_status, controller.getRemaining(), controller.getTotalOtetsuki()));
        }
    }

    /** Hides the status and navigation bars so the field gets the whole screen height. */
    private void setFullScreen(boolean fullScreen) {
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (fullScreen) {
            bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            bars.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            bars.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void showSetup() {
        practicePanel.setVisibility(View.GONE);
        setupPanel.setVisibility(View.VISIBLE);
        setFullScreen(false);
    }

    // --- KikiwakeController.Listener ---

    @Override
    public void onFieldChanged(Layout field) {
        fieldView.setLayout(field);
        updateStatus();
    }

    @Override
    public void onCorrect(Fuda fuda, long reactionTimeMs) { }

    @Override
    public void onOtetsuki(Fuda taken, int totalOtetsuki) {
        fieldView.flashWrong(taken);
        updateStatus();
    }

    @Override
    public void onAudioMissing(String assetPath) {
        if (warnedMissingAudio) return;   // once per practice is enough
        warnedMissingAudio = true;
        Toast.makeText(this, getString(R.string.kikiwake_audio_missing, assetPath), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFinished(PracticeResult result) {
        practicing = false;
        timer.stop();
        fieldView.setInteractive(false);
        if (isFinishing() || isDestroyed()) return;
        ResultOverlay.show(this, result, controller.getFieldSizeAtStart(),
                () -> startPractice(lastConfig),
                this::showSetup);
    }

    // --- lifecycle ---

    @Override
    protected void onPause() {
        super.onPause();
        if (practicing) controller.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (practicing) controller.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        practicing = false;
        timer.stop();
        player.release();
    }
}
