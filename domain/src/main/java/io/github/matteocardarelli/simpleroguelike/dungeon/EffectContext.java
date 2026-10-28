package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;

/**
 * Quello che un effetto può usare: chi usa l'oggetto, il piano e il flusso casuale di gioco. Lo
 * crea {@link Floor#useItem}, quindi {@code user} è sempre chi usa l'oggetto.
 */
public record EffectContext(EntityId user, Floor floor, RandomSource random) {}
