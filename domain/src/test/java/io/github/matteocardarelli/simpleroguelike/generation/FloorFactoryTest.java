package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.port.FixedTerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FloorFactoryTest {

    private static final FloorPopulator POPULATOR =
            new FloorPopulator(
                    new StandardItemCatalog(),
                    new StandardEnemyCatalog(),
                    StandardPlayer.DEFINITION);

    @Test
    void create_firstTerrainIsEnough_usesIt() {
        FloorFactory factory = new FloorFactory(new FixedTerrainGenerator(), POPULATOR);

        Floor floor =
                factory.create(
                        new FloorSpec(12, 8, 3, 2, 2, 1),
                        new SeededRandomSource(1),
                        new EntityIdSequence());

        assertThat(floor.terrain().width()).isEqualTo(12);
        assertThat(floor.enemies()).hasSize(3);
    }

    @Test
    void create_badTerrain_triesAnotherOne() {
        CountingGenerator generator = new CountingGenerator(2);
        FloorFactory factory = new FloorFactory(generator, POPULATOR);

        Floor floor =
                factory.create(
                        new FloorSpec(12, 8, 3, 0, 2, 5),
                        new SeededRandomSource(1),
                        new EntityIdSequence());

        assertThat(generator.calls).isEqualTo(3);
        assertThat(floor.enemies()).hasSize(3);
    }

    @Test
    void create_tooManyBadTerrains_failsWithTheLastCause() {
        FloorFactory factory = new FloorFactory(new CountingGenerator(10), POPULATOR);

        assertThatThrownBy(
                        () ->
                                factory.create(
                                        new FloorSpec(12, 8, 3, 0, 2, 3),
                                        new SeededRandomSource(1),
                                        new EntityIdSequence()))
                .isInstanceOf(FloorGenerationException.class)
                .hasMessageContaining("3 tentativi")
                .hasCauseInstanceOf(FloorGenerationException.class);
    }

    @Test
    void create_sameSeed_givesSameFloor() {
        FloorFactory factory = new FloorFactory(new FixedTerrainGenerator(), POPULATOR);
        FloorSpec spec = new FloorSpec(12, 8, 3, 2, 2, 1);

        Floor first = factory.create(spec, new SeededRandomSource(5), new EntityIdSequence());
        Floor second = factory.create(spec, new SeededRandomSource(5), new EntityIdSequence());

        assertThat(first.positionOf(first.playerId()))
                .isEqualTo(second.positionOf(second.playerId()));
        assertThat(first.itemsOnGround().keySet())
                .containsExactlyElementsOf(second.itemsOnGround().keySet());
    }

    /** Restituisce terreni senza pavimento per {@code badOnes} volte, poi una stanza. */
    private static final class CountingGenerator implements TerrainGenerator {
        private final int badOnes;
        private int calls;

        CountingGenerator(int badOnes) {
            this.badOnes = badOnes;
        }

        @Override
        public Grid<TileType> generate(int width, int height, RandomSource random) {
            calls++;
            if (calls <= badOnes) {
                List<TileType> rock = new ArrayList<>();
                for (int i = 0; i < width * height; i++) {
                    rock.add(TileType.WALL);
                }
                return new Grid<>(width, height, rock);
            }
            return new FixedTerrainGenerator().generate(width, height, random);
        }
    }
}
