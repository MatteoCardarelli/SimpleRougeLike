package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Lasciare a terra un oggetto dell'inventario, nella cella del player. */
public record DropItemCommand(EntityId itemId) implements Command {}
