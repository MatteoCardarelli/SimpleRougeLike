package io.github.matteocardarelli.simpleroguelike.ui.input;

import android.view.GestureDetector;
import android.view.MotionEvent;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * I gesti sulla mappa: uno swipe va nella sua direzione (un passo, o un lancio se si sta mirando),
 * un doppio tap apre l'inventario o annulla la mira. La direzione è quella dello spostamento più
 * lungo, orizzontale o verticale.
 */
public final class SwipeInputMapper extends GestureDetector.SimpleOnGestureListener {

    /** La velocità minima di uno swipe, in pixel al secondo, sotto la quale è un trascinamento. */
    static final float MIN_VELOCITY_PX = 100;

    private final PlayerInput input;
    private final float minSwipeDistancePx;

    public SwipeInputMapper(PlayerInput input, float minSwipeDistancePx) {
        this.input = input;
        this.minSwipeDistancePx = minSwipeDistancePx;
    }

    @Override
    public boolean onFling(
            @Nullable MotionEvent start, MotionEvent end, float velocityX, float velocityY) {
        if (start == null) {
            return false;
        }
        Optional<Direction> direction =
                direction(
                        end.getX() - start.getX(),
                        end.getY() - start.getY(),
                        velocityX,
                        velocityY,
                        minSwipeDistancePx);
        direction.ifPresent(input::swipe);
        return direction.isPresent();
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        input.toggleInventory();
        return true;
    }

    /**
     * La direzione di uno swipe, se lo è: abbastanza lungo e abbastanza veloce lungo l'asse
     * principale.
     */
    static Optional<Direction> direction(
            float dx, float dy, float velocityX, float velocityY, float minDistance) {
        if (Math.abs(dx) > Math.abs(dy)) {
            if (Math.abs(dx) > minDistance && Math.abs(velocityX) > MIN_VELOCITY_PX) {
                return Optional.of(dx > 0 ? Direction.RIGHT : Direction.LEFT);
            }
        } else if (Math.abs(dy) > minDistance && Math.abs(velocityY) > MIN_VELOCITY_PX) {
            return Optional.of(dy > 0 ? Direction.DOWN : Direction.UP);
        }
        return Optional.empty();
    }
}
