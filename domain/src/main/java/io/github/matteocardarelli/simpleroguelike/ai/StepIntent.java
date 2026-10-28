package io.github.matteocardarelli.simpleroguelike.ai;

import io.github.matteocardarelli.simpleroguelike.model.Direction;

/** Un passo in una direzione: uno spostamento, o un attacco se nella cella c'è un ostile. */
public record StepIntent(Direction direction) implements EnemyIntent {}
