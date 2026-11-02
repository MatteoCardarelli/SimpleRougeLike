package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Da dove arriva il piano di una partita. Nel gioco è {@link FloorFactory}; nei test può essere un
 * piano disegnato a mano, per giocare turni completi su una situazione nota.
 */
public interface FloorSource {

    /**
     * Crea il piano. Gli id di attori e oggetti vengono da {@code ids}, il caso solo da {@code
     * random}.
     *
     * @throws FloorGenerationException se non si riesce a creare un piano per la specifica
     */
    Floor create(FloorSpec spec, RandomSource random, EntityIdSequence ids);

    /**
     * Crea il piano successivo, con il player che arriva da quello di prima: stesse statistiche,
     * salute e inventario.
     *
     * @throws FloorGenerationException se non si riesce a creare un piano per la specifica
     */
    Floor createNext(FloorSpec spec, RandomSource random, EntityIdSequence ids, Player player);
}
