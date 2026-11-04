package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class EnemyLevelTest {

    @Test
    void level1_hasTheValuesOfTheDefinition() {
        Enemy goblin = new Enemy(new EntityId(1), FloorFixtures.goblin());

        assertThat(goblin.level()).isEqualTo(1);
        assertThat(goblin.stats()).isEqualTo(FloorFixtures.goblin().stats());
        assertThat(goblin.experienceReward()).isEqualTo(FloorFixtures.goblin().experienceReward());
    }

    /** Sul quinto piano un goblin ha il doppio della salute e dell'esperienza. */
    @Test
    void level5_goblin_isAboutTwiceAsStrong() {
        Enemy goblin = new Enemy(new EntityId(1), FloorFixtures.goblin(), 5);

        assertThat(goblin.stats()).isEqualTo(new Stats(60, 15, 3, 12, 9));
        assertThat(goblin.health()).isEqualTo(60);
        assertThat(goblin.experienceReward()).isEqualTo(50);
    }

    @Test
    void bossAndMerchant_doNotGrow() {
        Enemy slime = new Enemy(new EntityId(1), FloorFixtures.giantSlime(), 5);
        Enemy merchant = new Enemy(new EntityId(2), FloorFixtures.merchant(), 3);

        assertThat(slime.stats()).isEqualTo(FloorFixtures.giantSlime().stats());
        assertThat(slime.level()).isEqualTo(1);
        assertThat(merchant.stats()).isEqualTo(FloorFixtures.merchant().stats());
    }

    @Test
    void levelBelowOne_throws() {
        assertThatThrownBy(() -> new Enemy(new EntityId(1), FloorFixtures.goblin(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
