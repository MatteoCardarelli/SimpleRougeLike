package io.github.matteocardarelli.simpleroguelike.generation;

import org.jspecify.annotations.Nullable;

/** Un terreno non permette di rispettare la specifica del piano, per esempio è troppo piccolo. */
public final class FloorGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FloorGenerationException(String message) {
        super(message);
    }

    public FloorGenerationException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
