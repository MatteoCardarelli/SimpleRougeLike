package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class EnemyTest {

    @Test
    void enemy_takesValuesFromItsDefinition() {
        Enemy goblin = new Enemy(new EntityId(2), FloorFixtures.goblin());

        assertThat(goblin.kind()).isEqualTo(Kind.of("goblin"));
        assertThat(goblin.faction()).isEqualTo(Faction.MONSTERS);
        assertThat(goblin.stats()).isEqualTo(new Stats(30, 7, 1, 0, 5));
        assertThat(goblin.health()).isEqualTo(30);
        assertThat(goblin.experienceReward()).isEqualTo(25);
        assertThat(goblin.aggroRange()).isEqualTo(8);
        assertThat(goblin.behavior()).isEqualTo(BehaviorKey.of("chase_or_wander"));
        assertThat(goblin.definition()).isEqualTo(FloorFixtures.goblin());
    }
}
