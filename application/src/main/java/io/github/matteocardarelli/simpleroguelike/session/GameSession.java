package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.CommandDispatcher;
import io.github.matteocardarelli.simpleroguelike.command.CommandOutcome;
import io.github.matteocardarelli.simpleroguelike.dungeon.FixtureType;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.FloorChanged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.port.FieldOfView;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import io.github.matteocardarelli.simpleroguelike.state.GameState;
import io.github.matteocardarelli.simpleroguelike.state.PlayingState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * L'unico punto d'ingresso per l'interfaccia: crea le partite ed esegue i comandi, un turno
 * completo alla volta.
 *
 * <p>Un turno consumato fa agire i nemici, poi gli hook in ordine, poi toglie i morti e avanza il
 * turno. La morte del player chiude il turno: dopo non agisce più nessuno, qualunque sia la causa.
 * Alla fine aggiorna il campo visivo e la fase, e restituisce gli eventi visibili e lo snapshot.
 *
 * <p>Non è thread-safe: si usa dal thread principale.
 */
public final class GameSession {

    private final FloorSource floors;
    private final CommandDispatcher commands;
    private final EnemyTurnRunner enemies;
    private final FieldOfView fov;
    private final List<TurnHook> hooks;
    private final List<FloorSpec> floorSpecs;
    private final int visionRadius;
    private final VisibilityFilter visibility = new VisibilityFilter();
    private final SnapshotMapper mapper = new SnapshotMapper();

    private @Nullable Run run;
    private GameState state = new PlayingState();
    private long seed;
    private long revision;
    private @Nullable TurnResult last;

    /**
     * Crea la sessione, senza ancora una partita.
     *
     * @throws IllegalArgumentException se due hook hanno lo stesso ordine o il raggio non è
     *     positivo
     */
    public GameSession(
            FloorSource floors,
            CommandDispatcher commands,
            EnemyTurnRunner enemies,
            FieldOfView fov,
            Collection<TurnHook> hooks,
            FloorSpec spec,
            int visionRadius) {
        this(floors, commands, enemies, fov, hooks, List.of(spec), visionRadius);
    }

    /**
     * Crea la sessione per una partita su più piani, uno per specifica: si scende con le scale e si
     * vince solo sull'ultimo.
     *
     * @throws IllegalArgumentException se due hook hanno lo stesso ordine, il raggio non è positivo
     *     o non c'è nessun piano
     */
    public GameSession(
            FloorSource floors,
            CommandDispatcher commands,
            EnemyTurnRunner enemies,
            FieldOfView fov,
            Collection<TurnHook> hooks,
            List<FloorSpec> floorSpecs,
            int visionRadius) {
        if (floorSpecs.isEmpty()) {
            throw new IllegalArgumentException("Una partita ha almeno un piano");
        }
        if (visionRadius <= 0) {
            throw new IllegalArgumentException("Raggio visivo non valido: " + visionRadius);
        }
        List<TurnHook> sorted = new ArrayList<>(hooks);
        sorted.sort(Comparator.comparingInt(TurnHook::order));
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).order() == sorted.get(i - 1).order()) {
                throw new IllegalArgumentException(
                        "Due hook con lo stesso ordine: " + sorted.get(i).order());
            }
        }
        this.floors = floors;
        this.commands = commands;
        this.enemies = enemies;
        this.fov = fov;
        this.hooks = List.copyOf(sorted);
        this.floorSpecs = List.copyOf(floorSpecs);
        this.visionRadius = visionRadius;
    }

    /** Inizia una partita nuova: dallo stesso seed esce sempre la stessa. */
    public TurnResult newGame(long seed) {
        Run started = Run.start(seed, floorSpecs.get(0), floors);
        this.run = started;
        this.seed = seed;
        updateVisibility(started);
        this.state = new PlayingState().next(started.view(), floorSpecs.size() == 1);
        return remember(true, List.of(), started);
    }

    /**
     * Esegue un comando. Se la fase non lo accetta non succede niente, ma il risultato ha comunque
     * una revisione nuova.
     *
     * @throws IllegalStateException se non c'è ancora una partita
     * @throws IllegalArgumentException se nessun handler gestisce il comando
     */
    public TurnResult handle(Command command) {
        Run current = requireRun();
        if (!state.accepts(command)) {
            return remember(false, List.of(), current);
        }
        Floor floor = current.floor();
        CellSet visibleBefore = floor.visible();
        CommandOutcome outcome = commands.dispatch(command, current);
        List<GameEvent> events = new ArrayList<>(outcome.events());
        if (outcome.turnConsumed() && onStairs(current)) {
            // Le scale chiudono il turno: i nemici del piano di prima non agiscono più. Gli eventi
            // del passo si filtrano sul piano di prima, la discesa la vede sempre.
            List<GameEvent> seen =
                    new ArrayList<>(visibility.filter(events, visibleBefore, visibleBefore));
            current.descend(floors, floorSpecs.get(current.depth()));
            seen.add(new FloorChanged(current.depth()));
            current.endTurn();
            updateVisibility(current);
            state = state.next(current.view(), isLastFloor(current));
            return remember(true, seen, current);
        }
        if (outcome.turnConsumed()) {
            if (floor.player().isAlive()) {
                events.addAll(enemies.run(current));
            }
            for (TurnHook hook : hooks) {
                if (!floor.player().isAlive()) {
                    break;
                }
                events.addAll(hook.afterTurn(current));
            }
            floor.removeDead();
            current.endTurn();
        }
        updateVisibility(current);
        state = state.next(current.view(), isLastFloor(current));
        return remember(true, visibility.filter(events, visibleBefore, floor.visible()), current);
    }

    /**
     * L'ultimo risultato, lo stesso oggetto con la stessa revisione: chiamarlo non produce niente
     * di nuovo.
     *
     * @throws IllegalStateException se non c'è ancora una partita
     */
    public TurnResult current() {
        TurnResult result = last;
        if (result == null) {
            throw new IllegalStateException("Nessuna partita: chiamare prima newGame");
        }
        return result;
    }

    /** Vero se il player vivo sta sulle scale e c'è un piano sotto. */
    private boolean onStairs(Run current) {
        Floor floor = current.floor();
        if (!floor.player().isAlive() || current.depth() >= floorSpecs.size()) {
            return false;
        }
        return floor.fixtureAt(floor.positionOf(floor.playerId()))
                .map(fixture -> fixture.type() == FixtureType.STAIRS)
                .orElse(false);
    }

    private boolean isLastFloor(Run current) {
        return current.depth() >= floorSpecs.size();
    }

    private void updateVisibility(Run current) {
        Floor floor = current.floor();
        floor.updateVisibility(
                fov.visibleFrom(
                        floor.sightGrid(), floor.positionOf(floor.playerId()), visionRadius));
    }

    private TurnResult remember(boolean accepted, List<GameEvent> events, Run current) {
        TurnResult result =
                new TurnResult(
                        ++revision,
                        accepted,
                        events,
                        mapper.from(
                                current.view(),
                                state.phase(),
                                seed,
                                current.turn(),
                                current.depth(),
                                floorSpecs.size()));
        last = result;
        return result;
    }

    private Run requireRun() {
        Run current = run;
        if (current == null) {
            throw new IllegalStateException("Nessuna partita: chiamare prima newGame");
        }
        return current;
    }
}
