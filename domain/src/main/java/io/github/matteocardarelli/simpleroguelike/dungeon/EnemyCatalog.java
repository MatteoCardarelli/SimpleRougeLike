package io.github.matteocardarelli.simpleroguelike.dungeon;

import java.util.List;

/** I tipi di nemico che esistono nel gioco. */
public interface EnemyCatalog {

    /** In un ordine fisso, perché la generazione del piano sia deterministica. */
    List<EnemyDefinition> all();
}
