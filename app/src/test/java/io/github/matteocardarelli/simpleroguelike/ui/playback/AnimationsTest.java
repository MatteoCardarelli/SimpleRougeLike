package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimationsTest {

    private static final EntityId HERO = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final Position C = new Position(5, 5);

    private final ManualFrameClock clock = new ManualFrameClock();
    private final List<GameEvent> heard = new ArrayList<>();
    private final Scene scene = new Scene(SceneTest.PLAYER);
    private final AnimatedEventPlayer player =
            Animations.registerAll(new AnimatedEventPlayer(heard::add, clock, () -> true));

    private Sprite hero() {
        return scene.sprite(HERO).orElseThrow();
    }

    private void playToEnd(GameEvent event) {
        player.play(List.of(event), scene, () -> {});
        clock.frame(0);
        clock.frame(1000);
    }

    @Test
    void toward_givesTheFourDirections() {
        assertThat(Facings.toward(C, new Position(6, 5))).contains(Direction.RIGHT);
        assertThat(Facings.toward(C, new Position(4, 5))).contains(Direction.LEFT);
        assertThat(Facings.toward(C, new Position(5, 6))).contains(Direction.DOWN);
        assertThat(Facings.toward(C, new Position(5, 4))).contains(Direction.UP);
    }

    @Test
    void toward_sameCell_isEmpty() {
        assertThat(Facings.toward(C, C)).isEmpty();
    }

    @Test
    void toward_tie_prefersHorizontal() {
        assertThat(Facings.toward(C, new Position(6, 6))).contains(Direction.RIGHT);
        assertThat(Facings.toward(C, new Position(4, 4))).contains(Direction.LEFT);
    }

    @Test
    void toward_longerVerticalAxis_wins() {
        assertThat(Facings.toward(C, new Position(6, 8))).contains(Direction.DOWN);
        assertThat(Facings.toward(C, new Position(4, 2))).contains(Direction.UP);
    }

    @Test
    void step_turnsTheSpriteTowardTheNewCell() {
        scene.spawn(HERO, SceneTest.PLAYER, C);
        Position[] targets = {
            new Position(6, 5), new Position(5, 5), new Position(5, 6), new Position(5, 5)
        };
        Direction[] expected = {Direction.RIGHT, Direction.LEFT, Direction.DOWN, Direction.UP};
        Position at = C;
        for (int i = 0; i < targets.length; i++) {
            playToEnd(new Moved(HERO, SceneTest.PLAYER, at, targets[i]));
            assertThat(hero().facing()).isEqualTo(expected[i]);
            at = targets[i];
        }
    }

    @Test
    void step_walkFramesAlternateBetweenTheTwoHalfCycles() {
        scene.spawn(HERO, SceneTest.PLAYER, C);
        Position next = new Position(6, 5);

        player.play(List.of(new Moved(HERO, SceneTest.PLAYER, C, next)), scene, () -> {});
        assertThat(hero().walkFrame()).isEqualTo(0);
        clock.frame(0);
        clock.frame(Animations.STEP_MS / 2);
        assertThat(hero().walkFrame()).isEqualTo(1);
        clock.frame(Animations.STEP_MS);
        assertThat(hero().walkFrame()).isEqualTo(-1);

        player.play(List.of(new Moved(HERO, SceneTest.PLAYER, next, C)), scene, () -> {});
        assertThat(hero().walkFrame()).isEqualTo(3);
        clock.frame(0);
        clock.frame(Animations.STEP_MS * 9 / 10);
        assertThat(hero().walkFrame()).isEqualTo(5);
        clock.frame(Animations.STEP_MS * 2);
        assertThat(hero().walkFrame()).isEqualTo(-1);
    }

    @Test
    void step_finishMidAnimation_standsAndCountsTheStepOnce() {
        scene.spawn(HERO, SceneTest.PLAYER, C);
        player.play(
                List.of(new Moved(HERO, SceneTest.PLAYER, C, new Position(6, 5))), scene, () -> {});
        clock.frame(0);
        clock.frame(Animations.STEP_MS / 3);

        player.finish();

        assertThat(hero().walkFrame()).isEqualTo(-1);
        assertThat(hero().x()).isEqualTo(6f);
        assertThat(hero().steps()).isEqualTo(1);
    }

    @Test
    void bump_turnsTheAttackerTowardTheTarget() {
        scene.spawn(HERO, SceneTest.PLAYER, C);
        scene.spawn(GOBLIN, SceneTest.PLAYER, new Position(5, 4));

        playToEnd(new Attacked(HERO, GOBLIN, C, new Position(5, 4)));

        assertThat(hero().facing()).isEqualTo(Direction.UP);
    }
}
