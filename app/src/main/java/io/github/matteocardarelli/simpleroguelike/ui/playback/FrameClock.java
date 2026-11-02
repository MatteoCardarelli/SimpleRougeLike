package io.github.matteocardarelli.simpleroguelike.ui.playback;

import java.util.function.LongConsumer;

/**
 * Chi scandisce i fotogrammi delle animazioni: sul telefono il {@code Choreographer}, nei test un
 * orologio mosso a mano.
 */
public interface FrameClock {

    /** Chiama {@code onFrame} al prossimo fotogramma, con l'ora del fotogramma in millisecondi. */
    void requestFrame(LongConsumer onFrame);
}
