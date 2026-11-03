package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Assegna gli id di attori e oggetti, in ordine, a partire da 1. Una per partita. */
public final class EntityIdSequence {

    private int next;

    public EntityIdSequence() {
        this(1);
    }

    /** Riprende la sequenza da {@code next}, per una partita salvata. */
    public EntityIdSequence(int next) {
        if (next < 1) {
            throw new IllegalArgumentException("Id non valido: " + next);
        }
        this.next = next;
    }

    /** Il prossimo id che verrà dato, senza darlo. */
    public int peek() {
        return next;
    }

    public EntityId next() {
        return new EntityId(next++);
    }
}
