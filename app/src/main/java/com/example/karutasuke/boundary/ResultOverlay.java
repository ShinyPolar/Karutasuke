package com.example.karutasuke.boundary;

import android.app.Activity;

import com.example.karutasuke.R;
import com.example.karutasuke.entity.FudaAttempt;
import com.example.karutasuke.entity.PracticeResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows a PracticeResult over the practice screen, with もう一度 and 終了 buttons (FR-12). */
final class ResultOverlay {

    private static final int SLOWEST_SHOWN = 3;

    private ResultOverlay() { }

    /**
     * @param totalOnBoard how many 札 were on the board at the start
     * @param onRetry      runs when the user taps もう一度
     * @param onClose      runs when the user taps 終了
     */
    static void show(Activity activity, PracticeResult result, int totalOnBoard,
                     Runnable onRetry, Runnable onClose) {
        int taken = 0;
        int otetsuki = 0;
        long reactionSum = 0;
        List<FudaAttempt> timed = new ArrayList<>();
        for (FudaAttempt a : result.getAttempts()) {
            otetsuki += a.getOtetsukiCount();
            if (a.hasReaction()) {
                taken++;
                reactionSum += a.getReactionTime();
                timed.add(a);
            }
        }

        StringBuilder msg = new StringBuilder();
        long seconds = result.getDuration() / 1000;
        msg.append(activity.getString(R.string.result_time, seconds / 60, seconds % 60)).append('\n');
        msg.append(activity.getString(R.string.result_taken, taken, totalOnBoard)).append('\n');
        if (result.getScore() != null) {
            msg.append(activity.getString(R.string.result_clean, result.getScore())).append('\n');
        }
        msg.append(activity.getString(R.string.result_otetsuki, otetsuki));

        if (!timed.isEmpty()) {
            msg.append('\n').append(activity.getString(R.string.result_average,
                    formatSeconds(reactionSum / timed.size())));

            timed.sort((a, b) -> Long.compare(b.getReactionTime(), a.getReactionTime()));
            StringBuilder slowest = new StringBuilder();
            for (int i = 0; i < Math.min(SLOWEST_SHOWN, timed.size()); i++) {
                FudaAttempt a = timed.get(i);
                if (i > 0) slowest.append("、");
                slowest.append(a.getFuda().getKimariji()).append(' ').append(formatSeconds(a.getReactionTime()));
            }
            msg.append('\n').append(activity.getString(R.string.result_slowest, slowest));
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.result_title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.result_retry, (d, w) -> onRetry.run())
                .setNegativeButton(R.string.result_close, (d, w) -> onClose.run())
                .show();
    }

    private static String formatSeconds(long ms) {
        return String.format(Locale.ROOT, "%.2f秒", ms / 1000.0);
    }
}
