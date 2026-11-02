package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * I valori con cui parte il player: 100 di salute e 10 di attacco come nel gioco di prima, più un
 * po' di difesa, precisione e schivata.
 */
public final class StandardPlayer {

    /** L'inventario prima era illimitato: 10 oggetti bastano per un piano. */
    public static final PlayerDefinition DEFINITION =
            new PlayerDefinition(new Stats(100, 10, 2, 10, 5), 100, 10);

    private StandardPlayer() {}
}
