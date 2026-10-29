package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Sceglie l'handler di un comando in base alla sua classe. È un registro, non una catena di {@code
 * instanceof}: un comando nuovo si registra con il suo handler e il codice esistente non cambia.
 */
public final class CommandDispatcher {

    private final TreeMap<String, CommandHandler<?>> handlers = new TreeMap<>();

    /**
     * Registra gli handler.
     *
     * @throws IllegalArgumentException se due handler dichiarano lo stesso tipo di comando
     */
    public CommandDispatcher(Collection<? extends CommandHandler<?>> handlers) {
        for (CommandHandler<?> handler : handlers) {
            String type = handler.commandType().getName();
            if (this.handlers.putIfAbsent(type, handler) != null) {
                throw new IllegalArgumentException("Due handler per " + type);
            }
        }
    }

    /**
     * Esegue il comando con il suo handler.
     *
     * @throws IllegalArgumentException se nessun handler gestisce la classe del comando
     */
    public CommandOutcome dispatch(Command command, Run run) {
        CommandHandler<?> handler = handlers.get(command.getClass().getName());
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Nessun handler per " + command.getClass().getName());
        }
        return handle(handler, command, run);
    }

    private static <C extends Command> CommandOutcome handle(
            CommandHandler<C> handler, Command command, Run run) {
        return handler.handle(handler.commandType().cast(command), run);
    }
}
