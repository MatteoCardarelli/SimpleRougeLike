package io.github.matteocardarelli.simpleroguelike.ui.content;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Come l'interfaccia mostra ogni {@link Kind}: nome, descrizione e colore. Il dominio dà solo la
 * chiave; i testi stanno nelle risorse, nella lingua del telefono. I valori li sceglie chi crea
 * l'oggetto, insieme ai cataloghi.
 */
public final class ContentPresentation {

    /** L'aspetto di un {@code Kind}; il colore sostituisce l'illustrazione finché non arriva. */
    public record Style(@StringRes int name, @StringRes int description, @ColorRes int color) {}

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
