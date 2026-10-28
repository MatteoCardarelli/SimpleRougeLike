package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Il risultato di un'azione.
 *
 * @param performed falso se l'azione non è avvenuta, per esempio contro un muro: in quel caso non
 *     ci sono eventi e il turno non passa
 */
public record ActionResult(boolean performed, List<GameEvent> events) {

    private static final ActionResult BLOCKED = new ActionResult(false, List.of());

    public ActionResult {
        events = List.copyOf(events);
        if (!performed && !events.isEmpty()) {
            throw new IllegalArgumentException("Un'azione non avvenuta non produce eventi");
        }
    }

    public static ActionResult performed(List<GameEvent> events) {
        return new ActionResult(true, events);
    }

    public static ActionResult blocked() {
        return BLOCKED;
    }
}
