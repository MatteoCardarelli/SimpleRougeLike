package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Una strategia con cui un nemico decide cosa fare. Riceve il piano in sola lettura e il flusso
 * casuale di gioco come argomenti: non modifica niente e non tiene stato tra una partita e l'altra.
 */
public interface EnemyBehavior {

    /** Decide l'intenzione di un nemico vivo. */
    EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random);
}
