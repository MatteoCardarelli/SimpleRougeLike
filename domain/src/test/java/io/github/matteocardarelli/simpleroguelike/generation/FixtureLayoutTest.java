package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class FixtureLayoutTest {

    /** Una stanza grande e una piccola, unite da un corridoio di tre celle nella riga 2. */
    private static final Grid<TileType> TWO_ROOMS =
            FloorFixtures.terrain(
                    "##############",
                    "#......###...#",
                    "#............#",
                    "#......###...#",
                    "#......#######",
                    "##############");

    private static final FloorSpec SPEC = new FloorSpec(14, 6, 0, 0, 0, 1, 1, 1, 1, 1);

    @Test
    void plan_putsTheGateInTheCorridor_theChestInTheClosedRoom_andTheLeverOutside() {
        for (long seed = 1; seed <= 20; seed++) {
            FixtureLayout layout =
                    FixtureLayout.plan(TWO_ROOMS, SPEC, new SeededRandomSource(seed));

            assertThat(layout.gates()).hasSize(1);
            FixtureLayout.Gate gate = layout.gates().get(0);
            assertThat(gate.at().y()).isEqualTo(2);
            assertThat(gate.at().x()).isBetween(7, 9);
            assertThat(gate.zone()).allMatch(cell -> cell.x() > gate.at().x());
            assertThat(TWO_ROOMS.get(gate.lever())).isEqualTo(TileType.WALL);
            assertThat(layout.chests()).anyMatch(gate.zone()::contains);
            assertThat(layout.closed()).isEqualTo(gate.zone());
            assertThat(nextToTheOuterSide(gate)).isTrue();
        }
    }

    private static boolean nextToTheOuterSide(FixtureLayout.Gate gate) {
        for (Direction direction : Direction.values()) {
            Position next = gate.lever().translate(direction);
            if (TWO_ROOMS.isInside(next)
                    && TWO_ROOMS.get(next).isWalkable()
                    && next.x() < gate.at().x()) {
                return true;
            }
        }
        return false;
    }

    @Test
    void plan_withoutACorridor_hasNoGateAndNoDoor() {
        Grid<TileType> open = FloorFixtures.terrain("#####", "#...#", "#...#", "#####");

        FixtureLayout layout =
                FixtureLayout.plan(
                        open,
                        new FloorSpec(5, 4, 0, 0, 0, 1, 2, 0, 1, 0),
                        new SeededRandomSource(1));

        assertThat(layout.gates()).isEmpty();
        assertThat(layout.doors()).isEmpty();
    }

    @Test
    void plan_putsTheStairsOutsideCorridorsAndClosedZones() {
        for (long seed = 1; seed <= 20; seed++) {
            FixtureLayout layout =
                    FixtureLayout.plan(TWO_ROOMS, SPEC, new SeededRandomSource(seed));

            assertThat(layout.stairs()).isPresent();
            Position stairs = layout.stairs().get();
            assertThat(layout.closed()).doesNotContain(stairs);
            assertThat(stairs.y() == 2 && stairs.x() >= 7 && stairs.x() <= 9).isFalse();
            assertThat(layout.chests()).doesNotContain(stairs);
        }
    }

    @Test
    void plan_isDeterministic() {
        assertThat(FixtureLayout.plan(TWO_ROOMS, SPEC, new SeededRandomSource(7)))
                .isEqualTo(FixtureLayout.plan(TWO_ROOMS, SPEC, new SeededRandomSource(7)));
    }
}
