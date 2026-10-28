package io.github.matteocardarelli.simpleroguelike.squidlib;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorPopulator;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGeneratorContractTest;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

/** Il contratto di {@link TerrainGenerator}, più l'uso con il popolamento vero. */
class SquidTerrainGeneratorTest extends TerrainGeneratorContractTest {

    @Override
    protected TerrainGenerator createGenerator() {
        return new SquidTerrainGenerator();
    }

    @Test
    void generate_differentSeeds_giveDifferentMaps() {
        Grid<TileType> first =
                new SquidTerrainGenerator().generate(40, 30, new SeededRandomSource(1));
        Grid<TileType> second =
                new SquidTerrainGenerator().generate(40, 30, new SeededRandomSource(2));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void floorFactory_withSquidTerrain_alwaysPopulatesAFloor() {
        FloorFactory factory =
                new FloorFactory(
                        new SquidTerrainGenerator(),
                        new FloorPopulator(
                                new StandardItemCatalog(),
                                new StandardEnemyCatalog(),
                                StandardPlayer.DEFINITION));
        FloorSpec spec = new FloorSpec(50, 50, 5, 3, 8, 3);

        for (long seed = 1; seed <= 30; seed++) {
            Floor floor =
                    factory.create(spec, new SeededRandomSource(seed), new EntityIdSequence());

            assertThat(floor.enemies()).as("seed %d", seed).hasSize(5);
            assertThat(floor.itemsOnGround()).as("seed %d", seed).hasSize(3);
        }
    }
}
