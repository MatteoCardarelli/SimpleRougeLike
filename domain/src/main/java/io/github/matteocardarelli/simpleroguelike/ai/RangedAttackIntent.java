package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.Direction;

/** Un colpo a distanza in una direzione, in linea retta fino alla portata del nemico. */
public record RangedAttackIntent(Direction direction) implements EnemyIntent {}
