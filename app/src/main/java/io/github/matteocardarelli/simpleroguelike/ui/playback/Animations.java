package io.github.matteocardarelli.simpleroguelike.ui.playback;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Optional;

/**
 * Le animazioni degli eventi del gioco, brevi perché il gioco resti svelto: un passo scivola da una
 * cella all'altra, un attacco è uno scatto verso il bersaglio, chi è colpito lampeggia, chi muore
 * svanisce, un proiettile attraversa le celle del suo percorso. Un attore che la scena non ha non
 * si anima.
 */
public final class Animations {

    static final long STEP_MS = 110;
    static final long BUMP_MS = 120;
    static final long FLASH_MS = 140;
    static final long FADE_MS = 180;
    static final long LANDING_MS = 260;
    static final long PROJECTILE_MS_PER_CELL = 35;
    static final long PROJECTILE_MIN_MS = 90;

    /** Quanto si sposta verso il bersaglio chi attacca, in celle. */
    static final float BUMP_DISTANCE = 0.3f;

    private Animations() {}

    /** Registra tutte le animazioni sul riproduttore. */
    public static AnimatedEventPlayer registerAll(AnimatedEventPlayer player) {
        player.register(Moved.class, Animations::step);
        player.register(Attacked.class, Animations::bump);
        player.register(Damaged.class, Animations::flash);
        player.register(Died.class, Animations::fade);
        player.register(Fired.class, Animations::projectile);
        player.register(JumpAnnounced.class, Animations::liftOff);
        player.register(Landed.class, Animations::landing);
        return player;
    }

    static Optional<Animation> step(Moved event, Scene scene) {
        Sprite sprite =
                scene.sprite(event.actor())
                        .orElseGet(() -> scene.spawn(event.actor(), event.kind(), event.from()));
        Position from = event.from();
        Position to = event.to();
        return Optional.of(
                animation(
                        STEP_MS,
                        f -> sprite.moveTo(lerp(from.x(), to.x(), f), lerp(from.y(), to.y(), f))));
    }

    /**
     * Il salto: lo sprite si alza e vola sopra la sua ombra, più lento di un passo. La cella di
     * riferimento dell'ombra è l'angolo in alto a sinistra delle sue celle.
     */
    static Optional<Animation> liftOff(JumpAnnounced event, Scene scene) {
        if (event.cells().isEmpty()) {
            return Optional.empty();
        }
        int toX = Integer.MAX_VALUE;
        int toY = Integer.MAX_VALUE;
        for (Position cell : event.cells()) {
            toX = Math.min(toX, cell.x());
            toY = Math.min(toY, cell.y());
        }
        int targetX = toX;
        int targetY = toY;
        return scene.sprite(event.jumper())
                .map(
                        sprite -> {
                            float fromX = sprite.x();
                            float fromY = sprite.y();
                            return animation(
                                    LANDING_MS,
                                    f ->
                                            sprite.moveTo(
                                                    fromX + (targetX - fromX) * f,
                                                    fromY + (targetY - fromY) * f));
                        });
    }

    /** L'atterraggio di un salto: lo sprite vola fino alla cella nuova, più lento di un passo. */
    static Optional<Animation> landing(Landed event, Scene scene) {
        return scene.sprite(event.jumper())
                .map(
                        sprite -> {
                            Position from = event.from();
                            Position to = event.to();
                            return animation(
                                    LANDING_MS,
                                    f ->
                                            sprite.moveTo(
                                                    lerp(from.x(), to.x(), f),
                                                    lerp(from.y(), to.y(), f)));
                        });
    }

    static Optional<Animation> bump(Attacked event, Scene scene) {
        return scene.sprite(event.attacker())
                .map(
                        sprite -> {
                            Position from = event.attackerAt();
                            Position to = event.targetAt();
                            return animation(
                                    BUMP_MS,
                                    f -> {
                                        float push = BUMP_DISTANCE * (float) Math.sin(Math.PI * f);
                                        sprite.moveTo(
                                                from.x() + (to.x() - from.x()) * push,
                                                from.y() + (to.y() - from.y()) * push);
                                    });
                        });
    }

    static Optional<Animation> flash(Damaged event, Scene scene) {
        return scene.sprite(event.target())
                .map(
                        sprite ->
                                animation(
                                        FLASH_MS,
                                        f ->
                                                sprite.setAlpha(
                                                        1f
                                                                - 0.7f
                                                                        * (float)
                                                                                Math.sin(
                                                                                        Math.PI
                                                                                                * f))));
    }

    static Optional<Animation> fade(Died event, Scene scene) {
        return scene.sprite(event.actor())
                .map(sprite -> animation(FADE_MS, f -> sprite.setAlpha(1f - f)));
    }

    static Optional<Animation> projectile(Fired event, Scene scene) {
        if (event.path().isEmpty()) {
            return Optional.empty();
        }
        Position from = event.from();
        Position to = event.path().get(event.path().size() - 1);
        Sprite projectile = new Sprite(event.shooter(), event.projectile(), from.x(), from.y());
        scene.addEffect(projectile);
        long duration = Math.max(PROJECTILE_MIN_MS, PROJECTILE_MS_PER_CELL * event.path().size());
        return Optional.of(
                animation(
                        duration,
                        f -> {
                            projectile.moveTo(lerp(from.x(), to.x(), f), lerp(from.y(), to.y(), f));
                            if (f >= 1f) {
                                scene.removeEffect(projectile);
                            }
                        }));
    }

    private static float lerp(int from, int to, float fraction) {
        return from + (to - from) * fraction;
    }

    private interface Frame {
        void apply(float fraction);
    }

    private static Animation animation(long durationMs, Frame frame) {
        return new Animation() {
            @Override
            public long durationMs() {
                return durationMs;
            }

            @Override
            public void apply(float fraction) {
                frame.apply(fraction);
            }
        };
    }
}
