package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Optional;

/** Un nemico. I suoi valori vengono dalla definizione del suo tipo. */
public final class Enemy extends Actor {

    private final EnemyDefinition definition;
    private Optional<Position> pendingJump = Optional.empty();
    private boolean tired;
    private boolean angry;

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
        return definition.merchant() && !angry ? Faction.NEUTRAL : Faction.MONSTERS;
    }

    public boolean isMerchant() {
        return definition.merchant();
    }

    /** Vero per un mercante derubato: da lì in poi è un nemico. */
    public boolean isAngry() {
        return angry;
    }

    void anger() {
        angry = true;
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

    @Override
    public Footprint footprint() {
        return definition.footprint();
    }

    public boolean isBoss() {
        return definition.boss();
    }

    /** Dove atterrerà al prossimo turno, se ha annunciato un salto. */
    public Optional<Position> pendingJump() {
        return pendingJump;
    }

    /** Vero subito dopo un atterraggio: il turno dopo riposa. */
    public boolean isTired() {
        return tired;
    }

    void announceJump(Position anchor) {
        pendingJump = Optional.of(anchor);
    }

    void landed() {
        pendingJump = Optional.empty();
        tired = true;
    }

    void rested() {
        tired = false;
    }

    public EnemyDefinition definition() {
        return definition;
    }
}
