package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Il tipo di un oggetto o di un nemico, per esempio {@code potion} o {@code goblin}. L'interfaccia
 * lo usa per scegliere nome e aspetto.
 */
public record Kind(String value) {

    public Kind {
        Keys.requireSnakeCase(value, "Un Kind");
    }

    public static Kind of(String value) {
        return new Kind(value);
    }
}
