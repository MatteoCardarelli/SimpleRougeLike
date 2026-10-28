package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * Il personaggio del giocatore. Parte dal livello 1 con l'inventario vuoto. Vale sempre {@code
 * experience() < experienceToNextLevel()}.
 */
public final class Player extends Actor {

    public static final Kind KIND = Kind.of("player");

    private final Inventory inventory;
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
