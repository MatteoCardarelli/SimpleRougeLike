package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.Optional;

/**
 * Un tipo di oggetto, condiviso da tutti gli oggetti di quel tipo. Un oggetto si usa, con il suo
 * effetto, si equipaggia oppure si lancia. Un oggetto da equipaggiare o da lanciare non ha effetto
 * e non si usa; gli oggetti da lanciare si impilano nell'inventario.
 *
 * @param consumable se l'oggetto sparisce dopo l'uso
 * @param spawnWeight quanto spesso compare sul piano rispetto agli altri tipi; 0 vuol dire mai
 * @param equipment lo spazio e il bonus, se l'oggetto si equipaggia
 * @param throwing danno e portata, se l'oggetto si lancia
 */
public record ItemDefinition(
        Kind kind,
        ItemEffect effect,
        boolean consumable,
        int spawnWeight,
        Optional<Equipment> equipment,
        Optional<Throwing> throwing) {

    public ItemDefinition {
        if (spawnWeight < 0) {
            throw new IllegalArgumentException("Peso di comparsa negativo: " + spawnWeight);
        }
        if (equipment.isPresent() && consumable) {
            throw new IllegalArgumentException(
                    "Un oggetto da equipaggiare non si consuma: " + kind.value());
        }
        if (equipment.isPresent() && throwing.isPresent()) {
            throw new IllegalArgumentException(
                    "Un oggetto non si equipaggia e si lancia insieme: " + kind.value());
        }
    }

    /** Un oggetto che si usa. */
    public ItemDefinition(Kind kind, ItemEffect effect, boolean consumable, int spawnWeight) {
        this(kind, effect, consumable, spawnWeight, Optional.empty(), Optional.empty());
    }

    /** Un oggetto che si usa o si equipaggia, ma non si lancia. */
    public ItemDefinition(
            Kind kind,
            ItemEffect effect,
            boolean consumable,
            int spawnWeight,
            Optional<Equipment> equipment) {
        this(kind, effect, consumable, spawnWeight, equipment, Optional.empty());
    }

    /** Un oggetto da equipaggiare nello spazio {@code slot}, che aggiunge {@code bonus}. */
    public static ItemDefinition equipment(
            Kind kind, EquipmentSlot slot, Stats bonus, int spawnWeight) {
        return new ItemDefinition(
                kind, ItemEffect.NONE, false, spawnWeight, Optional.of(new Equipment(slot, bonus)));
    }

    /** Un oggetto da lanciare, che si impila nell'inventario e si consuma uno alla volta. */
    public static ItemDefinition throwable(Kind kind, Throwing throwing, int spawnWeight) {
        return new ItemDefinition(
                kind, ItemEffect.NONE, false, spawnWeight, Optional.empty(), Optional.of(throwing));
    }

    /** Vero per gli oggetti che occupano un solo posto nell'inventario, con un numero. */
    public boolean stackable() {
        return throwing.isPresent();
    }
}
