package io.github.matteocardarelli.simpleroguelike.pathfinding;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.port.PathfinderContractTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Il contratto di {@link Pathfinder}, più il comportamento su piani veri. */
class BreadthFirstPathfinderTest extends PathfinderContractTest {

    private final BreadthFirstPathfinder pathfinder = new BreadthFirstPathfinder();

    @Override
    protected Pathfinder createPathfinder() {
        return new BreadthFirstPathfinder();
    }

    /** Bug #1: i nemici non inseguivano il player, perché il percorso trovato era sempre vuoto. */
    @Test
    void nextStep_wallInTheWay_goesAroundIt() {
        Grid<TileType> terrain =
                FloorFixtures.terrain("#######", "#..#..#", "#..#..#", "#.....#", "#######");

        int steps = walk(terrain, none(terrain), new Position(1, 1), new Position(5, 1));

        assertThat(steps).isEqualTo(8);
    }

    @Test
    void nextStep_otherActorInTheCorridor_goesAroundIt() {
        Grid<TileType> terrain = FloorFixtures.terrain("#####", "#...#", "#...#", "#####");
        CellSet blocked = CellSet.of(5, 4, List.of(new Position(2, 1)));

        Optional<Direction> step =
                pathfinder.nextStep(terrain, blocked, new Position(1, 1), new Position(3, 1));

        assertThat(step).contains(Direction.DOWN);
    }

    @Test
    void nextStep_unreachableTarget_isEmpty() {
        Grid<TileType> terrain = FloorFixtures.terrain("#######", "#..#..#", "#######");

        assertThat(
                        pathfinder.nextStep(
                                terrain, none(terrain), new Position(1, 1), new Position(5, 1)))
                .isEmpty();
    }

    @Test
    void nextStep_twoEnemiesInTheSameTurn_bothGetAStep() {
        Grid<TileType> terrain = FloorFixtures.terrain("#######", "#.....#", "#.....#", "#######");
        Position player = new Position(3, 1);
        Position first = new Position(1, 1);
        Position second = new Position(5, 2);

        Optional<Direction> firstStep =
                pathfinder.nextStep(terrain, CellSet.of(7, 4, List.of(second)), first, player);
        Optional<Direction> secondStep =
                pathfinder.nextStep(terrain, CellSet.of(7, 4, List.of(first)), second, player);

        assertThat(firstStep).contains(Direction.RIGHT);
        assertThat(secondStep).isPresent();
        assertThat(second.translate(secondStep.get()).distanceTo(player))
                .isLessThan(second.distanceTo(player));
    }

    @Test
    void nextStep_followedToTheEnd_takesTheShortestPath() {
        Grid<TileType> terrain =
                FloorFixtures.terrain(
                        "##########",
                        "#........#",
                        "#.######.#",
                        "#.#....#.#",
                        "#.#.##.#.#",
                        "#...#....#",
                        "##########");

        assertThat(walk(terrain, none(terrain), new Position(1, 1), new Position(5, 3)))
                .isEqualTo(10);
    }

    /** Segue i passi fino all'arrivo e restituisce quanti ne servono. */
    private int walk(Grid<TileType> terrain, CellSet blocked, Position from, Position to) {
        Position at = from;
        int steps = 0;
        while (!at.equals(to)) {
            Optional<Direction> step = pathfinder.nextStep(terrain, blocked, at, to);
            assertThat(step).as("da %s", at).isPresent();
            at = at.translate(step.get());
            assertThat(terrain.get(at).isWalkable()).as("%s", at).isTrue();
            steps++;
            assertThat(steps).isLessThan(terrain.width() * terrain.height());
        }
        return steps;
    }

    private static CellSet none(Grid<TileType> terrain) {
        return CellSet.empty(terrain.width(), terrain.height());
    }
}
