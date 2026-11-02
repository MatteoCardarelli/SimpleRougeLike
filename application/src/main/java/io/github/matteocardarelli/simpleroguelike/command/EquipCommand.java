package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Equipaggiare un oggetto dell'inventario. */
public record EquipCommand(EntityId itemId) implements Command {}
