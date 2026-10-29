package io.github.matteocardarelli.simpleroguelike.feedback;

import io.github.matteocardarelli.simpleroguelike.di.GameConfig;
import io.github.matteocardarelli.simpleroguelike.di.GameSessionFactory;
import io.github.matteocardarelli.simpleroguelike.event.EventLogWriter;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Rigioca sulla JVM la partita di una nota di collaudo: stessa configurazione dell'app, stesso
 * seed, stessi comandi. Sullo stesso commit la partita arriva esattamente allo stato salvato.
 */
public final class FeedbackReplay {

    /** L'ultimo risultato della partita rigiocata e il suo registro, una riga per evento. */
    public record Result(TurnResult last, List<String> log) {}

    private FeedbackReplay() {}

    public static Result play(FeedbackEntry note) {
        GameSession session = GameSessionFactory.create(GameConfig.standard());
        CommandLogFormat format = new CommandLogFormat();
        EventLogWriter writer = new EventLogWriter();
        List<String> log = new ArrayList<>();

        TurnResult result = session.newGame(note.seed());
        log.add("seed=" + note.seed());
        for (String line : note.commands()) {
            int turn = result.snapshot().turn();
            log.add("t" + turn + " > " + line);
            result = session.handle(format.parse(line));
            log.addAll(writer.lines(turn, result.events()));
        }
        return new Result(result, List.copyOf(log));
    }

    /**
     * Dove la partita rigiocata è diversa da quella della nota: turno, fase e player. La revisione
     * non si confronta, perché nell'app cresce anche tra una partita e l'altra.
     *
     * @return vuoto se coincidono
     */
    public static List<String> differences(FeedbackEntry note, GameSnapshot replayed) {
        List<String> differences = new ArrayList<>();
        if (note.turn() != replayed.turn()) {
            differences.add(
                    "turno " + note.turn() + " nella nota, " + replayed.turn() + " rigiocato");
        }
        if (note.phase() != replayed.phase()) {
            differences.add(
                    "fase " + note.phase() + " nella nota, " + replayed.phase() + " rigiocata");
        }
        PlayerView saved = note.player();
        PlayerView player = replayed.player();
        if (!saved.equals(player)) {
            differences.add("player " + saved + " nella nota, " + player + " rigiocato");
        }
        return List.copyOf(differences);
    }
}
