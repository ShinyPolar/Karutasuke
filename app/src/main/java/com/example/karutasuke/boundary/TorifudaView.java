package com.example.karutasuke.boundary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.karutasuke.R;

import java.util.Collections;
import java.util.List;

/**
 * Draws a 取り札: a 52 × 73 card with the text written vertically (縦書き)
 * in columns from right to left, like a real competitive karuta card.
 *
 * Android's TextView can't do vertical text, so this view draws each character itself.
 * Width comes from the layout; height is always width × 73 / 52.
 */
public class TorifudaView extends View {

    private static final float ASPECT = 73f / 52f;       // real card is 52 mm × 73 mm
    private static final float BORDER_RATIO = 0.06f;     // green edge, as a fraction of width
    private static final float PADDING_RATIO = 0.10f;    // space between edge and text

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private List<String> lines = Collections.emptyList();

    public TorifudaView(Context context) { this(context, null); }

    public TorifudaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        cardPaint.setColor(ContextCompat.getColor(context, R.color.fuda_paper));
        borderPaint.setColor(ContextCompat.getColor(context, R.color.fuda_edge));
        borderPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(ContextCompat.getColor(context, R.color.fuda_ink));
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** The torifuda lines, e.g. ["わかころも", "てはつゆに", "ぬれつつ"]. First line is the rightmost column. */
    public void setLines(@NonNull List<String> lines) {
        this.lines = lines;
        setContentDescription(String.join("", lines));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, Math.round(width * ASPECT));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();

        // Card and green edge
        float border = w * BORDER_RATIO;
        float corner = w * 0.04f;
        rect.set(border / 2, border / 2, w - border / 2, h - border / 2);
        canvas.drawRoundRect(rect, corner, corner, cardPaint);
        borderPaint.setStrokeWidth(border);
        canvas.drawRoundRect(rect, corner, corner, borderPaint);

        if (lines.isEmpty()) return;

        // Every character gets a square cell; size it so the longest column fits.
        int maxChars = 1;
        for (String line : lines) maxChars = Math.max(maxChars, line.length());
        float pad = border + w * PADDING_RATIO;
        float columnWidth = (w - 2 * pad) / lines.size();
        float cell = Math.min(columnWidth, (h - 2 * pad) / maxChars);
        textPaint.setTextSize(cell * 0.92f);

        // Offset from a cell's centre to the text baseline
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baselineOffset = -(fm.ascent + fm.descent) / 2;

        for (int col = 0; col < lines.size(); col++) {
            String line = lines.get(col);
            float x = w - pad - columnWidth * (col + 0.5f);   // right to left
            for (int i = 0; i < line.length(); i++) {
                float y = pad + cell * (i + 0.5f) + baselineOffset;
                canvas.drawText(line, i, i + 1, x, y, textPaint);
            }
        }
    }
}
