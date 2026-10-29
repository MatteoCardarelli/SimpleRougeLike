package io.github.matteocardarelli.simpleroguelike.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.MoveCommand;
import io.github.matteocardarelli.simpleroguelike.command.UseItemCommand;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.LeveledUp;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorPopulator;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.port.FixedTerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.snapshot.ActorView;
import io.github.matteocardarelli.simpleroguelike.snapshot.GameSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

/** Proprietà di una partita intera, su seed e sequenze di comandi casuali. */
class GameSessionProperties {

    private static final FloorFactory FLOORS =
            new FloorFactory(
                    new FixedTerrainGenerator(),
                    new FloorPopulator(
                            new StandardItemCatalog(),
                            new StandardEnemyCatalog(),
                            StandardPlayer.DEFINITION));

    /** Dove sta un attore e quanta salute ha. */
    record Seen(Position position, int health) {}

    /**
     * Applicando allo snapshot precedente gli eventi visibili si ottiene lo snapshot successivo,
     * per ogni attore visibile prima e dopo il turno. Chi entra o esce dal campo visivo non ha un
     * evento che lo dica, ed è escluso.
     */
    @Property(tries = 200)
    void visibleEventsExplainTheNextSnapshot(
            @ForAll long seed, @ForAll("commands") List<Command> commands) {
        GameSession session = session();
        GameSnapshot before = session.newGame(seed).snapshot();

        for (Command command : commands) {
            TurnResult result = session.handle(resolve(command, before));
            GameSnapshot after = result.snapshot();

            Map<EntityId, Seen> expected = new TreeMap<>(seen(before));
            boolean leveledUp = false;
            for (GameEvent event : result.events()) {
                if (event instanceof Moved moved && expected.containsKey(moved.actor())) {
                    Seen old = expected.get(moved.actor());
                    expected.put(moved.actor(), new Seen(moved.to(), old.health()));
                } else if (event instanceof Damaged damaged
                        && expected.containsKey(damaged.target())) {
                    Seen old = expected.get(damaged.target());
                    expected.put(
                            damaged.target(), new Seen(old.position(), damaged.remainingHealth()));
                } else if (event instanceof Healed healed
                        && expected.containsKey(healed.target())) {
                    Seen old = expected.get(healed.target());
                    expected.put(
                            healed.target(),
                            new Seen(old.position(), old.health() + healed.amount()));
                } else if (event instanceof Died died) {
                    expected.remove(died.actor());
                } else if (event instanceof LeveledUp) {
                    leveledUp = true;
                }
            }

            Map<EntityId, Seen> actual = seen(after);
            for (EntityId id : actual.keySet()) {
                if (!expected.containsKey(id)) {
                    continue;
                }
                assertThat(actual.get(id).position())
                        .as("posizione di %s", id)
                        .isEqualTo(expected.get(id).position());
                boolean playerAfterLevelUp = leveledUp && id.equals(after.player().id());
                if (!playerAfterLevelUp) {
                    assertThat(actual.get(id).health())
                            .as("salute di %s", id)
                            .isEqualTo(expected.get(id).health());
                }
            }
            before = after;
        }
    }

    @Property(tries = 100)
    void sameSeedAndCommands_giveSameResults(
            @ForAll long seed, @ForAll("commands") List<Command> commands) {
        assertThat(play(seed, commands)).isEqualTo(play(seed, commands));
    }

    @Provide
    Arbitrary<List<Command>> commands() {
        Arbitrary<Command> move = Arbitraries.of(Direction.class).map(MoveCommand::new);
        Arbitrary<Command> useFirstItem = Arbitraries.just(new UseItemCommand(new EntityId(0)));
        return Arbitraries.frequencyOf(Tuple.of(9, move), Tuple.of(1, useFirstItem))
                .list()
                .ofMaxSize(40);
    }

    private static List<TurnResult> play(long seed, List<Command> commands) {
        GameSession session = session();
        List<TurnResult> results = new ArrayList<>();
        TurnResult last = session.newGame(seed);
        results.add(last);
        for (Command command : commands) {
            last = session.handle(resolve(command, last.snapshot()));
            results.add(last);
        }
        return results;
    }

    /** Il segnaposto "usa il primo oggetto" diventa l'id vero, se l'inventario non è vuoto. */
    private static Command resolve(Command command, GameSnapshot snapshot) {
        if (command instanceof UseItemCommand && !snapshot.inventory().isEmpty()) {
            return new UseItemCommand(snapshot.inventory().get(0).id());
        }
        return command;
    }

    private static Map<EntityId, Seen> seen(GameSnapshot snapshot) {
        TreeMap<EntityId, Seen> seen = new TreeMap<>();
        seen.put(
                snapshot.player().id(),
                new Seen(snapshot.player().position(), snapshot.player().health()));
        for (ActorView enemy : snapshot.visibleEnemies()) {
            seen.put(enemy.id(), new Seen(enemy.position(), enemy.health()));
        }
        return seen;
    }

    private static GameSession session() {
        return Sessions.session(FLOORS, 3, List.of(), List.of());
    }
}
