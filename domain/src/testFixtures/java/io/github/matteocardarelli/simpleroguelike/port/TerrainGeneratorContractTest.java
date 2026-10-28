package io.github.matteocardarelli.simpleroguelike.port;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Il contratto di {@link TerrainGenerator}, per l'adapter vero e per i fake. */
public abstract class TerrainGeneratorContractTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 30;

    /** Un'istanza nuova a ogni chiamata. */
    protected abstract TerrainGenerator createGenerator();

    @Test
    void generate_hasTheRequestedDimensions() {
        Grid<TileType> terrain = generate(createGenerator(), 1);

        assertThat(terrain.width()).isEqualTo(WIDTH);
        assertThat(terrain.height()).isEqualTo(HEIGHT);
    }

    @Test
    void generate_borderIsWall() {
        Grid<TileType> terrain = generate(createGenerator(), 2);

        for (Position cell : terrain.positions()) {
            boolean border =
                    cell.x() == 0
                            || cell.y() == 0
                            || cell.x() == WIDTH - 1
                            || cell.y() == HEIGHT - 1;
            if (border) {
                assertThat(terrain.get(cell)).as("%s", cell).isEqualTo(TileType.WALL);
            }
        }
    }

    @Test
    void generate_walkableCellsAreAllConnected() {
        for (long seed = 1; seed <= 5; seed++) {
            Grid<TileType> terrain = generate(createGenerator(), seed);
            List<Position> walkable = new ArrayList<>();
            for (Position cell : terrain.positions()) {
                if (terrain.get(cell).isWalkable()) {
                    walkable.add(cell);
                }
            }

            assertThat(walkable).as("seed %d", seed).isNotEmpty();
            assertThat(reachableFrom(terrain, walkable.get(0)).size())
                    .as("seed %d", seed)
                    .isEqualTo(walkable.size());
        }
    }

    @Test
    void generate_dependsOnlyOnItsArguments() {
        Grid<TileType> first = generate(createGenerator(), 42);
        TerrainGenerator used = createGenerator();
        generate(used, 7);
        used.generate(20, 20, new SeededRandomSource(8));

        assertThat(generate(createGenerator(), 42)).isEqualTo(first);
        assertThat(generate(used, 42)).isEqualTo(first);
    }

    private static Grid<TileType> generate(TerrainGenerator generator, long seed) {
        return generator.generate(WIDTH, HEIGHT, new SeededRandomSource(seed));
    }

    private static CellSet reachableFrom(Grid<TileType> terrain, Position start) {
        List<Position> reached = new ArrayList<>();
        boolean[] seen = new boolean[terrain.width() * terrain.height()];
        ArrayDeque<Position> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.y() * terrain.width() + start.x()] = true;
        while (!queue.isEmpty()) {
            Position cell = queue.poll();
            reached.add(cell);
            for (Direction direction : Direction.values()) {
                Position next = cell.translate(direction);
                if (terrain.isInside(next)
                        && terrain.get(next).isWalkable()
                        && !seen[next.y() * terrain.width() + next.x()]) {
                    seen[next.y() * terrain.width() + next.x()] = true;
                    queue.add(next);
                }
            }
        }
        return CellSet.of(terrain.width(), terrain.height(), reached);
    }
}
