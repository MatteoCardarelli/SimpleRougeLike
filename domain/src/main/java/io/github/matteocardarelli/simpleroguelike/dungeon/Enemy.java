package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

/** Un nemico. I suoi valori vengono dalla definizione del suo tipo. */
public final class Enemy extends Actor {

    private final EnemyDefinition definition;

    public Enemy(EntityId id, EnemyDefinition definition) {
        super(id, definition.stats());
        this.definition = definition;
    }

    @Override
    public Kind kind() {
        return definition.kind();
    }

    @Override
    public Faction faction() {
        return Faction.MONSTERS;
    }

    public int experienceReward() {
        return definition.experienceReward();
    }

    public int aggroRange() {
        return definition.aggroRange();
    }

    /** Quante celle raggiunge il suo colpo a distanza; 0 se combatte solo da vicino. */
    public int range() {
        return definition.range();
    }

    public BehaviorKey behavior() {
        return definition.behavior();
    }

    public EnemyDefinition definition() {
        return definition;
    }
}
