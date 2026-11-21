package io.github.matteocardarelli.simpleroguelike.ui.input;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Camminare tenendo il dito premuto: un joystick invisibile il cui centro è dove il dito ha
 * toccato. Spostando il dito oltre la distanza minima su un asse si sceglie la direzione; il primo
 * passo è subito, poi si ripete a ogni fine di riproduzione finché il dito resta lì.
 *
 * <p>La classe non conosce Android: i tempi arrivano come parametri, in millisecondi, e chi la usa
 * traduce i tocchi e gestisce i timer. Si ferma da sola quando il passo è bloccato (riprende solo
 * con un'altra direzione) o quando c'è pericolo, cioè un nemico nuovo in vista o danno al player
 * (riprende solo con un nuovo tocco). Se si sta mirando, il primo gesto lancia e nient'altro.
 */
public final class HoldToWalk {

    /** Da quanto tempo la direzione deve essere tenuta perché il passo si ripeta. */
    public static final long HOLD_DELAY_MS = 200;

    private final float minDistancePx;

    private boolean down;
    private float centerX;
    private float centerY;
    private @Nullable Direction held;
    private long heldSince;
    private boolean stepPending;
    private @Nullable Direction blocked;
    private boolean locked;
    private @Nullable Set<EntityId> knownEnemies;
    private long lastRevision = -1;

    public HoldToWalk(float minDistancePx) {
        this.minDistancePx = minDistancePx;
    }

    /** Il dito tocca lo schermo: qui c'è il centro. Un nuovo tocco toglie ogni blocco. */
    public void press(float x, float y) {
        down = true;
        centerX = x;
        centerY = y;
        held = null;
        blocked = null;
        locked = false;
    }

    /**
     * Il dito si sposta.
     *
     * @param now il tempo in millisecondi
     * @param aiming vero se il player sta scegliendo la direzione di un lancio
     * @return la direzione del passo (o del lancio) da fare subito, se ce n'è una
     */
    public Optional<Direction> drag(float x, float y, long now, boolean aiming) {
        if (!down) {
            return Optional.empty();
        }
        Direction direction = directionOf(x - centerX, y - centerY);
        if (direction == held) {
            return Optional.empty();
        }
        held = direction;
        heldSince = now;
        if (direction == null || locked || direction == blocked) {
            return Optional.empty();
        }
        blocked = null;
        stepPending = true;
        if (aiming) {
            locked = true;
        }
        return Optional.of(direction);
    }

    /** Il dito si alza, o il gesto è annullato: si ferma tutto. */
    public void release() {
        down = false;
        held = null;
        blocked = null;
        locked = false;
        stepPending = false;
    }

    /** Un turno è stato mostrato: si valutano il passo fatto e il pericolo, una volta per turno. */
    public void onTurnShown(TurnSummary turn) {
        if (turn.revision() == lastRevision) {
            return;
        }
        lastRevision = turn.revision();
        Set<EntityId> before = knownEnemies;
        knownEnemies = turn.visibleEnemies();
        boolean step = stepPending;
        stepPending = false;
        if (!down) {
            return;
        }
        boolean newEnemy = false;
        if (before != null) {
            for (EntityId enemy : turn.visibleEnemies()) {
                if (!before.contains(enemy)) {
                    newEnemy = true;
                }
            }
        }
        if (newEnemy || turn.playerDamaged()) {
            locked = true;
        } else if (step && !turn.playerProgressed()) {
            blocked = held;
        }
    }

    /**
     * Quanto manca al prossimo passo ripetuto: -1 se non ce n'è, 0 se si può fare adesso,
     * altrimenti i millisecondi da aspettare.
     */
    public long repeatWait(long now) {
        if (!down || held == null || locked || held == blocked || stepPending) {
            return -1;
        }
        return Math.max(0, heldSince + HOLD_DELAY_MS - now);
    }

    /** Il passo da ripetere adesso, se {@link #repeatWait} dice che è il momento. */
    public Optional<Direction> repeat(long now) {
        if (repeatWait(now) != 0) {
            return Optional.empty();
        }
        stepPending = true;
        return Optional.ofNullable(held);
    }

    private @Nullable Direction directionOf(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            if (Math.abs(dx) > minDistancePx) {
                return dx > 0 ? Direction.RIGHT : Direction.LEFT;
            }
        } else if (Math.abs(dy) > minDistancePx) {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
        return null;
    }
}
