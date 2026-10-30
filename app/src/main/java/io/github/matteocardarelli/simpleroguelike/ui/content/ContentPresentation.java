package io.github.matteocardarelli.simpleroguelike.ui.content;

import androidx.annotation.StringRes;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Come l'interfaccia mostra ogni {@link Kind}: il dominio dà solo la chiave, il nome sta nelle
 * risorse, nella lingua del telefono. I valori li sceglie chi crea l'oggetto, insieme ai cataloghi.
 */
public final class ContentPresentation {

    private final Map<Kind, Integer> names;

    public ContentPresentation(Map<Kind, Integer> names) {
        this.names = Collections.unmodifiableMap(new LinkedHashMap<>(names));
    }

    /**
     * La stringa con il nome del {@code Kind}.
     *
     * @throws IllegalArgumentException se il {@code Kind} non ha un nome: un contenuto nuovo senza
     *     la sua riga qui
     */
    @StringRes
    public int nameRes(Kind kind) {
        Integer name = names.get(kind);
        if (name == null) {
            throw new IllegalArgumentException("Nessun nome per " + kind.value());
        }
        return name;
    }

    /** I {@code Kind} che hanno un nome. */
    public Set<Kind> kinds() {
        return names.keySet();
    }
}
