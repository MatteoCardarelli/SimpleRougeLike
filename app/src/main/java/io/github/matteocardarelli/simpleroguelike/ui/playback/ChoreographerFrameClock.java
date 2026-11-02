package io.github.matteocardarelli.simpleroguelike.ui.playback;

import android.view.Choreographer;
import java.util.function.LongConsumer;

/** I fotogrammi del telefono, allineati al ridisegno dello schermo. */
public final class ChoreographerFrameClock implements FrameClock {

    @Override
    public void requestFrame(LongConsumer onFrame) {
        Choreographer.getInstance()
                .postFrameCallback(frameTimeNanos -> onFrame.accept(frameTimeNanos / 1_000_000L));
    }
}
