package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Lanciare un pezzo di un oggetto dell'inventario, in linea retta in una direzione. */
public record ThrowCommand(EntityId itemId, Direction direction) implements Command {}
