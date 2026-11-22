package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.rules.PoisonRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;

/** Il veleno: a fine turno ogni attore avvelenato subisce il suo danno, una volta per turno. */
public final class PoisonHook implements TurnHook {

    /** Prima del bottino, perché il veleno può uccidere. */
    public static final int ORDER = 10;

    private final PoisonRules poison;

    public PoisonHook(PoisonRules poison) {
        this.poison = poison;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public List<GameEvent> afterTurn(Run run) {
        return poison.tick(run.floor());
    }
}
