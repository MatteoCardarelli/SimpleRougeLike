package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.Position;

/**
 * Alzarsi in aria sopra l'ombra con la cella di riferimento in {@code anchor}: al prossimo turno il
 * nemico atterrerà lì.
 */
public record JumpIntent(Position anchor) implements EnemyIntent {}
