package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Assegna gli id di attori e oggetti, in ordine, a partire da 1. Una per partita. */
public final class EntityIdSequence {

    private int next = 1;

    public EntityId next() {
        return new EntityId(next++);
    }
}
