package com.example.karutasuke.boundary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.karutasuke.R;
import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.Jin;
import com.example.karutasuke.entity.Layout;
import com.example.karutasuke.entity.Position;
import com.example.karutasuke.entity.Side;

import java.util.HashMap;
import java.util.Map;

/**
 * The playing field as seen from your seat: 敵陣 at the top (upside down, facing the
 * opponent), 自陣 at the bottom (upright). Each 陣 has 3 段 of up to 16 札.
 *
 * Swipe or tap a 札 to take it; the listener decides whether that was right.
 */
public class FieldView extends View {

    public interface OnTakeListener {
        void onTake(Fuda fuda);
    }

    private static final int COLUMNS = 2 * Position.COLUMNS_PER_SIDE;   // 16 across
    private static final int ROWS = 2 * Position.DAN_COUNT;             // 6 down
    private static final float CARD_GAP = 0.06f;     // gap between 札 in a row, × card width
    private static final float ROW_GAP = 0.08f;      // gap between 段, × card width
    private static final long WRONG_FLASH_MS = 400;

    private final FudaCardPainter painter;
    private final Paint fieldPaint = new Paint();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Fuda, RectF> cardRects = new HashMap<>();
    private final float centreGapPx;
    private final float swipeSlopPx;

    private Layout layout = new Layout();
    private OnTakeListener listener;
    private boolean interactive;
    private Fuda wrongFuda;
    private Fuda pressed;
    private float downX, downY;
    private float fieldTop, fieldBottom, centreY;

    public FieldView(Context context) { this(context, null); }

    public FieldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        painter = new FudaCardPainter(context);
        fieldPaint.setColor(ContextCompat.getColor(context, R.color.field_tatami));
        linePaint.setColor(ContextCompat.getColor(context, R.color.field_line));
        float density = getResources().getDisplayMetrics().density;
        centreGapPx = 40 * density;     // the space between the two 陣; the page's status bar sits here
        swipeSlopPx = 16 * density;
    }

    public void setOnTakeListener(OnTakeListener listener) { this.listener = listener; }

    /** Shows these 札 at their positions. The layout is copied, so later changes need another call. */
    public void setLayout(Layout layout) {
        this.layout = layout.copy();
        wrongFuda = null;
        computeRects();
        invalidate();
    }

    /** false during the 暗記 period, so touching the field does nothing. */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        pressed = null;
    }

    /** Briefly tints a 札 red. */
    public void flashWrong(Fuda fuda) {
        wrongFuda = fuda;
        invalidate();
        postDelayed(() -> {
            if (wrongFuda == fuda) {
                wrongFuda = null;
                invalidate();
            }
        }, WRONG_FLASH_MS);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        computeRects();
    }

    /** Fits 16 × 6 札 into the view, keeping the real card shape, and records each 札's rectangle. */
    private void computeRects() {
        cardRects.clear();
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;

        float byWidth = w / (COLUMNS + (COLUMNS - 1) * CARD_GAP);
        float byHeight = (h - centreGapPx) / (ROWS * FudaCardPainter.ASPECT + (ROWS - 2) * ROW_GAP);
        float cardW = Math.min(byWidth, byHeight);
        float cardH = cardW * FudaCardPainter.ASPECT;
        float stepX = cardW * (1 + CARD_GAP);
        float stepY = cardH + cardW * ROW_GAP;

        float jinHeight = Position.DAN_COUNT * cardH + (Position.DAN_COUNT - 1) * cardW * ROW_GAP;
        float totalW = COLUMNS * cardW + (COLUMNS - 1) * cardW * CARD_GAP;
        float totalH = 2 * jinHeight + centreGapPx;
        float left = getPaddingLeft() + (w - totalW) / 2;
        fieldTop = getPaddingTop() + (h - totalH) / 2;
        fieldBottom = fieldTop + totalH;
        centreY = fieldTop + jinHeight + centreGapPx / 2;

        for (Map.Entry<Fuda, Position> e : layout.asMap().entrySet()) {
            Position p = e.getValue();
            int col = screenColumn(p);
            float top;
            if (p.getJin() == Jin.OPPONENT) {
                // Their 下段 (dan 1) is nearest them, so at the top of the screen.
                top = fieldTop + (p.getDan() - 1) * stepY;
            } else {
                // Your 下段 is nearest you, at the bottom.
                top = fieldBottom - cardH - (p.getDan() - 1) * stepY;
            }
            float x = left + col * stepX;
            cardRects.put(e.getKey(), new RectF(x, top, x + cardW, top + cardH));
        }
    }

    /**
     * Converts a Position (counted from the owner's view, column 1 = outer edge) to 0–15 from the
     * left of the screen. The opponent faces you, so their left is your right.
     */
    static int screenColumn(Position p) {
        boolean onScreenLeft = (p.getJin() == Jin.SELF) == (p.getSide() == Side.LEFT);
        return onScreenLeft ? p.getColumn() - 1 : COLUMNS - p.getColumn();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), fieldPaint);
        canvas.drawRect(0, centreY - 1, getWidth(), centreY + 1, linePaint);   // centre line

        for (Map.Entry<Fuda, RectF> e : cardRects.entrySet()) {
            Fuda f = e.getKey();
            Position p = layout.getPosition(f);
            boolean upsideDown = p != null && p.getJin() == Jin.OPPONENT;
            painter.draw(canvas, e.getValue(), f.getTorifuda(), upsideDown, f.equals(wrongFuda));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(event.getX(), event.getY());
                downX = event.getX();
                downY = event.getY();
                return pressed != null;
            case MotionEvent.ACTION_MOVE:
                // A swipe takes the 札 as soon as it has moved far enough, like sweeping it off.
                if (pressed != null && Math.hypot(event.getX() - downX, event.getY() - downY) > swipeSlopPx) {
                    take();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed != null) {
                    performClick();
                    take();   // a tap also counts
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = null;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void take() {
        Fuda f = pressed;
        pressed = null;
        if (f != null && listener != null) listener.onTake(f);
    }

    @Nullable
    private Fuda hit(float x, float y) {
        for (Map.Entry<Fuda, RectF> e : cardRects.entrySet()) {
            if (e.getValue().contains(x, y)) return e.getKey();
        }
        return null;
    }
}
