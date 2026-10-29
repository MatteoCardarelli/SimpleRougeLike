package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/** Un nemico vivo in una cella visibile. */
public record ActorView(EntityId id, Kind kind, Position position, int health, int maxHealth) {}
