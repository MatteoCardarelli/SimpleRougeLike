package io.github.matteocardarelli.simpleroguelike.ui.content;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

/**
 * Mostra l'immagine di un {@code Kind} in una vista, per inventario e schede: sta accanto a {@link
 * ContentPresentation}, che chi disegna le liste conosce già.
 */
public final class ArtViews {

    private ArtViews() {}

    /**
     * Mostra il {@code Kind} in una vista: la pixel art, ingrandita senza sfumare, se ce l'ha,
     * altrimenti l'illustrazione vettoriale.
     */
    public static void show(ImageView view, ContentPresentation content, Kind kind) {
        int sprite = content.spriteRes(kind);
        if (sprite == 0) {
            view.setImageResource(content.artRes(kind));
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        BitmapDrawable drawable =
                new BitmapDrawable(
                        view.getResources(),
                        BitmapFactory.decodeResource(view.getResources(), sprite, options));
        drawable.setFilterBitmap(false);
        drawable.setAntiAlias(false);
        drawable.setDither(false);
        view.setImageDrawable(drawable);
    }
}
