package io.github.matteocardarelli.simpleroguelike.dungeon;

import static io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource.ALWAYS_HIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.ai.JumpIntent;
import io.github.matteocardarelli.simpleroguelike.ai.LandIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RestIntent;
import io.github.matteocardarelli.simpleroguelike.ai.SlimeBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.WaitIntent;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.random.FixedRandomSource;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.JumpRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import org.junit.jupiter.api.Test;

class GiantSlimeTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId SLIME = new EntityId(2);
    private static final DamageRules DAMAGE =
            new DamageRules(new LevelingRules(new Stats(20, 2), 150));
    private static final ActionRules ACTIONS = new ActionRules(new CombatRules(DAMAGE));
    private static final JumpRules JUMPS = new JumpRules(DAMAGE);
    private static final SlimeBehavior SLIME_AI = new SlimeBehavior();

    /**
     * Il player in (1, 1), lo slime con la cella di riferimento in (7, 1): il cerchio va da x 7 a
     * 10 e da y 1 a 4.
     */
    private static Floor arena() {
        return FloorFixtures.parse(
                "............",
                ".@.....S....",
                "............",
                "............",
                "............",
                "............",
                "............");
    }

    @Test
    void slime_occupiesACircleOfTwelveCells_withoutTheCorners() {
        Floor floor = arena();

        assertThat(floor.cellsOf(SLIME)).hasSize(12);
        assertThat(floor.actorAt(new Position(8, 1))).map(Actor::id).contains(SLIME);
        assertThat(floor.actorAt(new Position(7, 2))).map(Actor::id).contains(SLIME);
        assertThat(floor.actorAt(new Position(7, 1))).isEmpty();
        assertThat(floor.isFree(new Position(5, 3))).isTrue();
    }

    @Test
    void player_bumpingAnyCellOfTheSlime_attacksIt() {
        // Lo slime è letto prima del player: si usano gli id del piano.
        Floor floor = FloorFixtures.parse(".S...", ".....", "@....", ".....", ".....");
        EntityId player = floor.playerId();
        EntityId slime = floor.enemies().get(0).id();

        var result = ACTIONS.step(floor, player, Direction.RIGHT, ALWAYS_HIT);

        assertThat(result.events())
                .anyMatch(e -> e instanceof Damaged d && d.target().equals(slime));
        assertThat(floor.positionOf(player)).isEqualTo(new Position(0, 2));
    }

    @Test
    void behavior_announcesAJumpOnThePlayer_landsNextTurn_thenRests() {
        Floor floor = arena();
        Enemy slime = floor.enemy(SLIME).orElseThrow();

        var intent = SLIME_AI.decide(slime, floor, new FixedRandomSource(1));

        assertThat(intent).isInstanceOf(JumpIntent.class);
        Position anchor = ((JumpIntent) intent).anchor();
        assertThat(slime.footprint().at(anchor)).contains(new Position(1, 1));
        assertThat(floor.announceJump(SLIME, anchor)).isInstanceOf(JumpAnnounced.class);
        assertThat(SLIME_AI.decide(slime, floor, new FixedRandomSource(1)))
                .isEqualTo(new LandIntent());

        var landing = JUMPS.land(floor, SLIME);

        assertThat(landing.get(0)).isInstanceOf(Landed.class);
        assertThat(landing).anyMatch(Moved.class::isInstance);
        assertThat(landing).anyMatch(e -> e instanceof Damaged d && d.target().equals(PLAYER));
        assertThat(floor.cellsOf(SLIME)).doesNotContain(floor.positionOf(PLAYER));
        assertThat(SLIME_AI.decide(slime, floor, new FixedRandomSource(1)))
                .isEqualTo(new RestIntent());
        floor.rest(SLIME);
        assertThat(SLIME_AI.decide(slime, floor, new FixedRandomSource(1)))
                .isNotEqualTo(new RestIntent());
    }

    @Test
    void land_whenThePlayerHasMoved_doesNoHarm() {
        Floor floor = arena();
        Position anchor =
                ((JumpIntent)
                                SLIME_AI.decide(
                                        floor.enemy(SLIME).orElseThrow(),
                                        floor,
                                        new FixedRandomSource(1)))
                        .anchor();
        floor.announceJump(SLIME, anchor);
        for (int i = 0; i < 5; i++) {
            floor.move(PLAYER, Direction.DOWN);
        }

        var landing = JUMPS.land(floor, SLIME);

        assertThat(landing).singleElement().isInstanceOf(Landed.class);
        assertThat(floor.player().health()).isEqualTo(floor.player().stats().maxHealth());
    }

    @Test
    void behavior_farFromThePlayer_waits() {
        Floor floor =
                FloorFixtures.parse(
                        "@.................................",
                        "..................S...............",
                        "..................................",
                        "..................................",
                        "..................................");

        assertThat(
                        SLIME_AI.decide(
                                floor.enemy(SLIME).orElseThrow(), floor, new FixedRandomSource(1)))
                .isEqualTo(new WaitIntent());
    }

    @Test
    void bossDefeated_whenTheSlimeDies_evenAfterItIsRemoved() {
        Floor floor = arena();

        assertThat(floor.bossId()).contains(SLIME);
        assertThat(floor.isBossDefeated()).isFalse();
        floor.damage(SLIME, 1000);
        assertThat(floor.isBossDefeated()).isTrue();
        floor.removeDead();
        assertThat(floor.isBossDefeated()).isTrue();
    }

    @Test
    void footprint_circleOfFour_hasTwelveCellsInAFourByFourSquare() {
        Footprint circle = Footprint.circle(4);

        assertThat(circle.offsets()).hasSize(12).doesNotContain(new Position(0, 0));
        assertThat(circle.size()).isEqualTo(4);
        assertThat(Footprint.SINGLE.at(new Position(3, 4))).containsExactly(new Position(3, 4));
    }
}
