package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Footprint;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArenaTest {

    private static final Footprint ROOM = Footprint.circle(8);

    /** Un piano 40×30 con stanze 6×4 collegate da corridoi, come quelli generati. */
    private static Grid<TileType> rooms() {
        String[] rows = new String[30];
        for (int y = 0; y < 30; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < 40; x++) {
                boolean border = x == 0 || y == 0 || x == 39 || y == 29;
                boolean room = x % 8 >= 1 && x % 8 <= 6 && y % 6 >= 1 && y % 6 <= 4;
                boolean corridor = y % 6 == 2 || x % 8 == 3;
                row.append(!border && (room || corridor) ? '.' : '#');
            }
            rows[y] = row.toString();
        }
        return FloorFixtures.terrain(rows);
    }

    @Test
    void build_makesTheRoomOfTheBossesShape_withTheBossInTheMiddle() {
        for (long seed = 1; seed <= 40; seed++) {
            Arena arena = Arena.build(rooms(), ROOM, 4, new SeededRandomSource(seed));

            assertThat(arena.interior()).hasSize(52);
            Position corner = corner(arena.interior());
            assertThat(arena.interior()).containsExactlyInAnyOrderElementsOf(ROOM.at(corner));
            assertThat(arena.bossAnchor()).isEqualTo(new Position(corner.x() + 2, corner.y() + 2));
            assertThat(arena.interior()).containsAll(Footprint.circle(4).at(arena.bossAnchor()));
            for (Position cell : arena.interior()) {
                assertThat(arena.terrain().get(cell).isWalkable()).isTrue();
            }
        }
    }

    @Test
    void build_leadsToTheRoomOnlyThroughTheDoor_andAFiveCellCorridorBetweenWalls() {
        for (long seed = 1; seed <= 40; seed++) {
            Arena arena = Arena.build(rooms(), ROOM, 4, new SeededRandomSource(seed));
            Grid<TileType> terrain = arena.terrain();

            assertThat(arena.corridor()).hasSize(Arena.CORRIDOR);
            assertThat(arena.door().isAdjacentTo(arena.corridor().get(0))).isTrue();
            assertThat(arena.interior()).anyMatch(cell -> cell.isAdjacentTo(arena.door()));
            List<Position> path = new ArrayList<>();
            path.add(arena.door());
            path.addAll(arena.corridor());
            for (int i = 0; i < arena.corridor().size(); i++) {
                Position cell = arena.corridor().get(i);
                assertThat(walkableAround(terrain, cell))
                        .as("seed %d, corridoio %d", seed, i)
                        .hasSize(2)
                        .contains(path.get(i));
            }
            for (Position cell : arena.interior()) {
                for (Position next : walkableAround(terrain, cell)) {
                    assertThat(arena.interior().contains(next) || next.equals(arena.door()))
                            .as("seed %d: %s accanto alla stanza", seed, next)
                            .isTrue();
                }
            }
        }
    }

    @Test
    void build_keepsTheRestOfTheFloorAwayFromTheRoomsWalls_andAllOfItReachable() {
        for (long seed = 1; seed <= 40; seed++) {
            Arena arena = Arena.build(rooms(), ROOM, 4, new SeededRandomSource(seed));
            Grid<TileType> terrain = arena.terrain();
            Set<Position> known = new LinkedHashSet<>(arena.interior());
            known.add(arena.door());
            known.addAll(arena.corridor());

            for (Position cell : terrain.positions()) {
                if (!terrain.get(cell).isWalkable() || known.contains(cell)) {
                    continue;
                }
                assertThat(arena.outside()).as("seed %d: %s", seed, cell).contains(cell);
                for (Position room : arena.interior()) {
                    assertThat(
                                    Math.max(
                                            Math.abs(cell.x() - room.x()),
                                            Math.abs(cell.y() - room.y())))
                            .as("seed %d: %s vicino alla stanza", seed, cell)
                            .isGreaterThan(2);
                }
            }
            assertThat(arena.outside()).hasSizeGreaterThanOrEqualTo(Arena.MIN_OUTSIDE);
        }
    }

    @Test
    void build_onAFloorTooSmall_fails() {
        Grid<TileType> small = FloorFixtures.terrain("#########", "#.......#", "#########");

        assertThatThrownBy(() -> Arena.build(small, ROOM, 4, new SeededRandomSource(1)))
                .isInstanceOf(FloorGenerationException.class);
    }

    private static Position corner(Set<Position> cells) {
        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        for (Position cell : cells) {
            x = Math.min(x, cell.x());
            y = Math.min(y, cell.y());
        }
        return new Position(x, y);
    }

    private static List<Position> walkableAround(Grid<TileType> terrain, Position cell) {
        List<Position> around = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position next = cell.translate(direction);
            if (terrain.isInside(next) && terrain.get(next).isWalkable()) {
                around.add(next);
            }
        }
        return around;
    }
}
