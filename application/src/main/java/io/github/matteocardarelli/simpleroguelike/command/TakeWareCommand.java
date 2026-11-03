package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Prendere la merce del negozio su cui sta il player: andrà pagata al mercante. */
public record TakeWareCommand(EntityId itemId) implements Command {}
