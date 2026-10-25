package com.example.karutasuke.boundary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.example.karutasuke.R;

import java.util.List;

/**
 * Draws a 取り札 (card, green edge, vertical text in right-to-left columns) into any rectangle.
 * Shared by TorifudaView (one card) and FieldView (the whole field).
 */
final class FudaCardPainter {

    /** A real card is 52 mm × 73 mm. */
    static final float ASPECT = 73f / 52f;

    private static final float BORDER_RATIO = 0.06f;    // green edge, as a fraction of width
    private static final float PADDING_RATIO = 0.10f;   // space between edge and text
    private static final int STANDARD_CHARS = 5;   // kana per column on a normal 取り札

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();

    FudaCardPainter(Context context) {
        cardPaint.setColor(ContextCompat.getColor(context, R.color.fuda_paper));
        borderPaint.setColor(ContextCompat.getColor(context, R.color.fuda_edge));
        borderPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(ContextCompat.getColor(context, R.color.fuda_ink));
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);
        highlightPaint.setColor(ContextCompat.getColor(context, R.color.fuda_wrong));
    }

    /**
     * @param upsideDown true for 札 in the 敵陣, which face the opponent
     * @param wrong      true to tint the card red (お手つき feedback)
     */
    void draw(Canvas canvas, RectF bounds, List<String> lines, boolean upsideDown, boolean wrong) {
        canvas.save();
        if (upsideDown) canvas.rotate(180, bounds.centerX(), bounds.centerY());

        float w = bounds.width();
        float h = bounds.height();
        float border = Math.max(1f, w * BORDER_RATIO);
        float corner = w * 0.04f;
        card.set(bounds.left + border / 2, bounds.top + border / 2,
                bounds.right - border / 2, bounds.bottom - border / 2);
        canvas.drawRoundRect(card, corner, corner, cardPaint);
        borderPaint.setStrokeWidth(border);
        canvas.drawRoundRect(card, corner, corner, borderPaint);

        if (!lines.isEmpty()) {
            float pad = border + w * PADDING_RATIO;
            float columnWidth = (w - 2 * pad) / lines.size();
            // Size the grid for a normal 5-kana column, so one long column doesn't shrink the whole card.
            float cell = Math.min(columnWidth, (h - 2 * pad) / STANDARD_CHARS);
            float columnHeight = cell * STANDARD_CHARS;

            for (int col = 0; col < lines.size(); col++) {
                String line = lines.get(col);
                // A column with more than 5 kana (e.g. いてつるかな) is squeezed into the same height.
                float step = columnHeight / Math.max(line.length(), STANDARD_CHARS);
                textPaint.setTextSize(Math.min(cell * 0.92f, step * 1.02f));
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float baselineOffset = -(fm.ascent + fm.descent) / 2;

                float x = bounds.right - pad - columnWidth * (col + 0.5f);   // right to left
                for (int i = 0; i < line.length(); i++) {
                    float y = bounds.top + pad + step * (i + 0.5f) + baselineOffset;
                    canvas.drawText(line, i, i + 1, x, y, textPaint);
                }
            }
        }

    

        if (wrong) canvas.drawRoundRect(card, corner, corner, highlightPaint);
        canvas.restore();
    }
}
