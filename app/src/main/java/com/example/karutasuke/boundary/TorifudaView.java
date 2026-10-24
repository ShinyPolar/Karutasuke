package com.example.karutasuke.boundary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * One 取り札, drawn by FudaCardPainter.
 * Width comes from the layout; height is always width × 73 / 52.
 */
public class TorifudaView extends View {

    private final FudaCardPainter painter;
    private final RectF bounds = new RectF();
    private List<String> lines = Collections.emptyList();

    public TorifudaView(Context context) { this(context, null); }

    public TorifudaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        painter = new FudaCardPainter(context);
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
        setMeasuredDimension(width, Math.round(width * FudaCardPainter.ASPECT));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        bounds.set(0, 0, getWidth(), getHeight());
        painter.draw(canvas, bounds, lines, false, false);
    }
}
