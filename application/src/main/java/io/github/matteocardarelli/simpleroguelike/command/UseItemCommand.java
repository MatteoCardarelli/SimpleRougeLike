package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Usare un oggetto dell'inventario. */
public record UseItemCommand(EntityId itemId) implements Command {}
