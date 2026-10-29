package io.github.matteocardarelli.simpleroguelike.feedback;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import java.util.ArrayList;
import java.util.List;

/**
 * I comandi della partita in corso, dal suo inizio, nell'ordine in cui il giocatore li ha dati,
 * accettati o no. La partita è deterministica: con il seed bastano a rigiocarla identica.
 */
public final class CommandJournal {

    private long seed;
    private final List<Command> commands = new ArrayList<>();

    /** Inizia il registro di una partita nuova, dimenticando quella di prima. */
    public void start(long seed) {
        this.seed = seed;
        commands.clear();
    }

    public void record(Command command) {
        commands.add(command);
    }

    public long seed() {
        return seed;
    }

    /** I comandi registrati, in una copia che non cambia con i comandi successivi. */
    public List<Command> commands() {
        return List.copyOf(commands);
    }
}
