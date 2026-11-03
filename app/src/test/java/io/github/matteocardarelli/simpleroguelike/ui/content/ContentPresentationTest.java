package io.github.matteocardarelli.simpleroguelike.ui.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.di.TestContent;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.FixtureType;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentPresentationTest {

    private final ContentPresentation content = TestContent.presentation();

    /** Ogni cosa che il gioco può mostrare ha nome, descrizione e illustrazione. */
    @Test
    void everyKindOfTheGame_hasItsArt() {
        List<Kind> kinds = new ArrayList<>();
        kinds.add(Player.KIND);
        for (ItemDefinition item : new StandardItemCatalog().all()) {
            kinds.add(item.kind());
        }
        for (EnemyDefinition enemy : new StandardEnemyCatalog().all()) {
            kinds.add(enemy.kind());
        }
        for (FixtureType fixture : FixtureType.values()) {
            kinds.add(fixture.kind());
        }

        assertThat(content.kinds()).containsAll(kinds);
        for (Kind kind : kinds) {
            assertThat(content.artRes(kind)).as(kind.value()).isNotZero();
        }
    }

    @Test
    void style_withoutArt_isRejected() {
        assertThatThrownBy(() -> new ContentPresentation.Style(1, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
