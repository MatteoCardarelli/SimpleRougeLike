package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.rules.TrapRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;

/**
 * La scoperta delle trappole: a fine turno ogni trappola nascosta nelle quattro celle accanto al
 * player si scopre una volta su tre, con un tiro del flusso di gameplay, lo stesso del
 * combattimento. È un hook e non parte del passo perché il tiro è una cosa del turno, non del
 * movimento: vale anche se il player resta fermo o attacca accanto a una trappola. Prima del
 * veleno, che può uccidere; con il player morto l'hook non gira. Lo scatto di una trappola, invece,
 * sta nel passo del player ({@code ActionRules}): deve avvenire appena ci entra.
 */
public final class TrapDiscoveryHook implements TurnHook {

    /** Prima del veleno e del bottino. */
    public static final int ORDER = 5;

    private final TrapRules traps;

    public TrapDiscoveryHook(TrapRules traps) {
        this.traps = traps;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public List<GameEvent> afterTurn(Run run) {
        return traps.discover(run.floor(), run.gameplay());
    }
}
