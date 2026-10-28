package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/** Aiuti comuni ai test delle strategie. */
final class Behaviors {

    private Behaviors() {}

    static Enemy enemy(Floor floor, int id) {
        return floor.enemy(new EntityId(id)).orElseThrow();
    }

    /**
     * Una strategia che restituisce sempre la stessa intenzione e ricorda di essere stata usata.
     */
    static final class Fixed implements EnemyBehavior {
        private final EnemyIntent intent;
        int calls;

        Fixed(EnemyIntent intent) {
            this.intent = intent;
        }

        @Override
        public EnemyIntent decide(Enemy enemy, FloorView floor, RandomSource random) {
            calls++;
            return intent;
        }
    }
}
