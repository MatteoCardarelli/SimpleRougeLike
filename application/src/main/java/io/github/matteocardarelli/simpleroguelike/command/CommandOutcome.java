package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Il risultato di un comando.
 *
 * @param turnConsumed se è passato del tempo e quindi tocca ai nemici; urtare un muro non fa
 *     passare il turno
 */
public record CommandOutcome(List<GameEvent> events, boolean turnConsumed) {

    private static final CommandOutcome NONE = new CommandOutcome(List.of(), false);

    public CommandOutcome {
        events = List.copyOf(events);
    }

    /** Non è successo niente e il turno non passa. */
    public static CommandOutcome none() {
        return NONE;
    }
}
