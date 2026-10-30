package io.github.matteocardarelli.simpleroguelike.di;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.matteocardarelli.simpleroguelike.snapshot.Phase;
import io.github.matteocardarelli.simpleroguelike.snapshot.PlayerView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Una partita con seed e comandi fissi, giocata con gli adapter, le regole e i contenuti veri, deve
 * dare sempre lo stesso registro: quello salvato in {@code src/test/resources/golden}. Se cambia
 * una regola del gioco il registro cambia di proposito, e si rigenera con {@code ./gradlew
 * :app:updateGolden}; se cambia senza motivo, la partita non è più deterministica.
 *
 * <p>Il registro è scritto da {@link EventLogWriter}, con un formato fatto a mano: lo stesso file
 * servirà a confrontare la partita giocata sul telefono.
 */
class GoldenGameTest {

    private static final long SEED = 20_261_029L;
    private static final int MAX_COMMANDS = 400;
    private static final String FILE = "standard-game.txt";
    private static final CommandLogFormat FORMAT = new CommandLogFormat();

    @Test
    void standardGame_matchesGoldenLog() throws IOException {
        List<String> log = play();
        Path golden =
                Path.of(
                        Objects.requireNonNull(
                                System.getProperty("srl.goldenDir"),
                                "La cartella del golden la passa Gradle: eseguire con Gradle"),
                        FILE);

        if (Boolean.getBoolean("srl.updateGolden")) {
            Files.createDirectories(golden.getParent());
            // Fine riga \n su ogni sistema: i test di :app compilano contro le API di Android,
            // che non hanno Files.writeString.
            Files.write(golden, (String.join("\n", log) + "\n").getBytes(StandardCharsets.UTF_8));
            return;
        }

        assertThat(golden).as("file atteso, da creare con ./gradlew :app:updateGolden").exists();
        assertThat(log).containsExactlyElementsOf(Files.readAllLines(golden));
    }

    @Test
    void standardGame_whenPlayedTwice_givesSameLog() {
        assertThat(play()).isEqualTo(play());
    }

    /**
     * Gioca la partita: un giocatore automatico va verso il nemico visibile più vicino e beve una
     * pozione quando la salute scende sotto la metà; altrimenti esplora, andando dritto finché non
     * urta qualcosa e poi cambiando direzione a caso. Le sue scelte dipendono solo dallo snapshot e
     * da un flusso casuale con un seed fisso.
     */
    private static List<String> play() {
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
        if (player.health() * 2 < player.maxHealth() && !snapshot.inventory().isEmpty()) {
            UseItemCommand use = new UseItemCommand(snapshot.inventory().get(0).id());
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
