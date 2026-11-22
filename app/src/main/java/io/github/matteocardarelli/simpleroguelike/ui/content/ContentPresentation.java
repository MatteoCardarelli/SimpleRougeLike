package io.github.matteocardarelli.simpleroguelike.ui.content;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Come l'interfaccia mostra ogni {@link Kind}: nome, descrizione, colore e illustrazione. Il
 * dominio dà solo la chiave; i testi stanno nelle risorse, nella lingua del telefono. I valori li
 * sceglie chi crea l'oggetto, insieme ai cataloghi.
 */
public final class ContentPresentation {

    /**
     * L'aspetto di un {@code Kind}: il colore resta per i dettagli, come una porta disegnata a
     * forme, l'illustrazione è quella di inventario e mappa. Ci vuole almeno l'illustrazione
     * vettoriale o la pixel art.
     */
    public record Style(
            @StringRes int name,
            @StringRes int description,
            @ColorRes int color,
            @DrawableRes int art,
            @DrawableRes int sprite) {

        /** Un aspetto senza pixel art, solo con l'illustrazione vettoriale. */
        public Style(
                @StringRes int name,
                @StringRes int description,
                @ColorRes int color,
                @DrawableRes int art) {
            this(name, description, color, art, 0);
        }

        /** Un aspetto con la sola pixel art, senza illustrazione vettoriale. */
        public static Style ofSprite(
                @StringRes int name,
                @StringRes int description,
                @ColorRes int color,
                @DrawableRes int sprite) {
            return new Style(name, description, color, 0, sprite);
        }

        /** Lo stesso aspetto con la sua immagine pixel art. */
        public Style withSprite(@DrawableRes int sprite) {
            return new Style(name, description, color, art, sprite);
        }

        public Style {
            if (art == 0 && sprite == 0) {
                throw new IllegalArgumentException(
                        "Un aspetto ha bisogno della sua illustrazione o della sua pixel art");
            }
        }
    }

    private final Map<Kind, Style> styles;

    public ContentPresentation(Map<Kind, Style> styles) {
        this.styles = Collections.unmodifiableMap(new LinkedHashMap<>(styles));
    }

    @StringRes
    public int nameRes(Kind kind) {
        return style(kind).name();
    }

    @StringRes
    public int descriptionRes(Kind kind) {
        return style(kind).description();
    }

    @ColorRes
    public int colorRes(Kind kind) {
        return style(kind).color();
    }

    /** L'illustrazione del {@code Kind}, vettoriale; 0 se ha solo la pixel art. */
    @DrawableRes
    public int artRes(Kind kind) {
        return style(kind).art();
    }

    /** L'immagine pixel art del {@code Kind}, o 0 se ha solo l'illustrazione vettoriale. */
    @DrawableRes
    public int spriteRes(Kind kind) {
        return style(kind).sprite();
    }

    /** I {@code Kind} che hanno un aspetto. */
    public Set<Kind> kinds() {
        return styles.keySet();
    }

    /**
     * @throws IllegalArgumentException se il {@code Kind} non ha un aspetto: un contenuto nuovo
     *     senza la sua riga
     */
    private Style style(Kind kind) {
        Style style = styles.get(kind);
        if (style == null) {
            throw new IllegalArgumentException("Nessun aspetto per " + kind.value());
        }
        return style;
    }
}
