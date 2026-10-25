package com.example.karutasuke.boundary;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One 取り札 in the middle of the screen that can be thrown away in any direction.
 *
 * Physics:
 *  - While dragging, the card follows the finger and tilts around the point you grabbed it by.
 *  - Let go fast enough (or far enough) and it flies off with your finger's velocity, slowed by
 *    friction and spinning according to the torque of the throw.
 *  - Otherwise it springs back to the centre.
 *
 * Two card views take turns: while one is flying away, the other already shows the next 札.
 */
public class SwipeDeckView extends FrameLayout {

    public interface OnSwipeListener {
        /** A card was thrown away. Call present() with the next 札. */
        void onSwiped();
    }

    private static final float MAX_HEIGHT_FRACTION = 0.82f;   // of the deck's height
    private static final float MAX_WIDTH_FRACTION = 0.5f;     // of the deck's width
    private static final float DISMISS_DISTANCE = 0.45f;      // × card width
    private static final float DRAG_TILT = 40f;               // degrees per unit of torque
    private static final float MAX_TILT = 30f;
    private static final float SPIN_GAIN = 130f;               // deg/s per unit of torque
    private static final float MAX_SPIN = 900f;               // deg/s
    private static final float FLING_FRICTION = 0.25f;
    private static final float SPIN_FRICTION = 0.5f;
    private static final float POP_IN_SCALE = 0.85f;

    private final TorifudaView[] cards = new TorifudaView[2];
    private final Map<View, List<DynamicAnimation<?>>> running = new HashMap<>();
    private final Set<View> flying = new HashSet<>();
    private final Rect hitRect = new Rect();
    private final float minFlingSpeed;      // px/s
    private final float minDismissSpeed;    // px/s

    private int active;
    private OnSwipeListener listener;
    private boolean interactive;
    private boolean popIn = true;

    private VelocityTracker tracker;
    private boolean dragging;
    private float downX, downY, startTx, startTy, startRotation, grabX, grabY;

    public SwipeDeckView(Context context) { this(context, null); }

