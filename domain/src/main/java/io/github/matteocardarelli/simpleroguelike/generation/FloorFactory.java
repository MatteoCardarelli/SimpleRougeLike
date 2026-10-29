package io.github.matteocardarelli.simpleroguelike.generation;

import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Crea un piano: terreno dalla porta, poi il popolamento. Se un terreno non basta per la specifica
 * ne genera un altro dallo stesso flusso casuale, fino a {@code maxAttempts} tentativi: i tentativi
 * restano deterministici.
 */
public final class FloorFactory implements FloorSource {

    private final TerrainGenerator terrain;
    private final FloorPopulator populator;

    public FloorFactory(TerrainGenerator terrain, FloorPopulator populator) {
        this.terrain = terrain;
        this.populator = populator;
    }

    /**
     * Crea il piano.
     *
     * @throws FloorGenerationException se nessun terreno, in {@code maxAttempts} tentativi, basta
     *     per la specifica; la causa è l'errore dell'ultimo tentativo
     */
    @Override
    public Floor create(FloorSpec spec, RandomSource random, EntityIdSequence ids) {
        FloorGenerationException last = null;
        for (int attempt = 0; attempt < spec.maxAttempts(); attempt++) {
            Grid<TileType> generated = terrain.generate(spec.width(), spec.height(), random);
            try {
                return populator.populate(generated, spec, random, ids);
            } catch (FloorGenerationException e) {
                last = e;
            }
        }
        throw new FloorGenerationException(
                "Nessun terreno valido in " + spec.maxAttempts() + " tentativi", last);
    }
}
