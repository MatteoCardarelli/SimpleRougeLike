package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.Nullable;

/**
 * Riproduce gli eventi con le animazioni, uno dopo l'altro, a ogni fotogramma di un {@link
 * FrameClock}. I passi consecutivi si animano insieme, così i nemici si muovono tutti nello stesso
 * momento; un evento senza animazione si mostra subito. L'ascoltatore sente ogni evento quando la
 * sua animazione comincia.
 *
 * <p>Se le animazioni del sistema sono spente, per accessibilità o nei test, riproduce tutto
 * subito, come {@link InstantEventPlayer}. È un registro per classe di evento, come il formattatore
 * dei messaggi: un evento nuovo si anima registrando il suo {@link EventAnimator}.
 */
public final class AnimatedEventPlayer implements EventPlayer {

    private final EventListener listener;
    private final FrameClock clock;
    private final BooleanSupplier enabled;
    private final Map<Class<? extends GameEvent>, EventAnimator<GameEvent>> animators =
            new LinkedHashMap<>();
    private @Nullable Playback current;

    /**
     * @param enabled vero se le animazioni sono accese, chiesto a ogni riproduzione
     */
    public AnimatedEventPlayer(EventListener listener, FrameClock clock, BooleanSupplier enabled) {
        this.listener = listener;
        this.clock = clock;
        this.enabled = enabled;
    }

    /** Registra l'animazione di un tipo di evento. */
    public <E extends GameEvent> void register(Class<E> type, EventAnimator<E> animator) {
        animators.put(type, (event, scene) -> animator.animate(type.cast(event), scene));
    }

    @Override
    public void play(List<GameEvent> events, Scene scene, Runnable onFinished) {
        finish();
        Playback playback = new Playback(group(events), scene, onFinished);
        current = playback;
        if (!enabled.getAsBoolean()) {
            completeAll(playback);
            return;
        }
        scene.setAnimating(true);
        startNextAnimatedStep(playback);
        if (playback.running.isEmpty()) {
            end(playback);
        } else {
            clock.requestFrame(now -> onFrame(playback, now));
        }
    }

    @Override
    public void finish() {
        Playback playback = current;
        if (playback != null) {
            completeAll(playback);
        }
    }

    private void onFrame(Playback playback, long nowMs) {
        if (playback != current) {
            return;
        }
        if (playback.startMs < 0) {
            playback.startMs = nowMs;
        }
        long elapsed = nowMs - playback.startMs;
        float fraction =
                playback.durationMs == 0 ? 1f : Math.min(1f, (float) elapsed / playback.durationMs);
        for (Animation animation : playback.running) {
            animation.apply(fraction);
        }
        if (fraction >= 1f) {
            playback.running = List.of();
            startNextAnimatedStep(playback);
            if (playback.running.isEmpty()) {
                end(playback);
                return;
            }
            playback.startMs = nowMs;
        }
        clock.requestFrame(now -> onFrame(playback, now));
    }

    /** Mostra subito i passi senza animazione e comincia il primo che ne ha. */
    private void startNextAnimatedStep(Playback playback) {
        while (playback.running.isEmpty() && !playback.steps.isEmpty()) {
            List<GameEvent> step = playback.steps.poll();
            List<Animation> animations = new ArrayList<>();
            long duration = 0;
            for (GameEvent event : step) {
                listener.onEventPlayed(event);
                EventAnimator<GameEvent> animator = animators.get(event.getClass());
                Optional<Animation> animation =
                        animator == null
                                ? Optional.empty()
                                : animator.animate(event, playback.scene);
                if (animation.isPresent()) {
                    animations.add(animation.get());
                    duration = Math.max(duration, animation.get().durationMs());
                }
            }
            for (Animation animation : animations) {
                animation.apply(0f);
            }
            playback.running = animations;
            playback.durationMs = duration;
            playback.startMs = -1;
        }
    }

    private void completeAll(Playback playback) {
        do {
            for (Animation animation : playback.running) {
                animation.apply(1f);
            }
            playback.running = List.of();
            startNextAnimatedStep(playback);
        } while (!playback.running.isEmpty());
        end(playback);
    }

    private void end(Playback playback) {
        if (current == playback) {
            current = null;
        }
        playback.scene.setAnimating(false);
        playback.onFinished.run();
    }

    /** I passi consecutivi diventano un gruppo solo, gli altri eventi uno per gruppo. */
    private static Deque<List<GameEvent>> group(List<GameEvent> events) {
        Deque<List<GameEvent>> steps = new ArrayDeque<>();
        List<GameEvent> moves = new ArrayList<>();
        for (GameEvent event : events) {
            if (event.getClass() == Moved.class) {
                moves.add(event);
                continue;
            }
            if (!moves.isEmpty()) {
                steps.add(moves);
                moves = new ArrayList<>();
            }
            steps.add(List.of(event));
        }
        if (!moves.isEmpty()) {
            steps.add(moves);
        }
        return steps;
    }

    private static final class Playback {
        final Deque<List<GameEvent>> steps;
        final Scene scene;
        final Runnable onFinished;
        List<Animation> running = List.of();
        long startMs = -1;
        long durationMs;

        Playback(Deque<List<GameEvent>> steps, Scene scene, Runnable onFinished) {
            this.steps = steps;
            this.scene = scene;
            this.onFinished = onFinished;
        }
    }
}
