package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Calcola le celle visibili da una posizione.
 *
 * <p>Contratto: il risultato ha le dimensioni del terreno, contiene l'origine e le celle adiacenti,
 * muri compresi, e nessuna cella oltre {@code radius} in nessuna delle due direzioni. Dipende solo
 * dagli argomenti.
 */
public interface FieldOfView {

    CellSet visibleFrom(Grid<TileType> terrain, Position origin, int radius);
}
