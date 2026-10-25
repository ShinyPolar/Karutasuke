package com.example.karutasuke.boundary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.karutasuke.R;
import com.example.karutasuke.control.FudaCatalog;
import com.example.karutasuke.control.FudaSetManager;
import com.example.karutasuke.control.NagashiController;
import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaSet;
import com.example.karutasuke.entity.NagashiConfig;
import com.example.karutasuke.entity.NagashiMode;
import com.example.karutasuke.entity.PracticeResult;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 流し (FR-01 – FR-05). Setup step → one 札 at a time, thrown away in any direction → result.
 * In 瞬間認識 (RECOGNITION) mode each 札 goes blank after the display time.
 */
public class NagashiPage extends AppCompatActivity implements NagashiController.Listener {

    private static final long KIMARIJI_SHOW_MS = 1200;
    private static final long KIMARIJI_FADE_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<FudaSet> sets;
    private int[] durationsMs;
    private NagashiController controller;
    private NagashiConfig lastConfig;

    private View setupPanel;
    private View practicePanel;
    private Spinner setSpinner;
    private TextView setInfo;
    private RadioGroup modeGroup;
    private View durationLabel;
    private Spinner durationSpinner;
    private MaterialSwitch kimarijiSwitch;
    private MaterialSwitch upsideDownSwitch;
    private SwipeDeckView deck;
    private TextView progress;
    private TextView kimarijiView;

    private final Runnable hideCard = () -> deck.blankActive();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nagashi_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nagashi_page), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        sets = FudaSetManager.getBuiltInSets(FudaCatalog.getInstance(this));
        durationsMs = getResources().getIntArray(R.array.display_duration_ms);
        controller = new NagashiController(this, new Random(), SystemClock::elapsedRealtime);

        setupPanel = findViewById(R.id.setup_panel);
        practicePanel = findViewById(R.id.practice_panel);
        setSpinner = findViewById(R.id.set_spinner);
        setInfo = findViewById(R.id.set_info);
        modeGroup = findViewById(R.id.mode);
        durationLabel = findViewById(R.id.duration_label);
        durationSpinner = findViewById(R.id.duration_spinner);
        kimarijiSwitch = findViewById(R.id.kimariji_switch);
        upsideDownSwitch = findViewById(R.id.upside_down_switch);
        deck = findViewById(R.id.deck);
        progress = findViewById(R.id.progress);
        kimarijiView = findViewById(R.id.kimariji);

        setUpSetupStep();
        deck.setOnSwipeListener(this::onSwiped);
        findViewById(R.id.start_button).setOnClickListener(v -> startPractice(readConfig()));
        findViewById(R.id.stop_button).setOnClickListener(v -> controller.finish());
    }

    // --- setup step ---

    private void setUpSetupStep() {
        List<String> names = new ArrayList<>();
        for (FudaSet s : sets) names.add(s.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setSpinner.setAdapter(adapter);
        setSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setInfo.setText(getString(R.string.kikiwake_set_size, sets.get(position).size()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        ArrayAdapter<CharSequence> durations = ArrayAdapter.createFromResource(
                this, R.array.display_duration_labels, android.R.layout.simple_spinner_item);
        durations.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durations);
        durationSpinner.setSelection(2);   // 0.3秒

        modeGroup.setOnCheckedChangeListener((g, id) -> updateDurationVisibility());
        updateDurationVisibility();
    }

    private void updateDurationVisibility() {
        int visibility = isRecognition() ? View.VISIBLE : View.GONE;
        durationLabel.setVisibility(visibility);
        durationSpinner.setVisibility(visibility);
    }

    private boolean isRecognition() {
        return modeGroup.getCheckedRadioButtonId() == R.id.mode_recognition;
    }

    private NagashiConfig readConfig() {
        return new NagashiConfig(
                sets.get(setSpinner.getSelectedItemPosition()),
                isRecognition() ? NagashiMode.RECOGNITION : NagashiMode.MANUAL,
                durationsMs[durationSpinner.getSelectedItemPosition()],
                kimarijiSwitch.isChecked(),
                upsideDownSwitch.isChecked());
    }

    // --- practice ---

    private void startPractice(NagashiConfig config) {
        lastConfig = config;
        setupPanel.setVisibility(View.GONE);
        practicePanel.setVisibility(View.VISIBLE);
        setFullScreen(true);
        kimarijiView.animate().cancel();
        kimarijiView.setAlpha(0f);
        deck.clear();
        deck.setInteractive(true);
        deck.setPopIn(config.getMode() == NagashiMode.MANUAL);   // a pop-in would eat a 0.05秒 flash
        controller.start(config);
    }

    private void onSwiped() {
        handler.removeCallbacks(hideCard);
        Fuda swiped = controller.onSwipeFuda();
        if (swiped != null && lastConfig.isShowKimariji()) flashKimariji(swiped);
    }

    /** Shows the 決まり字 of the 札 just swiped, beside the card, then fades it out. */
    private void flashKimariji(Fuda fuda) {
        kimarijiView.animate().cancel();
        kimarijiView.setText(fuda.getKimariji());
        kimarijiView.setAlpha(1f);
        kimarijiView.animate().alpha(0f).setStartDelay(KIMARIJI_SHOW_MS).setDuration(KIMARIJI_FADE_MS);
    }

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
        deck.clear();
        practicePanel.setVisibility(View.GONE);
        setupPanel.setVisibility(View.VISIBLE);
        setFullScreen(false);
    }

    // --- NagashiController.Listener ---

    @Override
    public void onShow(Fuda fuda, boolean upsideDown, int index, int total) {
        deck.present(fuda.getTorifuda(), upsideDown);
        progress.setText(getString(R.string.nagashi_progress, index + 1, total));
        if (lastConfig.getMode() == NagashiMode.RECOGNITION) {
            handler.postDelayed(hideCard, lastConfig.getDisplayDurationMs());
        }
    }

    @Override
    public void onFinished(PracticeResult result) {
        handler.removeCallbacks(hideCard);
        deck.setInteractive(false);
        if (isFinishing() || isDestroyed()) return;
        ResultOverlay.show(this, result, result.getAttempts().size(),
                () -> startPractice(lastConfig),
                this::showSetup);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
