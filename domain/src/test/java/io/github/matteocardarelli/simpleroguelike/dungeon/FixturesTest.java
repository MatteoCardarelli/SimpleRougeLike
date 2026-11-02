package io.github.matteocardarelli.simpleroguelike.dungeon;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FixturesTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final ActionRules ACTIONS =
            new ActionRules(
                    new CombatRules(new DamageRules(new LevelingRules(new Stats(20, 2), 150))));

    @Test
    void closedDoor_blocksMovementAndSight_andABumpOpensIt() {
        Floor floor = FloorFixtures.parse("@+.");
        Position door = new Position(1, 0);

        assertThat(floor.isWalkable(door)).isFalse();
        assertThat(floor.movementGrid().get(door)).isEqualTo(TileType.WALL);
        assertThat(floor.sightGrid().get(door)).isEqualTo(TileType.WALL);

        var result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.performed()).isTrue();
        assertThat(result.events()).containsExactly(new DoorOpened(door));
        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(0, 0));
        assertThat(floor.isWalkable(door)).isTrue();
        assertThat(floor.sightGrid().get(door)).isEqualTo(TileType.FLOOR);

        ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(floor.positionOf(PLAYER)).isEqualTo(door);
    }

    @Test
    void closedDoor_stopsEnemies() {
        Floor floor = FloorFixtures.parse("@.+g");

        assertThat(ACTIONS.step(floor, new EntityId(2), Direction.LEFT, ALWAYS_HIT).performed())
                .isFalse();
        assertThat(floor.fixtureAt(new Position(2, 0)).orElseThrow().isOpen()).isFalse();
    }

    @Test
    void lever_opensItsGates_once() {
        Floor floor = FloorFixtures.parse("#L#", ".@G", "...");
        Position gate = new Position(2, 1);

        assertThat(floor.isWalkable(gate)).isFalse();
        assertThat(floor.sightGrid().get(gate)).isEqualTo(TileType.FLOOR);
        assertThat(ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT).performed()).isFalse();

        var pulled = ACTIONS.step(floor, PLAYER, Direction.UP, ALWAYS_HIT);

        assertThat(pulled.events())
                .containsExactly(new LeverPulled(new Position(1, 0), List.of(gate)));
        assertThat(floor.isWalkable(gate)).isTrue();
        assertThat(ACTIONS.step(floor, PLAYER, Direction.UP, ALWAYS_HIT).performed()).isFalse();
    }

    @Test
    void chest_opens_leavesItsItem_andThenLetsThePlayerIn() {
        Floor floor = FloorFixtures.parse("@$");
        Position chest = new Position(1, 0);

        var opened = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(opened.events())
                .containsExactly(new ChestOpened(chest, Optional.of(StandardItemCatalog.POTION)));
        assertThat(floor.itemAt(chest)).isPresent();
        assertThat(floor.fixtureAt(chest).orElseThrow().contents()).isEmpty();
        assertThat(ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT).events())
                .anyMatch(ItemPickedUp.class::isInstance);
    }

    @Test
    void interact_onlyNextToThePlayer() {
        Floor floor = FloorFixtures.parse("@.+");

        assertThatThrownBy(() -> floor.interact(PLAYER, new Position(2, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(floor.interact(PLAYER, new Position(1, 0))).isEmpty();
    }

    @Test
    void builder_rejectsMisplacedFixtures() {
        Grid<TileType> terrain = FloorFixtures.terrain("#..", "...");
        Player player = new Player(PLAYER, FloorFixtures.PLAYER_STATS, 100, 5);

        assertThatThrownBy(
                        () ->
                                new FloorBuilder()
                                        .terrain(terrain)
                                        .player(player, new Position(1, 0))
                                        .fixture(Fixture.lever(List.of()), new Position(2, 0))
                                        .build())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                new FloorBuilder()
                                        .terrain(terrain)
                                        .player(player, new Position(1, 0))
                                        .fixture(Fixture.door(), new Position(0, 0))
                                        .build())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                new FloorBuilder()
                                        .terrain(terrain)
                                        .player(player, new Position(1, 0))
                                        .fixture(Fixture.door(), new Position(1, 0))
                                        .build())
                .isInstanceOf(IllegalStateException.class);
    }
}
