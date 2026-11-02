package io.github.matteocardarelli.simpleroguelike.ui.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * Un orologio dei fotogrammi mosso a mano: ogni {@link #frame} chiama chi ha chiesto il prossimo.
 */
final class ManualFrameClock implements FrameClock {

    private List<LongConsumer> waiting = new ArrayList<>();

    @Override
    public void requestFrame(LongConsumer onFrame) {
        waiting.add(onFrame);
    }

    /** Un fotogramma all'ora {@code nowMs}. */
    void frame(long nowMs) {
        List<LongConsumer> now = waiting;
        waiting = new ArrayList<>();
        for (LongConsumer onFrame : now) {
            onFrame.accept(nowMs);
        }
    }

    boolean hasPendingFrame() {
        return !waiting.isEmpty();
    }
}
