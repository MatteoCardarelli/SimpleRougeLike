package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import java.util.List;

/**
 * Cosa fa un oggetto quando si usa. Un effetto modifica il piano solo con i metodi pubblici di
 * {@link Floor} e restituisce gli eventi di quello che è successo.
 */
public interface ItemEffect {

    /** Applica l'effetto e restituisce gli eventi prodotti, vuoti se non è cambiato niente. */
    List<GameEvent> apply(EffectContext context);
}
