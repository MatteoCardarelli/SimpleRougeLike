package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;

/** Un oggetto a terra in una cella visibile. */
public record ItemView(EntityId id, Kind kind, Position position) {}
