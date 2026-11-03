package io.github.matteocardarelli.simpleroguelike.snapshot;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;

/** La merce del negozio su cui sta il player, con il suo prezzo in monete. */
public record WareView(EntityId item, Kind kind, int price) {}
