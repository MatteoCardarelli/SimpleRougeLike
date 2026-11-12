package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.FixtureType;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FloorPopulatorTest {

    private static final FloorPopulator POPULATOR =
            new FloorPopulator(
                    new StandardItemCatalog(),
                    new StandardEnemyCatalog(),
                    StandardPlayer.DEFINITION);

    private static final Grid<TileType> ROOM =
            FloorFixtures.terrain(
                    "##########",
                    "#........#",
                    "#........#",
                    "#...##...#",
                    "#........#",
                    "#........#",
                    "##########");

    @Test
    void populate_placesEveryoneOnDistinctWalkableCells() {
        Floor floor = populate(new FloorSpec(10, 7, 4, 3, 3, 1), 1);

        List<Position> actors = new ArrayList<>();
        actors.add(floor.positionOf(floor.playerId()));
        for (Enemy enemy : floor.enemies()) {
            actors.add(floor.positionOf(enemy.id()));
        }
        assertThat(floor.enemies()).hasSize(4);
        assertThat(floor.itemsOnGround()).hasSize(3);
        assertThat(actors).doesNotHaveDuplicates().allMatch(floor::isWalkable);
        assertThat(floor.itemsOnGround().keySet())
                .allMatch(floor::isWalkable)
                .doesNotContainAnyElementsOf(actors);
    }

    @Test
    void populate_enemiesStartFarFromThePlayer() {
        for (long seed = 1; seed <= 20; seed++) {
            Floor floor = populate(new FloorSpec(10, 7, 3, 0, 5, 1), seed);
            Position player = floor.positionOf(floor.playerId());

            for (Enemy enemy : floor.enemies()) {
                assertThat(floor.positionOf(enemy.id()).distanceTo(player))
                        .as("seed %d", seed)
                        .isGreaterThanOrEqualTo(5);
            }
        }
    }

    @Test
    void populate_idsArePlayerThenEnemiesThenItems() {
        Floor floor = populate(new FloorSpec(10, 7, 2, 2, 1, 1), 3);

        assertThat(floor.playerId()).isEqualTo(new EntityId(1));
        assertThat(floor.enemies())
                .extracting(Enemy::id)
                .containsExactly(new EntityId(2), new EntityId(3));
        assertThat(floor.itemsOnGround().values())
                .extracting(Item::id)
                .containsExactlyInAnyOrder(new EntityId(4), new EntityId(5));
    }

    @Test
    void populate_usesThePlayerDefinitionAndTheCatalogs() {
        Floor floor = populate(new FloorSpec(10, 7, 1, 1, 1, 1), 4);

        assertThat(floor.player().stats()).isEqualTo(StandardPlayer.DEFINITION.stats());
        assertThat(floor.player().inventory().capacity()).isEqualTo(10);
        assertThat(floor.enemies().get(0).kind()).isEqualTo(StandardEnemyCatalog.GOBLIN);
        var spawned = floor.itemsOnGround().values().iterator().next().definition();
        assertThat(new StandardItemCatalog().all()).contains(spawned);
        assertThat(spawned.spawnWeight()).isPositive();
    }

    @Test
    void populate_sameSeed_givesSameFloor() {
        FloorSpec spec = new FloorSpec(10, 7, 4, 3, 2, 1);

        assertThat(describe(populate(spec, 9))).isEqualTo(describe(populate(spec, 9)));
        assertThat(describe(populate(spec, 9))).isNotEqualTo(describe(populate(spec, 10)));
    }

    @Test
    void populate_tooManyEnemiesForTheDistance_fails_withoutUsingIds() {
        EntityIdSequence ids = new EntityIdSequence();

        assertThatThrownBy(
                        () ->
                                POPULATOR.populate(
                                        ROOM,
                                        new FloorSpec(10, 7, 2, 0, 50, 1),
                                        new SeededRandomSource(1),
                                        ids))
                .isInstanceOf(FloorGenerationException.class);
        assertThat(ids.next()).isEqualTo(new EntityId(1));
    }

    @Test
    void populate_tooManyItems_fails() {
        assertThatThrownBy(() -> populate(new FloorSpec(10, 7, 0, 100, 0, 1), 1))
                .isInstanceOf(FloorGenerationException.class);
    }

    @Test
    void populate_aPack_spawnsTogether_andTheTotalStaysTheSpecOne() {
        FloorPopulator wolves =
                new FloorPopulator(
                        new StandardItemCatalog(),
                        () -> java.util.List.of(FloorFixtures.wolf()),
                        StandardPlayer.DEFINITION);
        FloorSpec spec = new FloorSpec(10, 7, 3, 0, 0, 1);

        Floor floor =
                wolves.populate(ROOM, spec, new SeededRandomSource(5), new EntityIdSequence());

        assertThat(floor.enemies()).hasSize(3);
        Position first = floor.positionOf(floor.enemies().get(0).id());
        for (var wolf : floor.enemies()) {
            assertThat(floor.positionOf(wolf.id()).distanceTo(first))
                    .isLessThanOrEqualTo(FloorPopulator.PACK_RADIUS);
        }
    }

    @Test
    void populate_theKeyOfALockedDoor_isOnThePlayerSide() {
        Grid<TileType> twoRooms =
                FloorFixtures.terrain(
                        "##############",
                        "#......###...#",
                        "#............#",
                        "#......###...#",
                        "#......#######",
                        "##############");
        FloorSpec spec = new FloorSpec(14, 6, 0, 0, 0, 1, 1, 0, 0, 0, 1);

        for (long seed = 1; seed <= 20; seed++) {
            Floor floor =
                    POPULATOR.populate(
                            twoRooms, spec, new SeededRandomSource(seed), new EntityIdSequence());
            Position door =
                    floor.fixtures().entrySet().stream()
                            .filter(e -> e.getValue().type() == FixtureType.LOCKED_DOOR)
                            .map(java.util.Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow();
            Position player = floor.positionOf(floor.playerId());
            Position key =
                    floor.itemsOnGround().entrySet().stream()
                            .filter(e -> e.getValue().kind().equals(StandardItemCatalog.KEY))
                            .map(java.util.Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow();
            assertThat(Integer.signum(key.x() - door.x()))
                    .isEqualTo(Integer.signum(player.x() - door.x()));
        }
    }

    /** Dietro una porta chiusa a chiave, una volta su quattro, c'è un oggetto raro. */
    @Test
    void populate_behindALockedDoor_sometimesARareItem_onTheOtherSide() {
        Grid<TileType> twoRooms =
                FloorFixtures.terrain(
                        "##############",
                        "#......###...#",
                        "#............#",
                        "#......###...#",
                        "#......#######",
                        "##############");
        FloorSpec spec = new FloorSpec(14, 6, 0, 0, 0, 1, 1, 0, 0, 0, 1);

        int rewarded = 0;
        int floors = 200;
        for (long seed = 1; seed <= floors; seed++) {
            Floor floor =
                    POPULATOR.populate(
                            twoRooms, spec, new SeededRandomSource(seed), new EntityIdSequence());
            Position door =
                    floor.fixtures().entrySet().stream()
                            .filter(e -> e.getValue().type() == FixtureType.LOCKED_DOOR)
                            .map(java.util.Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow();
            Position player = floor.positionOf(floor.playerId());
            for (var entry : floor.itemsOnGround().entrySet()) {
                if (entry.getValue().definition().key().isPresent()) {
                    continue;
                }
                rewarded++;
                assertThat(entry.getValue().definition().spawnWeight())
                        .isBetween(1, FloorPopulator.RARE_WEIGHT);
                assertThat(Integer.signum(entry.getKey().x() - door.x()))
                        .isEqualTo(-Integer.signum(player.x() - door.x()));
            }
        }
        assertThat(rewarded).isBetween(floors / 8, floors * 3 / 8);
    }

    @Test
    void populate_enemiesHaveTheLevelOfTheSpec() {
        FloorSpec spec = new FloorSpec(10, 7, 3, 0, 3, 1, 0, 0, 0, 0, 0, 0, 0, 4);

        Floor floor = populate(spec, 1);

        assertThat(floor.enemies()).isNotEmpty().allMatch(enemy -> enemy.level() == 4);
    }

    @Test
    void populate_theBossFloor_hasTheArenaTheBossItsDoorAndItsKey() {
        StringBuilder wall = new StringBuilder();
        StringBuilder open = new StringBuilder("#");
        for (int x = 0; x < 40; x++) {
            wall.append('#');
        }
        for (int x = 0; x < 38; x++) {
            open.append('.');
        }
        open.append('#');
        String[] rows = new String[30];
        for (int y = 0; y < 30; y++) {
            rows[y] = y == 0 || y == 29 ? wall.toString() : open.toString();
        }
        Grid<TileType> terrain = FloorFixtures.terrain(rows);
        FloorSpec spec = new FloorSpec(40, 30, 5, 3, 0, 1, 0, 0, 0, 0, 0, 1);

        for (long seed = 1; seed <= 10; seed++) {
            Floor floor =
                    POPULATOR.populate(
                            terrain, spec, new SeededRandomSource(seed), new EntityIdSequence());

            assertThat(floor.bossId()).isPresent();
            var boss = floor.enemy(floor.bossId().get()).orElseThrow();
            assertThat(boss.kind()).isEqualTo(StandardEnemyCatalog.GIANT_SLIME);
            assertThat(floor.fixtures().values())
                    .filteredOn(f -> f.type() == FixtureType.BOSS_DOOR)
                    .hasSize(1);
            assertThat(floor.itemsOnGround().values())
                    .filteredOn(i -> i.kind().equals(StandardItemCatalog.BOSS_KEY))
                    .hasSize(1);
            Position player = floor.positionOf(floor.playerId());
            assertThat(floor.cellsOf(boss.id()).get(0).distanceTo(player)).isGreaterThan(1);
            assertThat(floor.enemies()).hasSize(6);
            assertThat(floor.bossRoom().size()).isEqualTo(52);
            assertThat(floor.bossRoom().positions()).containsAll(floor.cellsOf(boss.id()));
            assertThat(floor.bossRoom().contains(player)).isFalse();
            for (var enemy : floor.enemies()) {
                if (!enemy.isBoss()) {
                    assertThat(floor.bossRoom().positions())
                            .doesNotContainAnyElementsOf(floor.cellsOf(enemy.id()));
                }
            }
        }
    }

    @Test
    void populate_noWalkableCell_fails() {
        Grid<TileType> rock = FloorFixtures.terrain("###", "###", "###");

        assertThatThrownBy(
                        () ->
                                POPULATOR.populate(
                                        rock,
                                        new FloorSpec(3, 3, 0, 0, 0, 1),
                                        new SeededRandomSource(1),
                                        new EntityIdSequence()))
                .isInstanceOf(FloorGenerationException.class);
    }

    private static Floor populate(FloorSpec spec, long seed) {
        return POPULATOR.populate(ROOM, spec, new SeededRandomSource(seed), new EntityIdSequence());
    }

    private static List<String> describe(Floor floor) {
        List<String> lines = new ArrayList<>();
        lines.add("player " + floor.positionOf(floor.playerId()));
        floor.enemies().forEach(e -> lines.add(e.id() + " " + floor.positionOf(e.id())));
        floor.itemsOnGround().forEach((at, item) -> lines.add(item.id() + " " + at));
        return lines;
    }
}
