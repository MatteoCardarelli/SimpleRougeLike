package io.github.matteocardarelli.simpleroguelike.dungeon;

import java.util.List;

/** I tipi di oggetto che esistono nel gioco. */
public interface ItemCatalog {

    /** In un ordine fisso, perché la generazione del piano sia deterministica. */
    List<ItemDefinition> all();
}
