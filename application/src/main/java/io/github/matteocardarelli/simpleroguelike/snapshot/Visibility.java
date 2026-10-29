package io.github.matteocardarelli.simpleroguelike.snapshot;

/** Cosa il player sa di una cella. */
public enum Visibility {
    /** Mai vista: l'interfaccia non mostra niente. */
    UNEXPLORED,
    /** Vista in passato: si mostra il terreno, non chi c'è adesso. */
    EXPLORED,
    /** Vista adesso. */
    VISIBLE
}
