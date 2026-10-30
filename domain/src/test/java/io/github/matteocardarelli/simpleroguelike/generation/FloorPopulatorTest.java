package io.github.matteocardarelli.simpleroguelike.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
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
        assertThat(floor.itemsOnGround().values().iterator().next().kind())
                .isIn(StandardItemCatalog.POTION, StandardItemCatalog.GREATER_POTION);
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
