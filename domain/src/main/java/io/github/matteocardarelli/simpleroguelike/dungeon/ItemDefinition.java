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
 * @param key che porte apre, se l'oggetto è una chiave
 */
public record ItemDefinition(
        Kind kind,
        ItemEffect effect,
        boolean consumable,
        int spawnWeight,
        Optional<Equipment> equipment,
        Optional<Throwing> throwing,
        Optional<KeyType> key) {

    public ItemDefinition {
        if (spawnWeight < 0) {
            throw new IllegalArgumentException("Peso di comparsa negativo: " + spawnWeight);
        }
        if (equipment.isPresent() && consumable) {
            throw new IllegalArgumentException(
                    "Un oggetto da equipaggiare non si consuma: " + kind.value());
        }
        int uses =
                (equipment.isPresent() ? 1 : 0)
                        + (throwing.isPresent() ? 1 : 0)
                        + (key.isPresent() ? 1 : 0);
        if (uses > 1) {
            throw new IllegalArgumentException(
                    "Un oggetto si equipaggia, si lancia o apre porte, una cosa sola: "
                            + kind.value());
        }
    }

    /** Un oggetto che si usa. */
    public ItemDefinition(Kind kind, ItemEffect effect, boolean consumable, int spawnWeight) {
        this(
                kind,
                effect,
                consumable,
                spawnWeight,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /** Un oggetto che si usa o si equipaggia, ma non si lancia. */
    public ItemDefinition(
            Kind kind,
            ItemEffect effect,
            boolean consumable,
            int spawnWeight,
            Optional<Equipment> equipment) {
        this(kind, effect, consumable, spawnWeight, equipment, Optional.empty(), Optional.empty());
    }

    /** Un oggetto che si usa, si equipaggia o si lancia, ma non è una chiave. */
    public ItemDefinition(
            Kind kind,
            ItemEffect effect,
            boolean consumable,
            int spawnWeight,
            Optional<Equipment> equipment,
            Optional<Throwing> throwing) {
        this(kind, effect, consumable, spawnWeight, equipment, throwing, Optional.empty());
    }

    /**
     * Una chiave: non si usa, si impila e la consuma la porta che apre. Non compare a caso sul
     * piano: la mette la generazione dove serve.
     */
    public static ItemDefinition key(Kind kind, KeyType type) {
        return new ItemDefinition(
                kind,
                ItemEffect.NONE,
                false,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.of(type));
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

    /**
     * Vero per gli oggetti che si usano: non si equipaggiano, non si lanciano e non sono chiavi.
     */
    public boolean usable() {
        return !equipment.isPresent() && !throwing.isPresent() && !key.isPresent();
    }

    /** Vero per gli oggetti che occupano un solo posto nell'inventario, con un numero. */
    public boolean stackable() {
        return throwing.isPresent() || key.isPresent();
    }
}
