package io.github.matteocardarelli.simpleroguelike.rules;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static io.github.matteocardarelli.simpleroguelike.rules.Rules.ACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import org.junit.jupiter.api.Test;

class ActionRulesTest {

    private static final EntityId PLAYER = new EntityId(1);

    @Test
    void step_intoFreeCell_moves() {
        Floor floor = FloorFixtures.parse("@.");

        ActionResult result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.performed()).isTrue();
        assertThat(result.events())
                .containsExactly(
                        new Moved(PLAYER, Player.KIND, new Position(0, 0), new Position(1, 0)));
    }

    @Test
    void step_playerOntoItem_picksItUp() {
        Floor floor = FloorFixtures.parse("@!");

        ActionResult result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events())
                .containsExactly(
                        new Moved(PLAYER, Player.KIND, new Position(0, 0), new Position(1, 0)),
                        new ItemPickedUp(new EntityId(2), Kind.of("potion"), new Position(1, 0)));
    }

    @Test
    void step_enemyOntoItem_leavesItThere() {
        Floor floor = FloorFixtures.parse("@g!");
        EntityId goblin = new EntityId(2);

        ActionResult result = ACTIONS.step(floor, goblin, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events()).hasSize(1).first().isInstanceOf(Moved.class);
        assertThat(floor.itemAt(new Position(2, 0))).isPresent();
    }

    @Test
    void step_intoWall_isBlocked() {
        Floor floor = FloorFixtures.parse("#@");

        ActionResult result = ACTIONS.step(floor, PLAYER, Direction.LEFT, ALWAYS_HIT);

        assertThat(result).isEqualTo(ActionResult.blocked());
        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(1, 0));
    }

    @Test
    void step_outsideTheFloor_isBlocked() {
        Floor floor = FloorFixtures.parse("@");

        assertThat(ACTIONS.step(floor, PLAYER, Direction.UP, ALWAYS_HIT).performed()).isFalse();
    }

    @Test
    void step_intoHostile_attacks() {
        Floor floor = FloorFixtures.parse("@g");
        EntityId goblin = new EntityId(2);

        ActionResult result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events())
                .startsWith(
                        new Attacked(PLAYER, goblin, new Position(0, 0), new Position(1, 0)),
                        new Damaged(goblin, new Position(1, 0), 9, 21));
        assertThat(floor.positionOf(PLAYER)).isEqualTo(new Position(0, 0));
    }

    @Test
    void step_enemyIntoPlayer_attacksWithTheSameRule() {
        Floor floor = FloorFixtures.parse("@g");
        EntityId goblin = new EntityId(2);

        ActionResult result = ACTIONS.step(floor, goblin, Direction.LEFT, ALWAYS_HIT);

        assertThat(result.events())
                .containsExactly(
                        new Attacked(goblin, PLAYER, new Position(1, 0), new Position(0, 0)),
                        new Damaged(PLAYER, new Position(0, 0), 7, 93));
    }

    @Test
    void step_intoAlly_isBlocked() {
        Floor floor = FloorFixtures.parse("@gg");

        ActionResult result = ACTIONS.step(floor, new EntityId(2), Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.performed()).isFalse();
        assertThat(floor.enemies()).allMatch(enemy -> enemy.health() == 30);
    }

    @Test
    void step_ontoCorpse_moves() {
        Floor floor = FloorFixtures.parse("@g");
        floor.damage(new EntityId(2), 1000);

        ActionResult result = ACTIONS.step(floor, PLAYER, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events()).hasSize(1).first().isInstanceOf(Moved.class);
    }

    @Test
    void step_byDeadActor_isRejected() {
        Floor floor = FloorFixtures.parse("@g.");
        floor.damage(new EntityId(2), 1000);

        assertThatThrownBy(() -> ACTIONS.step(floor, new EntityId(2), Direction.RIGHT, ALWAYS_HIT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actionResult_notPerformedWithEvents_isRejected() {
        assertThatThrownBy(
                        () ->
                                new ActionResult(
                                        false,
                                        java.util.List.of(
                                                new Moved(
                                                        PLAYER,
                                                        Player.KIND,
                                                        new Position(0, 0),
                                                        new Position(1, 0)))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
