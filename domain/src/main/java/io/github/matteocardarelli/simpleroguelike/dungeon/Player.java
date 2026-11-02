package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Il personaggio del giocatore. Parte dal livello 1 con l'inventario vuoto. Vale sempre {@code
 * experience() < experienceToNextLevel()}.
 *
 * <p>Gli oggetti equipaggiati restano nell'inventario, uno per spazio, e le loro statistiche si
 * sommano a quelle di base in {@link #stats()}.
 */
public final class Player extends Actor {

    public static final Kind KIND = Kind.of("player");

    private final Inventory inventory;
    private final Map<EquipmentSlot, Item> equipped = new EnumMap<>(EquipmentSlot.class);
    private int level = 1;
    private int experience;
    private int experienceToNextLevel;

    /**
     * @param experienceToFirstLevel l'esperienza che serve per arrivare al livello 2
     * @param inventoryCapacity quanti oggetti può portare
     */
    public Player(EntityId id, Stats stats, int experienceToFirstLevel, int inventoryCapacity) {
        super(id, stats);
        if (experienceToFirstLevel <= 0) {
            throw new IllegalArgumentException("Soglia non valida: " + experienceToFirstLevel);
        }
        this.experienceToNextLevel = experienceToFirstLevel;
        this.inventory = new Inventory(inventoryCapacity);
    }

    public Player(EntityId id, PlayerDefinition definition) {
        this(
                id,
                definition.stats(),
                definition.experienceToFirstLevel(),
                definition.inventoryCapacity());
    }

    @Override
    public Kind kind() {
        return KIND;
    }

    @Override
    public Faction faction() {
        return Faction.PLAYER;
    }

    public int level() {
        return level;
    }

    public int experience() {
        return experience;
    }

    public int experienceToNextLevel() {
        return experienceToNextLevel;
    }

    public Inventory inventory() {
        return inventory;
    }

    /** Le statistiche di base più i bonus di tutto quello che il player ha equipaggiato. */
    @Override
    public Stats stats() {
        Stats total = baseStats();
        for (Item item : equipped.values()) {
            total = total.plus(equipmentOf(item).bonus());
        }
        return total;
    }

    /** Gli oggetti equipaggiati, per spazio, nell'ordine degli spazi. */
    public Map<EquipmentSlot, Item> equipped() {
        return Collections.unmodifiableMap(equipped);
    }

    public boolean isEquipped(EntityId itemId) {
        for (Item item : equipped.values()) {
            if (item.id().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Equipaggia un oggetto dell'inventario nel suo spazio.
     *
     * @return l'oggetto che occupava lo spazio, se c'era
     */
    Optional<Item> equip(Item item) {
        Equipment equipment = equipmentOf(item);
        Optional<Item> replaced = Optional.ofNullable(equipped.put(equipment.slot(), item));
        clampHealth();
        return replaced;
    }

    /** Toglie l'oggetto dal suo spazio; la salute resta entro il nuovo massimo. */
    Optional<Item> unequip(Item item) {
        EquipmentSlot slot = equipmentOf(item).slot();
        Item current = equipped.get(slot);
        if (current == null || !current.id().equals(item.id())) {
            return Optional.empty();
        }
        equipped.remove(slot);
        clampHealth();
        return Optional.of(item);
    }

    private static Equipment equipmentOf(Item item) {
        return item.equipment()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        item.kind().value() + " non si equipaggia"));
    }

    /**
     * Aggiunge l'esperienza e applica tutti i livelli guadagnati, anche più di uno.
     *
     * @return quanti livelli sono stati guadagnati
     */
    int gainExperience(int amount, LevelingPolicy policy) {
        if (amount < 0) {
            throw new IllegalArgumentException("Esperienza negativa: " + amount);
        }
        experience = Math.addExact(experience, amount);
        int gained = 0;
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            int next = policy.nextThreshold(experienceToNextLevel);
            if (next <= experienceToNextLevel) {
                throw new IllegalStateException(
                        "La soglia deve crescere: " + experienceToNextLevel + " -> " + next);
            }
            experienceToNextLevel = next;
            level++;
            raiseStats(policy.bonusPerLevel());
            gained++;
        }
        return gained;
    }
}
