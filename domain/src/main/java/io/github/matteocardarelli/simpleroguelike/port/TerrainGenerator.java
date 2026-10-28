package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Genera il terreno di un piano. Dove nascono player, nemici e oggetti non lo decide il terreno, ma
 * {@code FloorPopulator}.
 *
 * <p>Contratto: la griglia ha le dimensioni chieste, il bordo è tutto muro, le celle attraversabili
 * sono almeno una e collegate tra loro in 4 direzioni. Il risultato dipende solo dagli argomenti:
 * l'implementazione usa solo {@code random}, mai un generatore suo.
 */
public interface TerrainGenerator {

    Grid<TileType> generate(int width, int height, RandomSource random);
}
