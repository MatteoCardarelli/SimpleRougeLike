package io.github.matteocardarelli.simpleroguelike.rules;

import io.github.matteocardarelli.simpleroguelike.model.Stats;

/** Le regole con i valori del gioco, per i test. */
final class Rules {

    static final LevelingRules LEVELING = new LevelingRules(new Stats(20, 2), 150);
    static final DamageRules DAMAGE = new DamageRules(LEVELING);
    static final CombatRules COMBAT = new CombatRules(DAMAGE);
    static final ActionRules ACTIONS = new ActionRules(COMBAT);

    private Rules() {}
}
