package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.effect.HealEffect;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.List;

/** Gli oggetti del gioco. Un oggetto nuovo è una riga in più qui. */
public final class StandardItemCatalog implements ItemCatalog {

    public static final Kind POTION = Kind.of("potion");

    /** Cura più del doppio della pozione, e compare circa una volta ogni quattro. */
    public static final Kind GREATER_POTION = Kind.of("greater_potion");

    private static final List<ItemDefinition> ITEMS =
            List.of(
                    new ItemDefinition(POTION, new HealEffect(25), true, 10),
                    new ItemDefinition(GREATER_POTION, new HealEffect(60), true, 3));

    @Override
    public List<ItemDefinition> all() {
        return ITEMS;
    }
}
