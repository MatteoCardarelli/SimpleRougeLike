package io.github.matteocardarelli.simpleroguelike.ui.input;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Il doppio tap sulla mappa apre l'inventario o annulla la mira. Gli spostamenti non passano da
 * qui: li gestisce {@link HoldToWalk}.
 */
public final class SwipeInputMapper extends GestureDetector.SimpleOnGestureListener {

    private final PlayerInput input;

    public SwipeInputMapper(PlayerInput input) {
        this.input = input;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        input.toggleInventory();
        return true;
    }
}
