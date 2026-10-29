package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.Direction;

/** Un passo del player: uno spostamento, o un attacco se nella cella c'è un nemico. */
public record MoveCommand(Direction direction) implements Command {}
