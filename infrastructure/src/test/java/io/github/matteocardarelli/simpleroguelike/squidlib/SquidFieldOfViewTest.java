package io.github.matteocardarelli.simpleroguelike.squidlib;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.FieldOfView;
import io.github.matteocardarelli.simpleroguelike.port.FieldOfViewContractTest;
import org.junit.jupiter.api.Test;

/** Il contratto di {@link FieldOfView}, più il comportamento con i muri. */
class SquidFieldOfViewTest extends FieldOfViewContractTest {

    @Override
    protected FieldOfView createFieldOfView() {
        return new SquidFieldOfView();
    }

    @Test
    void visibleFrom_doesNotSeeThroughWalls() {
        Grid<TileType> terrain =
                FloorFixtures.terrain(
                        "#########", "#...#...#", "#...#...#", "#...#...#", "#########");

        CellSet visible = new SquidFieldOfView().visibleFrom(terrain, new Position(2, 2), 8);

        assertThat(visible.contains(new Position(4, 2))).as("il muro si vede").isTrue();
        assertThat(visible.contains(new Position(5, 2))).as("dietro il muro").isFalse();
        assertThat(visible.contains(new Position(7, 2))).isFalse();
        assertThat(visible.contains(new Position(1, 1))).isTrue();
    }

    @Test
    void visibleFrom_radiusIsACircle() {
        Grid<TileType> terrain =
                FloorFixtures.terrain(
                        "###########",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "#.........#",
                        "###########");
        Position origin = new Position(5, 5);

        CellSet visible = new SquidFieldOfView().visibleFrom(terrain, origin, 3);

        assertThat(visible.contains(new Position(8, 5))).as("a 3 in linea").isTrue();
        assertThat(visible.contains(new Position(8, 8))).as("a 3 in diagonale").isFalse();
    }

    @Test
    void visibleFrom_newTerrain_isNotConfusedWithTheCachedOne() {
        SquidFieldOfView fov = new SquidFieldOfView();
        Grid<TileType> open = FloorFixtures.terrain("#####", "#...#", "#####");
        Grid<TileType> split = FloorFixtures.terrain("#####", "#.#.#", "#####");

        fov.visibleFrom(open, new Position(1, 1), 5);
        CellSet visible = fov.visibleFrom(split, new Position(1, 1), 5);

        assertThat(visible.contains(new Position(3, 1))).isFalse();
    }
}
