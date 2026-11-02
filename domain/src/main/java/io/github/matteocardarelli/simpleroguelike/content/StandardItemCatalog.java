package io.github.matteocardarelli.simpleroguelike.content;

import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.effect.HealEffect;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.List;

/**
 * Gli oggetti del gioco. Un oggetto nuovo è una riga in più qui. Le pozioni sono le più comuni;
 * armi, armature e anelli sono più rari, e quelli più forti ancora di più.
 */
public final class StandardItemCatalog implements ItemCatalog {

    public static final Kind POTION = Kind.of("potion");

    /** Cura più del doppio della pozione, e compare circa una volta ogni quattro. */
    public static final Kind GREATER_POTION = Kind.of("greater_potion");

    public static final Kind SWORD = Kind.of("sword");
    public static final Kind AXE = Kind.of("axe");
    public static final Kind LEATHER_ARMOR = Kind.of("leather_armor");
    public static final Kind CHAIN_MAIL = Kind.of("chain_mail");
    public static final Kind RING_OF_ACCURACY = Kind.of("ring_of_accuracy");
    public static final Kind RING_OF_VIGOR = Kind.of("ring_of_vigor");

    private static final List<ItemDefinition> ITEMS =
            List.of(
                    new ItemDefinition(POTION, new HealEffect(25), true, 10),
                    new ItemDefinition(GREATER_POTION, new HealEffect(60), true, 3),
                    ItemDefinition.equipment(SWORD, EquipmentSlot.WEAPON, new Stats(0, 4), 2),
                    ItemDefinition.equipment(AXE, EquipmentSlot.WEAPON, new Stats(0, 7), 1),
                    ItemDefinition.equipment(
                            LEATHER_ARMOR, EquipmentSlot.ARMOR, new Stats(0, 0, 2, 0, 0), 2),
                    ItemDefinition.equipment(
                            CHAIN_MAIL, EquipmentSlot.ARMOR, new Stats(0, 0, 4, 0, 0), 1),
                    ItemDefinition.equipment(
                            RING_OF_ACCURACY, EquipmentSlot.RING, new Stats(0, 0, 0, 10, 0), 1),
                    ItemDefinition.equipment(
                            RING_OF_VIGOR, EquipmentSlot.RING, new Stats(20, 0), 1));

    @Override
    public List<ItemDefinition> all() {
        return ITEMS;
    }
}
