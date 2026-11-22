package io.github.matteocardarelli.simpleroguelike.ui.render;

import androidx.annotation.DrawableRes;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.ui.playback.Animations;

/** Le immagini degli effetti di UI, il colpo e la nuvola: quale fotogramma mostra ogni effetto. */
final class EffectSprites {

    private static final int[] SLASH = {
        R.drawable.fx_slash_0, R.drawable.fx_slash_1, R.drawable.fx_slash_2
    };

    private static final int[] PUFF = {
        R.drawable.fx_puff_0, R.drawable.fx_puff_1, R.drawable.fx_puff_2, R.drawable.fx_puff_3
    };

    private EffectSprites() {}

    /**
     * L'immagine del fotogramma {@code frame} dell'effetto {@code kind}, o 0 se non è un effetto a
     * fotogrammi (un proiettile). Un fotogramma fuori misura resta sul primo o sull'ultimo.
     */
    @DrawableRes
    static int drawable(Kind kind, int frame) {
        if (kind.equals(Animations.FX_SLASH)) {
            return SLASH[Math.max(0, Math.min(SLASH.length - 1, frame))];
        }
        if (kind.equals(Animations.FX_PUFF)) {
            return PUFF[Math.max(0, Math.min(PUFF.length - 1, frame))];
        }
        return 0;
    }
}
