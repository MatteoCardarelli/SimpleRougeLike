package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/** I valori con cui parte il player, gli stessi del gioco di prima del refactoring. */
public final class StandardPlayer {

    /** L'inventario prima era illimitato: 10 oggetti bastano per un piano. */
    public static final PlayerDefinition DEFINITION =
            new PlayerDefinition(new Stats(100, 10), 100, 10);

    private StandardPlayer() {}
}
