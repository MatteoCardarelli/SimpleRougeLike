package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.run.Run;

/**
 * Esegue un tipo di comando. Riceve dal costruttore i servizi che gli servono e dalla chiamata la
 * partita su cui agire.
 *
 * @param <C> il tipo di comando gestito
 */
public interface CommandHandler<C extends Command> {

    /** La classe del comando gestito: è la chiave con cui il dispatcher sceglie l'handler. */
    Class<C> commandType();

    CommandOutcome handle(C command, Run run);
}
