package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.event.EventLogWriter;
import io.github.matteocardarelli.simpleroguelike.feedback.CommandLogFormat;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.random.SeededRandomSource;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * La partita del test golden: seed e comandi fissi, con gli adapter, le regole e i contenuti veri.
 * La giocano sia il test sulla JVM sia quello sul dispositivo, e i due registri devono coincidere
 * con il file atteso: è la prova che la partita è la stessa sulla JVM e su Android.
 *
 * <p>Il registro è scritto da {@link EventLogWriter}, con un formato fatto a mano: niente {@code
 * toString()} dei record né nomi di classi, che su Android cambiano.
 */
public final class GoldenGame {

    /** Il nome del file atteso, nella cartella del golden. */
    public static final String FILE = "standard-game.txt";

    private static final long SEED = 20_261_029L;
    private static final int MAX_COMMANDS = 400;
    private static final CommandLogFormat FORMAT = new CommandLogFormat();

    private GoldenGame() {}

    /**
     * Gioca la partita: un giocatore automatico va verso il nemico visibile più vicino e beve una
     * pozione quando la salute scende sotto la metà; altrimenti esplora, andando dritto finché non
     * urta qualcosa e poi cambiando direzione a caso. Le sue scelte dipendono solo dallo snapshot e
     * da un flusso casuale con un seed fisso.
     */
    public static List<String> play() {
        return play(session -> session);
    }

    /**
     * Gioca la stessa partita, passando la sessione da {@code between} dopo ogni comando: per
     * esempio per salvarla e riprenderla in una sessione nuova. Se {@code between} non cambia
     * niente, il registro è quello di {@link #play()}.
     */
    public static List<String> play(UnaryOperator<GameSession> between) {
        GameSession session = TestGraph.session(GameConfig.standard());
        EventLogWriter writer = new EventLogWriter();
        RandomSource wandering = new SeededRandomSource(SEED);
        List<String> log = new ArrayList<>();

        TurnResult result = session.newGame(SEED);
        log.add("seed=" + SEED);
        log.add(summary(result.snapshot()));
        Direction heading = randomDirection(wandering);
        boolean stuck = false;
        for (int i = 0; i < MAX_COMMANDS && result.snapshot().phase() == Phase.PLAYING; i++) {
            GameSnapshot before = result.snapshot();
            Choice choice = next(before, heading, stuck);
            log.add("t" + before.turn() + " > " + choice.description());
            result = session.handle(choice.command());
            log.addAll(writer.lines(before.turn(), result.events()));
            if (result.snapshot().phase() == Phase.PLAYING) {
                session = between.apply(session);
                result = session.current();
            }
            // Un passo contro un muro lascia il player fermo, anche se il turno passa: si cambia
            // direzione.
            stuck = choice.intoWallOf(before);
            if (stuck) {
                heading = randomDirection(wandering);
            }
        }
        log.add(summary(result.snapshot()));
        return log;
    }

    /**
     * Un comando con la sua riga nel registro, scritta quando lo si sceglie.
     *
     * @param direction la direzione del passo; vuota se il comando non è un passo
     */
    private record Choice(Command command, String description, Optional<Direction> direction) {

        boolean intoWallOf(GameSnapshot snapshot) {
            return direction
                    .map(d -> snapshot.player().position().translate(d))
                    .map(cell -> snapshot.cells().get(cell).wall())
                    .orElse(false);
        }
    }

    private static Choice next(GameSnapshot snapshot, Direction heading, boolean stuck) {
        PlayerView player = snapshot.player();
        Optional<InventoryEntry> potion =
                snapshot.inventory().stream().filter(InventoryEntry::usable).findFirst();
        if (player.health() * 2 < player.maxHealth() && potion.isPresent()) {
            UseItemCommand use = new UseItemCommand(potion.get().id());
            return new Choice(use, FORMAT.line(use), Optional.empty());
        }
        Optional<ActorView> target =
                snapshot.visibleEnemies().stream()
                        .min(
                                Comparator.comparingInt(
                                        enemy -> enemy.position().distanceTo(player.position())));
        // Dopo un urto esplora: verso il nemico urterebbe di nuovo lo stesso muro.
        if (target.isPresent() && !stuck) {
            return move(towards(player.position(), target.get().position()));
        }
        return move(heading);
    }

    private static Direction randomDirection(RandomSource random) {
        return Direction.values()[random.nextInt(Direction.values().length)];
    }

    /** La riga è quella delle note di collaudo: lo stesso formato, scritto a mano. */
    private static Choice move(Direction direction) {
        MoveCommand move = new MoveCommand(direction);
        return new Choice(move, FORMAT.line(move), Optional.of(direction));
    }

    private static Direction towards(Position from, Position to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy > 0 ? Direction.DOWN : Direction.UP;
    }

    private static String summary(GameSnapshot snapshot) {
        PlayerView player = snapshot.player();
        return "t"
                + snapshot.turn()
                + " phase="
                + phase(snapshot)
                + " player="
                + player.position().x()
                + ","
                + player.position().y()
                + " hp="
                + player.health()
                + "/"
                + player.maxHealth()
                + " level="
                + player.level()
                + " xp="
                + player.experience()
                + "/"
                + player.experienceToNextLevel()
                + " enemies="
                + snapshot.visibleEnemies().size()
                + " items="
                + snapshot.visibleItems().size()
                + " inventory="
                + snapshot.inventory().size();
    }

    /** Scritta a mano, come le direzioni. */
    private static String phase(GameSnapshot snapshot) {
        return switch (snapshot.phase()) {
            case PLAYING -> "playing";
            case GAME_OVER -> "game_over";
            case VICTORY -> "victory";
        };
    }
}
