package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * Un'entità viva sul piano: il player o un nemico. La posizione non è qui: la tiene {@link Floor}.
 *
 * <p>Fuori da questo package un attore si può solo leggere: salute e statistiche cambiano solo con
 * i metodi di {@link Floor}. La salute resta tra 0 e {@code stats().maxHealth()}, e un attore a 0 è
 * morto per sempre. Le statistiche sono quelle di base più gli eventuali bonus, come
 * l'equipaggiamento del player.
 */
public abstract class Actor {

    private final EntityId id;
    private Stats stats;
    private int health;

    Actor(EntityId id, Stats stats) {
        if (stats.maxHealth() <= 0) {
            throw new IllegalArgumentException("Un attore deve avere salute: " + stats);
        }
        this.id = id;
        this.stats = stats;
        this.health = stats.maxHealth();
    }

    public final EntityId id() {
        return id;
    }

    public abstract Kind kind();

    public abstract Faction faction();

    /** Le statistiche con cui l'attore combatte: quelle di base, per chi non ha bonus. */
    public Stats stats() {
        return stats;
    }

    /** Le statistiche senza bonus: quelle iniziali più la crescita per livello. */
    public final Stats baseStats() {
        return stats;
    }

    public final int health() {
        return health;
    }

    public final boolean isAlive() {
        return health > 0;
    }

    /** Toglie al massimo la salute rimasta e restituisce quanta ne ha tolta. */
    final int takeDamage(int amount) {
        int applied = Math.min(amount, health);
        health -= applied;
        return applied;
    }

    /** Restituisce la salute recuperata: 0 per un attore morto, che resta morto. */
    final int restoreHealth(int amount) {
        if (!isAlive()) {
            return 0;
        }
        int restored = Math.min(amount, stats().maxHealth() - health);
        health += restored;
        return restored;
    }

    /** Riporta la salute entro la salute massima, se un bonus tolto l'ha abbassata. */
    final void clampHealth() {
        health = Math.min(health, stats().maxHealth());
    }

    /** Aumenta le statistiche di base; la salute cresce quanto la salute massima. */
    final void raiseStats(Stats bonus) {
        stats = stats.plus(bonus);
        health += bonus.maxHealth();
    }
}
