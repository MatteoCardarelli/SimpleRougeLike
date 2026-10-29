package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import java.util.List;

/**
 * Il risultato di una partita nuova o di un comando.
 *
 * @param revision cresce a ogni risultato, anche se il comando è rifiutato o il turno non passa:
 *     identifica il risultato, così l'interfaccia non riproduce due volte le stesse animazioni
 * @param accepted falso se la fase della partita non accettava il comando
 * @param events gli eventi del turno che il player può vedere, nell'ordine in cui sono accaduti
 */
public record TurnResult(
        long revision, boolean accepted, List<GameEvent> events, GameSnapshot snapshot) {

    public TurnResult {
        events = List.copyOf(events);
    }
}
