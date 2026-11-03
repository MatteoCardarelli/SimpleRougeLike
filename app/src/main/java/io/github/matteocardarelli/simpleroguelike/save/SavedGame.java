package io.github.matteocardarelli.simpleroguelike.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Una partita salvata: le righe della sessione e il registro dei comandi giocati dall'inizio, che
 * servono alle note di collaudo anche dopo la ripresa. Il file ha prima il registro, poi una riga
 * {@code session} e la sessione.
 *
 * @param journalSeed il seed con cui la partita era iniziata
 * @param commands i comandi, una riga ciascuno nel formato delle note di collaudo
 * @param session le righe scritte dalla sessione
 */
public record SavedGame(long journalSeed, List<String> commands, List<String> session) {

    private static final String JOURNAL = "journal ";
    private static final String COMMAND = "cmd ";
    private static final String SESSION = "session";

    public SavedGame {
        commands = List.copyOf(commands);
        session = List.copyOf(session);
    }

    /** Le righe del file. */
    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add(JOURNAL + journalSeed);
        for (String command : commands) {
            lines.add(COMMAND + command);
        }
        lines.add(SESSION);
        lines.addAll(session);
        return lines;
    }

    /**
     * Rilegge le righe scritte da {@link #lines()}.
     *
     * @throws IllegalArgumentException se le righe non hanno questa forma; la sessione la controlla
     *     poi la sessione stessa
     */
    public static SavedGame parse(List<String> lines) {
        if (lines.isEmpty() || !lines.get(0).startsWith(JOURNAL)) {
            throw new IllegalArgumentException("Salvataggio senza la riga journal");
        }
        long seed = Long.parseLong(lines.get(0).substring(JOURNAL.length()));
        List<String> commands = new ArrayList<>();
        int index = 1;
        while (index < lines.size() && lines.get(index).startsWith(COMMAND)) {
            commands.add(lines.get(index).substring(COMMAND.length()));
            index++;
        }
        if (index >= lines.size() || !lines.get(index).equals(SESSION)) {
            throw new IllegalArgumentException("Salvataggio senza la riga session");
        }
        return new SavedGame(seed, commands, lines.subList(index + 1, lines.size()));
    }
}
