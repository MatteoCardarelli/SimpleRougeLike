package io.github.matteocardarelli.simpleroguelike.port;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Il contratto di {@link Pathfinder}. Lo estendono l'adapter vero e i fake usati nei test, così i
 * test che usano un fake non si reggono su un comportamento diverso da quello del gioco.
 */
public abstract class PathfinderContractTest {

    private static final Grid<TileType> ROOM =
            FloorFixtures.terrain(
                    "#######", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", "#######");

    /** Un'istanza nuova a ogni chiamata. */
    protected abstract Pathfinder createPathfinder();

    @Test
    void nextStep_sameCell_isEmpty() {
        Position cell = new Position(3, 3);

        assertThat(createPathfinder().nextStep(ROOM, none(ROOM), cell, cell)).isEmpty();
    }

    @Test
    void nextStep_adjacentTarget_stepsOntoIt() {
        Position from = new Position(3, 3);

        for (Direction direction : Direction.values()) {
            Position to = from.translate(direction);

            assertThat(createPathfinder().nextStep(ROOM, none(ROOM), from, to))
                    .as("verso %s", direction)
                    .contains(direction);
        }
    }

    @Test
    void nextStep_straightCorridor_goesTowardTheTarget() {
        Grid<TileType> corridor = FloorFixtures.terrain("#######", "#.....#", "#######");

        Optional<Direction> step =
                createPathfinder()
                        .nextStep(corridor, none(corridor), new Position(1, 1), new Position(5, 1));

        assertThat(step).contains(Direction.RIGHT);
    }

    @Test
    void nextStep_noFreeNeighbour_isEmpty() {
        Grid<TileType> pocket = FloorFixtures.terrain("#####", "#...#", "#####");
        CellSet blocked = CellSet.of(5, 3, List.of(new Position(2, 1)));

        assertThat(
                        createPathfinder()
                                .nextStep(pocket, blocked, new Position(1, 1), new Position(3, 1)))
                .isEmpty();
    }

    @Test
    void nextStep_neverEntersWallsOrBlockedCells() {
        Grid<TileType> terrain =
                FloorFixtures.terrain("#######", "#.#...#", "#.#.#.#", "#...#.#", "#######");
        CellSet blocked = CellSet.of(7, 5, List.of(new Position(1, 2), new Position(3, 3)));
        Position to = new Position(5, 3);

        for (Position from : terrain.positions()) {
            if (!terrain.get(from).isWalkable() || blocked.contains(from) || from.equals(to)) {
                continue;
            }
            Optional<Direction> step = createPathfinder().nextStep(terrain, blocked, from, to);
            if (step.isPresent()) {
                Position next = from.translate(step.get());
                assertThat(terrain.isInside(next)).as("da %s", from).isTrue();
                assertThat(terrain.get(next).isWalkable()).as("da %s", from).isTrue();
                assertThat(blocked.contains(next)).as("da %s", from).isFalse();
            }
        }
    }

    @Test
    void nextStep_dependsOnlyOnItsArguments() {
        Position from = new Position(1, 1);
        Position to = new Position(5, 5);
        Optional<Direction> first = createPathfinder().nextStep(ROOM, none(ROOM), from, to);
        Pathfinder used = createPathfinder();
        used.nextStep(ROOM, none(ROOM), new Position(5, 1), new Position(1, 5));
        used.nextStep(ROOM, CellSet.of(7, 7, List.of(new Position(2, 1))), from, to);

        assertThat(createPathfinder().nextStep(ROOM, none(ROOM), from, to)).isEqualTo(first);
        assertThat(used.nextStep(ROOM, none(ROOM), from, to)).isEqualTo(first);
        assertThat(used.nextStep(ROOM, none(ROOM), from, to)).isEqualTo(first);
    }

    private static CellSet none(Grid<TileType> terrain) {
        return CellSet.empty(terrain.width(), terrain.height());
    }
}
