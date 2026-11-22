package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.dungeon.FixtureType;
import org.junit.jupiter.api.Test;

class FixtureSpritesTest {

    /** Ogni tipo di oggetto fisso ha la sua immagine, aperto e chiuso. */
    @Test
    void everyFixtureType_hasASpriteInBothStates() {
        for (FixtureType type : FixtureType.values()) {
            assertThat(FixtureSprites.sprite(type.kind().value(), false))
                    .as(type.kind().value())
                    .isNotZero();
            assertThat(FixtureSprites.sprite(type.kind().value(), true))
                    .as(type.kind().value() + " open")
                    .isNotZero();
        }
    }

    @Test
    void sprite_choosesByKindAndState() {
        assertThat(FixtureSprites.sprite("door", false)).isEqualTo(R.drawable.fixture_door);
        assertThat(FixtureSprites.sprite("door", true)).isEqualTo(R.drawable.fixture_door_open);
        assertThat(FixtureSprites.sprite("lever", true)).isEqualTo(R.drawable.fixture_lever_open);
        assertThat(FixtureSprites.sprite("chest", false)).isEqualTo(R.drawable.fixture_chest);
    }

    @Test
    void stairs_haveOneImageForBothStates() {
        assertThat(FixtureSprites.sprite("stairs", true))
                .isEqualTo(FixtureSprites.sprite("stairs", false))
                .isEqualTo(R.drawable.fixture_stairs);
    }

    @Test
    void traps_haveOneImageEach_whetherSpentOrNot() {
        assertThat(FixtureSprites.sprite("trap_spikes", false))
                .isEqualTo(FixtureSprites.sprite("trap_spikes", true))
                .isEqualTo(R.drawable.trap_spikes);
        assertThat(FixtureSprites.sprite("trap_dart", false))
                .isEqualTo(FixtureSprites.sprite("trap_dart", true))
                .isEqualTo(R.drawable.trap_dart);
    }

    @Test
    void unknownKind_hasNoSprite() {
        assertThat(FixtureSprites.sprite("unknown", false)).isZero();
    }
}
