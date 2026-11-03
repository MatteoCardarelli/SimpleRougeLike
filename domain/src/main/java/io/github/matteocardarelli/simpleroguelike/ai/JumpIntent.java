package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Annunciare un salto: al prossimo turno il nemico atterrerà con la cella di riferimento in {@code
 * anchor}.
 */
public record JumpIntent(Position anchor) implements EnemyIntent {}
