package io.github.matteocardarelli.simpleroguelike.ui.input;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldToWalkTest {

    private static final float DISTANCE = 100;
    private static final EntityId GOBLIN = new EntityId(7);

    private HoldToWalk walk;
    private long revision;

    @BeforeEach
    void setUp() {
        walk = new HoldToWalk(DISTANCE);
        shown(true, false, Set.of());
    }

    private void shown(boolean progressed, boolean damaged, Set<EntityId> enemies) {
        walk.onTurnShown(new TurnSummary(++revision, progressed, damaged, enemies));
    }

    @Test
    void drag_pastTheThreshold_stepsAtOnce() {
        walk.press(500, 500);
        assertThat(walk.drag(550, 500, 10, false)).isEmpty();
        assertThat(walk.drag(601, 520, 20, false)).hasValue(Direction.RIGHT);
        assertThat(walk.drag(620, 520, 30, false)).isEmpty();
    }

    @Test
    void drag_followsTheLongerAxisOfTheDisplacement() {
        walk.press(500, 500);
        assertThat(walk.drag(480, 620, 0, false)).hasValue(Direction.DOWN);
        assertThat(walk.drag(500, 380, 10, false)).hasValue(Direction.UP);
        assertThat(walk.drag(380, 500, 20, false)).hasValue(Direction.LEFT);
    }

    @Test
    void repeat_waitsForTheHoldDelayAfterTheStep() {
        walk.press(0, 0);
        walk.drag(200, 0, 1000, false);
        shown(true, false, Set.of());
        assertThat(walk.repeatWait(1050)).isEqualTo(150);
        assertThat(walk.repeat(1050)).isEmpty();
        assertThat(walk.repeatWait(1200)).isZero();
        assertThat(walk.repeat(1200)).hasValue(Direction.RIGHT);
    }

    @Test
    void repeat_afterTheDelay_isImmediateAtEachTurn() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of());
        assertThat(walk.repeat(500)).hasValue(Direction.RIGHT);
        assertThat(walk.repeatWait(500)).isEqualTo(-1);
        shown(true, false, Set.of());
        assertThat(walk.repeat(700)).hasValue(Direction.RIGHT);
    }

    @Test
    void quickFlick_isASingleStep() {
        walk.press(0, 0);
        assertThat(walk.drag(200, 0, 0, false)).hasValue(Direction.RIGHT);
        shown(true, false, Set.of());
        assertThat(walk.repeatWait(100)).isEqualTo(100);
        walk.release();
        assertThat(walk.repeatWait(300)).isEqualTo(-1);
        assertThat(walk.repeat(300)).isEmpty();
    }

    @Test
    void directionChange_stepsInTheNewDirectionAndRepeatsIt() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of());
        assertThat(walk.drag(0, 200, 300, false)).hasValue(Direction.DOWN);
        shown(true, false, Set.of());
        assertThat(walk.repeatWait(350)).isEqualTo(150);
        assertThat(walk.repeat(500)).hasValue(Direction.DOWN);
    }

    @Test
    void backToTheCentre_stopsRepeating() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of());
        assertThat(walk.drag(10, 0, 100, false)).isEmpty();
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        assertThat(walk.drag(200, 0, 1100, false)).hasValue(Direction.RIGHT);
    }

    @Test
    void release_stopsEverything() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of());
        walk.release();
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        assertThat(walk.drag(300, 0, 1000, false)).isEmpty();
    }

    @Test
    void blockedStep_stopsUntilADifferentDirection() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(false, false, Set.of());
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        // Tornare al centro e riandare nella stessa direzione non basta.
        walk.drag(0, 0, 1100, false);
        assertThat(walk.drag(200, 0, 1200, false)).isEmpty();
        assertThat(walk.repeatWait(2000)).isEqualTo(-1);
        assertThat(walk.drag(0, 200, 2100, false)).hasValue(Direction.DOWN);
        shown(true, false, Set.of());
        assertThat(walk.repeat(2400)).hasValue(Direction.DOWN);
    }

    @Test
    void blockedStep_resumesAfterLiftingAndPressingAgain() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(false, false, Set.of());
        walk.release();
        walk.press(0, 0);
        assertThat(walk.drag(200, 0, 100, false)).hasValue(Direction.RIGHT);
    }

    @Test
    void newVisibleEnemy_stopsUntilRelease() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of(GOBLIN));
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        assertThat(walk.drag(0, 200, 1100, false)).isEmpty();
        walk.release();
        walk.press(0, 0);
        assertThat(walk.drag(0, 200, 1200, false)).hasValue(Direction.DOWN);
    }

    @Test
    void enemyAlreadyVisible_doesNotStop() {
        shown(true, false, Set.of(GOBLIN));
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, false, Set.of(GOBLIN));
        assertThat(walk.repeat(300)).hasValue(Direction.RIGHT);
    }

    @Test
    void damageToThePlayer_stopsUntilRelease() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        shown(true, true, Set.of());
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        assertThat(walk.drag(0, -200, 1100, false)).isEmpty();
    }

    @Test
    void aiming_throwsOnceAndNothingRepeats() {
        walk.press(0, 0);
        assertThat(walk.drag(200, 0, 0, true)).hasValue(Direction.RIGHT);
        shown(true, false, Set.of());
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
        assertThat(walk.drag(0, 200, 1100, false)).isEmpty();
    }

    @Test
    void stepNotYetShown_doesNotRepeat() {
        walk.press(0, 0);
        walk.drag(200, 0, 0, false);
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
    }

    @Test
    void tapWithoutMovement_doesNothing() {
        walk.press(0, 0);
        assertThat(walk.drag(30, 20, 10, false)).isEmpty();
        assertThat(walk.repeatWait(1000)).isEqualTo(-1);
    }
}
