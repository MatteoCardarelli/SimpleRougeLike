package io.github.matteocardarelli.simpleroguelike.ui.render;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import androidx.annotation.DrawableRes;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Le immagini pixel art di terreno, oggetti e oggetti fissi: ognuna si carica una volta, la prima
 * che serve, senza ridimensionarla. La scala la sceglie chi disegna, con {@link #newPaint()}.
 */
public final class PixelSprites {

    private final Resources resources;
    private final Map<Integer, Bitmap> cache = new HashMap<>();

    PixelSprites(Context context) {
        this.resources = context.getResources();
    }

    /** Per la pixel art niente filtri né sfumature: i pixel restano nitidi. */
    static Paint newPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        return paint;
    }

    /** L'immagine della risorsa, caricata una sola volta. */
    Bitmap bitmap(@DrawableRes int id) {
        @Nullable Bitmap bitmap = cache.get(id);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(resources, id, noScaling());
            cache.put(id, bitmap);
        }
        return bitmap;
    }

    private static BitmapFactory.Options noScaling() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return options;
    }
}
