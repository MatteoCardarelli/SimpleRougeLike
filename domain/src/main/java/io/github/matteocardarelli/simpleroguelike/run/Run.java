package io.github.matteocardarelli.simpleroguelike.run;

import io.github.matteocardarelli.simpleroguelike.dungeon.EntityIdSequence;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.FloorView;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;

/**
 * Una partita: tutto lo stato che cambia mentre si gioca. Il piano, la sequenza degli id, i due
 * flussi casuali e il numero di turno nascono qui e qui muoiono: niente sopravvive da una partita
 * all'altra.
 *
 * <p>I flussi casuali sono due e indipendenti: la generazione dei piani e il gioco. Cambiare il
 * modo in cui i nemici usano il caso non cambia la mappa che esce dallo stesso seed. Non è
 * thread-safe.
 */
public final class Run {

    private final Floor floor;
    private final EntityIdSequence ids;
    private final RandomSource generation;
    private final RandomSource gameplay;
    private int turn = 1;

    private Run(Floor floor, EntityIdSequence ids, RandomSource generation, RandomSource gameplay) {
        this.floor = floor;
        this.ids = ids;
        this.generation = generation;
        this.gameplay = gameplay;
    }

    /** Inizia una partita: dallo stesso seed esce sempre la stessa. */
    public static Run start(long seed, FloorSpec spec, FloorFactory floors) {
        SeededRandomSource root = new SeededRandomSource(seed);
        RandomSource generation = root.split();
        RandomSource gameplay = root.split();
        EntityIdSequence ids = new EntityIdSequence();
        Floor floor = floors.create(spec, generation, ids);
        return new Run(floor, ids, generation, gameplay);
    }

    public Floor floor() {
        return floor;
    }

    /** Il piano in sola lettura, per chi deve solo guardare. */
    public FloorView view() {
        return floor;
    }

    public EntityIdSequence ids() {
        return ids;
    }

    /** Il flusso casuale per creare piani. */
    public RandomSource generation() {
        return generation;
    }

    /** Il flusso casuale del gioco: comportamenti dei nemici ed effetti. */
    public RandomSource gameplay() {
        return gameplay;
    }

    /** Il turno in corso, da 1. */
    public int turn() {
        return turn;
    }

    public void endTurn() {
        turn++;
    }
}
