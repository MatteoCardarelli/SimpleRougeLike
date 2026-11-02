package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.Optional;

/**
 * L'animazione di un tipo di evento.
 *
 * @param <E> il tipo di evento animato
 */
public interface EventAnimator<E extends GameEvent> {

    /** L'animazione dell'evento sulla scena; vuota se non c'è niente da mostrare. */
    Optional<Animation> animate(E event, Scene scene);
}
