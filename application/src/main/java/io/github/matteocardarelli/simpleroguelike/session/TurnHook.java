package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;

/**
 * Una regola che scatta alla fine di ogni turno consumato, dopo i nemici, se il player è ancora
 * vivo: rigenerazione, veleno, comparsa di nemici. Un hook che infligge danno usa {@code
 * DamageRules}.
 */
public interface TurnHook {

    /** L'ordine tra gli hook: prima i numeri più bassi. Due hook non possono avere lo stesso. */
    int order();

    List<GameEvent> afterTurn(Run run);
}
