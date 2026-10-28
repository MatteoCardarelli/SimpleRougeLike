package io.github.matteocardarelli.simpleroguelike.dungeon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void player_startsAtLevelOneWithFullHealth() {
        Player player = new Player(new EntityId(1), new Stats(100, 10), 100, 5);

        assertThat(player.level()).isEqualTo(1);
        assertThat(player.experience()).isZero();
        assertThat(player.experienceToNextLevel()).isEqualTo(100);
        assertThat(player.health()).isEqualTo(100);
        assertThat(player.isAlive()).isTrue();
        assertThat(player.kind()).isEqualTo(Player.KIND);
        assertThat(player.faction()).isEqualTo(Faction.PLAYER);
        assertThat(player.inventory().capacity()).isEqualTo(5);
    }

    @Test
    void player_invalidValues_areRejected() {
        assertThatThrownBy(() -> new Player(new EntityId(1), new Stats(0, 10), 100, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Player(new EntityId(1), new Stats(10, 10), 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Player(new EntityId(1), new Stats(10, 10), 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void player_negativeExperience_isRejected() {
        Player player = new Player(new EntityId(1), new Stats(10, 1), 10, 1);

        LevelingPolicy policy =
                new LevelingPolicy() {
                    @Override
                    public Stats bonusPerLevel() {
                        return new Stats(0, 0);
                    }

                    @Override
                    public int nextThreshold(int current) {
                        return current + 1;
                    }
                };

        assertThatThrownBy(() -> player.gainExperience(-1, policy))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
