package io.github.matteocardarelli.simpleroguelike.ui.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Animations;
import org.junit.jupiter.api.Test;

class EffectSpritesTest {

    @Test
    void drawable_slashFollowsTheFrames() {
        assertThat(EffectSprites.drawable(Animations.FX_SLASH, 0)).isEqualTo(R.drawable.fx_slash_0);
        assertThat(EffectSprites.drawable(Animations.FX_SLASH, 2)).isEqualTo(R.drawable.fx_slash_2);
    }

    @Test
    void drawable_puffFollowsTheFrames() {
        assertThat(EffectSprites.drawable(Animations.FX_PUFF, 0)).isEqualTo(R.drawable.fx_puff_0);
        assertThat(EffectSprites.drawable(Animations.FX_PUFF, 3)).isEqualTo(R.drawable.fx_puff_3);
    }

    @Test
    void drawable_outOfRangeFrame_isClamped() {
        assertThat(EffectSprites.drawable(Animations.FX_SLASH, -1))
                .isEqualTo(R.drawable.fx_slash_0);
        assertThat(EffectSprites.drawable(Animations.FX_PUFF, 9)).isEqualTo(R.drawable.fx_puff_3);
    }

    @Test
    void drawable_projectile_isNotAnEffectImage() {
        assertThat(EffectSprites.drawable(Kind.of("arrow"), 0)).isZero();
    }
}
