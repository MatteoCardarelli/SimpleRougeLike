package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.Optional;

/**
 * Un tipo di oggetto, condiviso da tutti gli oggetti di quel tipo. Un oggetto si usa, con il suo
 * effetto, oppure si equipaggia: un oggetto da equipaggiare non ha effetto e non si consuma.
 *
 * @param consumable se l'oggetto sparisce dopo l'uso
 * @param spawnWeight quanto spesso compare sul piano rispetto agli altri tipi; 0 vuol dire mai
 * @param equipment lo spazio e il bonus, se l'oggetto si equipaggia
 */
public record ItemDefinition(
        Kind kind,
        ItemEffect effect,
        boolean consumable,
        int spawnWeight,
        Optional<Equipment> equipment) {

    public ItemDefinition {
        if (spawnWeight < 0) {
            throw new IllegalArgumentException("Peso di comparsa negativo: " + spawnWeight);
        }
        if (equipment.isPresent() && consumable) {
            throw new IllegalArgumentException(
                    "Un oggetto da equipaggiare non si consuma: " + kind.value());
        }
    }

    /** Un oggetto che si usa. */
    public ItemDefinition(Kind kind, ItemEffect effect, boolean consumable, int spawnWeight) {
        this(kind, effect, consumable, spawnWeight, Optional.empty());
    }

    /** Un oggetto da equipaggiare nello spazio {@code slot}, che aggiunge {@code bonus}. */
    public static ItemDefinition equipment(
            Kind kind, EquipmentSlot slot, Stats bonus, int spawnWeight) {
        return new ItemDefinition(
                kind, ItemEffect.NONE, false, spawnWeight, Optional.of(new Equipment(slot, bonus)));
    }
}
