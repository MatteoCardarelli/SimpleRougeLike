package io.github.matteocardarelli.simpleroguelike.ui.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AnimatedEventPlayerTest extends EventPlayerContractTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final Kind GOBLIN_KIND = Kind.of("goblin");
    private static final Position A = new Position(1, 1);
    private static final Position B = new Position(2, 1);

    private final ManualFrameClock clock = new ManualFrameClock();
    private final List<GameEvent> heard = new ArrayList<>();
    private final AtomicInteger finished = new AtomicInteger();

    @Override
    EventPlayer create(EventListener listener) {
        return Animations.registerAll(
                new AnimatedEventPlayer(listener, new ManualFrameClock(), () -> true));
    }

    private AnimatedEventPlayer player(boolean enabled) {
        return Animations.registerAll(new AnimatedEventPlayer(heard::add, clock, () -> enabled));
    }

    private static Scene scene() {
        Scene scene = new Scene(SceneTest.PLAYER);
        scene.spawn(PLAYER, SceneTest.PLAYER, A);
        scene.spawn(GOBLIN, GOBLIN_KIND, B);
        return scene;
    }

    @Test
    void play_eventsWithoutAnimation_finishAtOnce() {
        player(true).play(List.of(new ExperienceGained(A, 5)), scene(), finished::incrementAndGet);

        assertThat(finished).hasValue(1);
        assertThat(heard).hasSize(1);
        assertThat(clock.hasPendingFrame()).isFalse();
    }

    @Test
    void play_aStep_slidesTheSpriteFrameByFrame() {
        Scene scene = scene();
        player(true)
                .play(
                        List.of(new Moved(PLAYER, SceneTest.PLAYER, A, new Position(1, 2))),
                        scene,
                        finished::incrementAndGet);
        Sprite sprite = scene.sprite(PLAYER).orElseThrow();

        assertThat(scene.isAnimating()).isTrue();
        clock.frame(1000);
        clock.frame(1000 + Animations.STEP_MS / 2);
        assertThat(sprite.y()).isCloseTo(1.5f, within(0.01f));
        assertThat(finished).hasValue(0);

        clock.frame(1000 + Animations.STEP_MS);
        assertThat(sprite.y()).isEqualTo(2f);
        assertThat(finished).hasValue(1);
        assertThat(scene.isAnimating()).isFalse();
        assertThat(clock.hasPendingFrame()).isFalse();
    }

    @Test
    void play_consecutiveSteps_moveTogether() {
        Scene scene = scene();
        player(true)
                .play(
                        List.of(
                                new Moved(PLAYER, SceneTest.PLAYER, A, new Position(0, 1)),
                                new Moved(GOBLIN, GOBLIN_KIND, B, new Position(3, 1))),
                        scene,
                        finished::incrementAndGet);

        clock.frame(0);
        clock.frame(Animations.STEP_MS);

        assertThat(scene.sprite(PLAYER).orElseThrow().x()).isEqualTo(0f);
        assertThat(scene.sprite(GOBLIN).orElseThrow().x()).isEqualTo(3f);
        assertThat(finished).hasValue(1);
    }

    @Test
    void play_eventsOneAfterTheOther_andTheListenerHearsEachWhenItStarts() {
        Scene scene = scene();
        List<GameEvent> events =
                List.of(new Attacked(PLAYER, GOBLIN, A, B), new Damaged(GOBLIN, B, 5, 10));
        player(true).play(events, scene, finished::incrementAndGet);

        assertThat(heard).containsExactly(events.get(0));
        clock.frame(0);
        clock.frame(Animations.BUMP_MS);
        assertThat(heard).containsExactlyElementsOf(events);
        assertThat(finished).hasValue(0);
        clock.frame(Animations.BUMP_MS + Animations.FLASH_MS);

        assertThat(finished).hasValue(1);
        assertThat(scene.sprite(PLAYER).orElseThrow().x()).isEqualTo(1f);
        assertThat(scene.sprite(GOBLIN).orElseThrow().alpha()).isEqualTo(1f);
    }

    @Test
    void play_aDeath_fadesTheSprite() {
        Scene scene = scene();
        player(true).play(List.of(new Died(GOBLIN, GOBLIN_KIND, B)), scene, () -> {});

        clock.frame(0);
        clock.frame(Animations.FADE_MS / 2);
        assertThat(scene.sprite(GOBLIN).orElseThrow().alpha()).isCloseTo(0.5f, within(0.01f));
    }

    @Test
    void play_aShot_flies_andDisappearsAtTheEnd() {
        Scene scene = scene();
        Fired shot =
                new Fired(
                        GOBLIN,
                        GOBLIN_KIND,
                        Kind.of("arrow"),
                        B,
                        List.of(new Position(3, 1), new Position(4, 1), new Position(5, 1)));
        player(true).play(List.of(shot), scene, finished::incrementAndGet);

        assertThat(scene.effects()).singleElement().satisfies(e -> assertThat(e.x()).isEqualTo(2f));
        clock.frame(0);
        clock.frame(10_000);

        assertThat(scene.effects()).isEmpty();
        assertThat(finished).hasValue(1);
    }

    @Test
    void finish_completesEverything_once() {
        Scene scene = scene();
        AnimatedEventPlayer player = player(true);
        player.play(
                List.of(
                        new Moved(PLAYER, SceneTest.PLAYER, A, new Position(1, 2)),
                        new Died(GOBLIN, GOBLIN_KIND, B)),
                scene,
                finished::incrementAndGet);

        player.finish();
        clock.frame(5000);

        assertThat(scene.sprite(PLAYER).orElseThrow().y()).isEqualTo(2f);
        assertThat(scene.sprite(GOBLIN).orElseThrow().alpha()).isEqualTo(0f);
        assertThat(heard).hasSize(2);
        assertThat(finished).hasValue(1);
    }

    @Test
    void play_whileAnotherPlays_finishesTheFirst() {
        Scene scene = scene();
        AnimatedEventPlayer player = player(true);
        AtomicInteger first = new AtomicInteger();
        player.play(
                List.of(new Moved(PLAYER, SceneTest.PLAYER, A, new Position(1, 2))),
                scene,
                first::incrementAndGet);

        player.play(List.of(new ExperienceGained(A, 1)), scene, finished::incrementAndGet);

        assertThat(first).hasValue(1);
        assertThat(finished).hasValue(1);
    }

    @Test
    void play_withAnimationsOff_showsEverythingAtOnce() {
        Scene scene = scene();
        player(false)
                .play(
                        List.of(new Moved(PLAYER, SceneTest.PLAYER, A, new Position(1, 2))),
                        scene,
                        finished::incrementAndGet);

        assertThat(finished).hasValue(1);
        assertThat(scene.sprite(PLAYER).orElseThrow().y()).isEqualTo(2f);
        assertThat(clock.hasPendingFrame()).isFalse();
    }

    @Test
    void play_aStepOfAnActorNotInTheScene_addsItsSprite() {
        Scene scene = new Scene(SceneTest.PLAYER);
        player(true)
                .play(
                        List.of(new Moved(GOBLIN, GOBLIN_KIND, A, B)),
                        scene,
                        finished::incrementAndGet);

        assertThat(scene.sprite(GOBLIN)).isPresent();
    }
}
