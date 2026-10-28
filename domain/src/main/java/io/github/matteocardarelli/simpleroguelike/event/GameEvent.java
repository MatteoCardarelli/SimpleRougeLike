package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;

/**
 * Un fatto accaduto durante un turno. Gli eventi di un turno sono nell'ordine in cui sono accaduti:
 * lo stato finale dice com'è la partita, gli eventi dicono come ci si è arrivati.
 *
 * <p>Un evento porta id, {@code Kind} e posizioni, mai testi: le parole le sceglie l'interfaccia.
 */
public interface GameEvent {

    /**
     * Le celle in cui l'evento è avvenuto, usate per nasconderlo se il giocatore non le vede.
     *
     * @return vuota per un evento globale, sempre visibile
     */
    List<Position> where();
}
