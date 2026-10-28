package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;

/**
 * Una strategia registrata con la sua chiave, la stessa costante usata nel catalogo dei nemici. Le
 * strategie composte dentro un'altra non hanno bisogno di una chiave.
 */
public record BehaviorBinding(BehaviorKey key, EnemyBehavior behavior) {}