    public SwipeDeckView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        minFlingSpeed = 700 * density;
        minDismissSpeed = 1800 * density;
        for (int i = 0; i < cards.length; i++) {
            TorifudaView card = new TorifudaView(context);
            card.setElevation(8 * density);
            card.setOutlineProvider(ViewOutlineProvider.BOUNDS);
            card.setVisibility(INVISIBLE);
            addView(card, new LayoutParams(0, 0, Gravity.CENTER));
            cards[i] = card;
        }
    }

    public void setOnSwipeListener(OnSwipeListener listener) { this.listener = listener; }

    /** false shows new cards at full size immediately (for very short display times). */
    public void setPopIn(boolean popIn) { this.popIn = popIn; }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) cancelDrag();
    }

    /**
     * Shows a new 札 on the active card, popping it in with a small spring.
     * @param upsideDown true to show it the way an opponent's 札 faces you
     */
    public void present(List<String> lines, boolean upsideDown) {
        TorifudaView card = cards[active];
        flying.remove(card);
        cancelAnimations(card);
        reset(card);
        card.setLines(lines);
        card.setUpsideDown(upsideDown);
        card.setVisibility(VISIBLE);
        if (popIn) {
            card.setScaleX(POP_IN_SCALE);
            card.setScaleY(POP_IN_SCALE);
            spring(card, DynamicAnimation.SCALE_X, 1f, 0f);
            spring(card, DynamicAnimation.SCALE_Y, 1f, 0f);
        }

        TorifudaView other = cards[1 - active];
        if (flying.contains(other)) other.bringToFront();   // the thrown card stays on top
    }

    /** Hides the text of the active card (RECOGNITION mode). */
    public void blankActive() {
        cards[active].setLines(Collections.emptyList());
    }

    /** Removes all cards. */
    public void clear() {
        cancelDrag();
        flying.clear();
        for (TorifudaView card : cards) {
            cancelAnimations(card);
            reset(card);
            card.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cardW = Math.min(w * MAX_WIDTH_FRACTION, h * MAX_HEIGHT_FRACTION / FudaCardPainter.ASPECT);
        int width = Math.round(cardW);
        int height = Math.round(cardW * FudaCardPainter.ASPECT);
        post(() -> {
            for (TorifudaView card : cards) {
                LayoutParams lp = (LayoutParams) card.getLayoutParams();
                lp.width = width;
                lp.height = height;
                card.setLayoutParams(lp);
            }
        });
    }

    // --- touch ---

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        TorifudaView card = cards[active];

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (!interactive || card.getVisibility() != VISIBLE) return false;
                card.getHitRect(hitRect);
                if (!hitRect.contains((int) x, (int) y)) return false;
                cancelAnimations(card);
                card.setScaleX(1f);
                card.setScaleY(1f);
                dragging = true;
                downX = x;
                downY = y;
                startTx = card.getTranslationX();
                startTy = card.getTranslationY();
                startRotation = card.getRotation();
                // Where on the card the finger is, relative to its centre.
                grabX = x - (card.getLeft() + startTx + card.getWidth() / 2f);
                grabY = y - (card.getTop() + startTy + card.getHeight() / 2f);
                tracker = VelocityTracker.obtain();
                tracker.addMovement(event);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) return false;
                tracker.addMovement(event);
                float dx = x - downX;
                float dy = y - downY;
                card.setTranslationX(startTx + dx);
                card.setTranslationY(startTy + dy);
                card.setRotation(startRotation + clamp(torque(card, dx, dy) * DRAG_TILT, MAX_TILT));
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (!dragging) return false;
                tracker.addMovement(event);
                tracker.computeCurrentVelocity(1000);
                float vx = tracker.getXVelocity();
                float vy = tracker.getYVelocity();
                endTracking();

                float tx = card.getTranslationX();
                float ty = card.getTranslationY();
                float distance = (float) Math.hypot(tx, ty);
                float speed = (float) Math.hypot(vx, vy);
                boolean movingAway = vx * tx + vy * ty >= 0 || distance < card.getWidth() * 0.1f;
                if ((speed > minFlingSpeed && movingAway) || distance > card.getWidth() * DISMISS_DISTANCE) {
                    throwAway(card, vx, vy);
                } else {
                    if (distance < card.getWidth() * 0.05f) performClick();
                    springBack(card, vx, vy);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    endTracking();
                    springBack(card, 0, 0);
                }
                return true;
            default:
                return dragging;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** r × d (grab point × movement), normalised by card height², so it doesn't depend on screen size. */
    private float torque(View card, float dx, float dy) {
        float h = Math.max(1, card.getHeight());
        return (grabX * dy - grabY * dx) / (h * h);
    }

    // --- physics ---

    private void throwAway(TorifudaView card, float vx, float vy) {
        float speed = (float) Math.hypot(vx, vy);
        if (speed < minDismissSpeed) {
            // Dragged far but released slowly: still send it off, in the direction it was going.
            float tx = card.getTranslationX();
            float ty = card.getTranslationY();
            float d = (float) Math.hypot(tx, ty);
            float dirX, dirY;
            if (speed > 1) { dirX = vx / speed; dirY = vy / speed; }
            else if (d > 1) { dirX = tx / d; dirY = ty / d; }
            else { dirX = 0; dirY = -1; }
            vx = dirX * minDismissSpeed;
            vy = dirY * minDismissSpeed;
        }
        float spin = clamp(torque(card, vx, vy) * SPIN_GAIN, MAX_SPIN);

        flying.add(card);
        fling(card, DynamicAnimation.TRANSLATION_X, vx, FLING_FRICTION, true);
        fling(card, DynamicAnimation.TRANSLATION_Y, vy, FLING_FRICTION, true);
        fling(card, DynamicAnimation.ROTATION, spin, SPIN_FRICTION, false);

        active = 1 - active;
        if (listener != null) listener.onSwiped();
    }

    private void fling(View card, DynamicAnimation.ViewProperty property, float velocity, float friction,
                       boolean watchBounds) {
        FlingAnimation anim = new FlingAnimation(card, property).setStartVelocity(velocity).setFriction(friction);
        if (watchBounds) {
            anim.addUpdateListener((a, value, v) -> {
                if (isOffScreen(card)) post(() -> finishFlight(card));
            });
            anim.addEndListener((a, canceled, value, v) -> {
                if (!canceled) post(() -> finishFlight(card));   // stopped before leaving: just vanish
            });
        }
        track(card, anim);
        anim.start();
    }

    private void springBack(View card, float vx, float vy) {
        spring(card, DynamicAnimation.TRANSLATION_X, 0f, vx);
        spring(card, DynamicAnimation.TRANSLATION_Y, 0f, vy);
        spring(card, DynamicAnimation.ROTATION, 0f, 0f);
    }

    private void spring(View card, DynamicAnimation.ViewProperty property, float target, float velocity) {
        SpringAnimation anim = new SpringAnimation(card, property, target);
        anim.getSpring()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        anim.setStartVelocity(velocity);
        track(card, anim);
        anim.start();
    }

    private void finishFlight(View card) {
        if (!flying.remove(card)) return;
        cancelAnimations(card);
        card.setVisibility(INVISIBLE);
        reset(card);
    }

    private boolean isOffScreen(View card) {
        float cx = card.getLeft() + card.getWidth() / 2f + card.getTranslationX();
        float cy = card.getTop() + card.getHeight() / 2f + card.getTranslationY();
        float r = (float) Math.hypot(card.getWidth(), card.getHeight()) / 2f;
        return cx + r < 0 || cx - r > getWidth() || cy + r < 0 || cy - r > getHeight();
    }

    // --- helpers ---

    private void track(View card, DynamicAnimation<?> anim) {
        List<DynamicAnimation<?>> list = running.get(card);
        if (list == null) {
            list = new ArrayList<>();
            running.put(card, list);
        }
        list.add(anim);
    }

    private void cancelAnimations(View card) {
        List<DynamicAnimation<?>> list = running.remove(card);
        if (list == null) return;
        for (DynamicAnimation<?> a : list) a.cancel();
    }

    private static void reset(@NonNull View card) {
        card.setTranslationX(0);
        card.setTranslationY(0);
        card.setRotation(0);
        card.setScaleX(1);
        card.setScaleY(1);
        card.setAlpha(1);
    }

    private void cancelDrag() {
        if (dragging) endTracking();
    }

    private void endTracking() {
        dragging = false;
        if (tracker != null) {
            tracker.recycle();
            tracker = null;
        }
    }

    private static float clamp(float v, float limit) {
        return Math.max(-limit, Math.min(limit, v));
    }
}
