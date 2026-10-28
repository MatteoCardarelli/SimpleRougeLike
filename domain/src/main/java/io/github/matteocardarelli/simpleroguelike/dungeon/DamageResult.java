package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Il risultato di {@link Floor#damage}.
 *
 * @param applied la salute tolta davvero, al massimo quella che l'attore aveva
 * @param killed se l'attore è morto per questo danno
 * @param events {@code Damaged} se è stata tolta salute, seguito da {@code Died} se l'attore è
 *     morto
 */
public record DamageResult(
        int applied, int remainingHealth, boolean killed, List<GameEvent> events) {

    public DamageResult {
        events = List.copyOf(events);
    }
}
