package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import org.junit.jupiter.api.Test;

class ActorSpritesTest {

    /** Ogni nemico del catalogo ha un'immagine per ogni direzione. */
    @Test
    void everyEnemyOfTheCatalog_hasASpriteInEveryDirection() {
        for (EnemyDefinition enemy : new StandardEnemyCatalog().all()) {
            for (Direction direction : Direction.values()) {
                assertThat(ActorSprites.sprite(enemy.kind(), direction))
                        .as(enemy.kind().value() + " " + direction)
                        .isNotZero();
            }
        }
    }

    @Test
    void sprite_choosesByDirection() {
        assertThat(ActorSprites.sprite(StandardEnemyCatalog.GOBLIN, Direction.UP))
                .isEqualTo(R.drawable.enemy_goblin_north);
        assertThat(ActorSprites.sprite(StandardEnemyCatalog.GOBLIN, Direction.DOWN))
                .isEqualTo(R.drawable.enemy_goblin_south);
        assertThat(ActorSprites.sprite(StandardEnemyCatalog.WOLF, Direction.LEFT))
                .isEqualTo(R.drawable.enemy_wolf_west);
        assertThat(ActorSprites.sprite(StandardEnemyCatalog.MERCHANT, Direction.RIGHT))
                .isEqualTo(R.drawable.enemy_merchant_east);
    }

    @Test
    void giantSlime_hasOnePoseForEveryDirection() {
        for (Direction direction : Direction.values()) {
            assertThat(ActorSprites.sprite(StandardEnemyCatalog.GIANT_SLIME, direction))
                    .isEqualTo(R.drawable.boss_giant_slime);
        }
    }

    @Test
    void front_isTheSouthImage() {
        assertThat(ActorSprites.front(StandardEnemyCatalog.SPIDER))
                .isEqualTo(R.drawable.enemy_spider_south);
    }

    @Test
    void unknownKind_hasNoSprite() {
        assertThat(ActorSprites.sprite(RangedRules.ARROW, Direction.UP)).isZero();
        assertThat(ActorSprites.sprite(Kind.of("unknown"), Direction.UP)).isZero();
    }
}
