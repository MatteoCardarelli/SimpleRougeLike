package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Togliersi un oggetto equipaggiato, che resta nell'inventario. */
public record UnequipCommand(EntityId itemId) implements Command {}
