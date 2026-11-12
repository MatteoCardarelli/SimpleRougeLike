package io.github.matteocardarelli.simpleroguelike.ai;

import static io.github.matteocardarelli.simpleroguelike.ai.Behaviors.enemy;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorFixtures;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.port.StraightLinePathfinder;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import org.junit.jupiter.api.Test;

class SurroundBehaviorTest {

    private final SurroundBehavior surround = new SurroundBehavior(new StraightLinePathfinder());

    @Test
    void decide_nextToThePlayer_attacks() {
        Floor floor = FloorFixtures.parse("@w");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_twoWolvesOnTheSameSide_splitTheCellsAroundThePlayer() {
        // Il player è al centro; i due lupi arrivano da destra. Il primo prende la cella a destra
        // del player, il secondo deve aggirarlo verso un'altra cella libera.
        Floor floor = FloorFixtures.parse(".....", "..@.w", "....w", ".....");

        assertThat(decide(floor, 2)).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(decide(floor, 3)).isNotEqualTo(new StepIntent(Direction.UP));
    }

    @Test
    void decide_isTheSameEveryTime() {
        Floor floor = FloorFixtures.parse(".....", "..@.w", "....w", ".....");

        assertThat(decide(floor, 3)).isEqualTo(decide(floor, 3));
    }

    @Test
    void decide_playerDead_waits() {
        Floor floor = FloorFixtures.parse("@..w");
        floor.damage(new EntityId(1), 1000);

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    @Test
    void decide_twoPacks_doNotShareTheCellsAroundThePlayer() {
        // Intorno al player restano libere solo la cella a sinistra e quella a destra. Il lupo in
        // (4, 2) prende quella a destra; quello in (3, 4), se è del suo branco, ripiega su quella a
        // sinistra, se è di un altro branco punta anche lui a quella a destra.
        String[] rows = {".....", "..#..", "..@.w", "..#..", "...w."};
        Floor samePack = FloorFixtures.parse(rows);
        rows[4] = "...W.";
        Floor twoPacks = FloorFixtures.parse(rows);

        assertThat(decide(samePack, 2)).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(decide(samePack, 3)).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(decide(twoPacks, 2)).isEqualTo(new StepIntent(Direction.LEFT));
        assertThat(decide(twoPacks, 3)).isEqualTo(new StepIntent(Direction.UP));
    }

    @Test
    void decide_aFarMember_huntsWhenACompanionSeesThePlayer() {
        // Il primo lupo (id 2) è a 2 passi dal player; il secondo (id 3) a 19, oltre la sua
        // distanza di inseguimento (10), ma caccia con il branco.
        Floor floor = FloorFixtures.parse("@.w..................w");

        assertThat(decide(floor, 3)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_aLoneWolfFarAway_waitsLikeBefore() {
        Floor floor = FloorFixtures.parse("@.............W");

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
    }

    @Test
    void decide_withoutThePlayer_aStrayMemberGoesBackToTheLeader_theOthersWait() {
        Floor floor = FloorFixtures.parse("@......................", "..............w.w....w.");

        assertThat(decide(floor, 2)).isEqualTo(new WaitIntent());
        assertThat(decide(floor, 3)).isEqualTo(new WaitIntent());
        assertThat(decide(floor, 4)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    @Test
    void decide_whenTheLeaderDies_theNextMemberLeads() {
        Floor floor = FloorFixtures.parse("@......................", "..............w.w....w.");
        floor.damage(new EntityId(2), 1000);

        assertThat(decide(floor, 3)).isEqualTo(new WaitIntent());
        assertThat(decide(floor, 4)).isEqualTo(new StepIntent(Direction.LEFT));
    }

    private EnemyIntent decide(Floor floor, int enemyId) {
        return surround.decide(enemy(floor, enemyId), floor, new SeededRandomSource(1));
    }
}
