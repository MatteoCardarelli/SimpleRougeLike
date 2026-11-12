package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Faction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import java.util.Optional;

/**
 * Un nemico. I suoi valori vengono dalla definizione del suo tipo e dal suo livello, che è quello
 * del piano dove nasce: più si scende, più è forte e più esperienza dà. Boss e mercante hanno
 * sempre i valori della definizione.
 */
public final class Enemy extends Actor {

    private final EnemyDefinition definition;
    private Optional<Position> pendingJump = Optional.empty();
    private JumpPhase jumpPhase = JumpPhase.RESTING;
    private boolean angry;
    private boolean retreated;
    private final int level;
    private final Optional<EntityId> pack;

    public Enemy(EntityId id, EnemyDefinition definition) {
        this(id, definition, 1);
    }

    /** Un nemico che non fa parte di un branco. */
    public Enemy(EntityId id, EnemyDefinition definition, int level) {
        this(id, definition, level, Optional.empty());
    }

    /**
     * @param level il livello, da 1: ogni livello oltre il primo dà un quarto di salute ed
     *     esperienza in più, 2 di attacco, 3 di precisione, 1 di schivata e mezzo punto di difesa
     * @param pack l'id del suo branco, quello del primo membro; vuoto per chi va da solo
     * @throws IllegalArgumentException se il livello è minore di 1
     */
    public Enemy(EntityId id, EnemyDefinition definition, int level, Optional<EntityId> pack) {
        super(id, statsAt(definition, level));
        this.definition = definition;
        this.level = scales(definition) ? level : 1;
        this.pack = pack;
    }

    /**
     * L'id del branco, quello del primo membro: i membri decidono insieme, come una mente sola.
     * Vuoto per chi va da solo.
     */
    public Optional<EntityId> pack() {
        return pack;
    }

    private static boolean scales(EnemyDefinition definition) {
        return !definition.boss() && !definition.merchant();
    }

    private static Stats statsAt(EnemyDefinition definition, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Livello non valido: " + level);
        }
        Stats base = definition.stats();
        if (!scales(definition) || level == 1) {
            return base;
        }
        int extra = level - 1;
        return new Stats(
                base.maxHealth() * (4 + extra) / 4,
                base.attack() + 2 * extra,
                base.defense() + extra / 2,
                base.accuracy() + 3 * extra,
                base.evasion() + extra);
    }

    /** Il livello del nemico, da 1. */
    public int level() {
        return level;
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
        return definition.experienceReward() * (3 + level) / 4;
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

    /** Dove atterrerà al prossimo turno, se è in aria: la cella di riferimento della sua ombra. */
    public Optional<Position> pendingJump() {
        return pendingJump;
    }

    /** Il punto del ciclo del salto in cui si trova; {@link JumpPhase#RESTING} se non salta. */
    public JumpPhase jumpPhase() {
        return jumpPhase;
    }

    /** Vero mentre è in aria: non occupa celle e non si può colpire. */
    public boolean isAirborne() {
        return jumpPhase == JumpPhase.AIRBORNE;
    }

    /** Vero se la sua ultima azione è stata allontanarsi dal player: per ora non scappa più. */
    public boolean hasJustRetreated() {
        return retreated;
    }

    void retreated(boolean value) {
        retreated = value;
    }

    /** Rimette lo stato salvato; solo per riprendere una partita. */
    void restoreState(JumpPhase phase, Optional<Position> pendingJump, boolean angry) {
        if ((phase == JumpPhase.AIRBORNE) != pendingJump.isPresent()) {
            throw new IllegalArgumentException("Solo un nemico in aria ha un'ombra: " + phase);
        }
        this.jumpPhase = phase;
        this.pendingJump = pendingJump;
        this.angry = angry;
    }

    void rested() {
        jumpPhase = JumpPhase.READY;
    }

    void charged() {
        jumpPhase = JumpPhase.CHARGING;
    }

    void liftedOff(Position anchor) {
        jumpPhase = JumpPhase.AIRBORNE;
        pendingJump = Optional.of(anchor);
    }

    void landed() {
        jumpPhase = JumpPhase.RESTING;
        pendingJump = Optional.empty();
    }

    public EnemyDefinition definition() {
        return definition;
    }
}
