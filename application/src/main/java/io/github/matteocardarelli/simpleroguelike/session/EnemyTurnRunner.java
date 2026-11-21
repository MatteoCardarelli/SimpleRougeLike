package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.ChargeIntent;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyIntent;
import io.github.matteocardarelli.simpleroguelike.ai.JumpIntent;
import io.github.matteocardarelli.simpleroguelike.ai.LandIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RangedAttackIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RestIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RetreatIntent;
import io.github.matteocardarelli.simpleroguelike.ai.StepIntent;
import io.github.matteocardarelli.simpleroguelike.ai.WaitIntent;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.random.RandomSource;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.JumpRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Il turno dei nemici: ognuno, in ordine di id, decide un'intenzione con la sua strategia e la
 * esegue con la stessa regola del player. Si ferma appena il player muore.
 *
 * <p>È l'unico punto che interpreta le intenzioni, con uno {@code switch} esaustivo: un'intenzione
 * nuova non compila finché non viene gestita qui.
 */
public final class EnemyTurnRunner {

    /** La probabilità, in percentuale, che dopo l'atterraggio dello slime cadano dei goblin. */
    static final int FALL_CHANCE = 30;

    /** Quanti goblin cadono al massimo in una volta. */
    static final int FALL_MAX = 2;

    /** Quanti goblin vivi ci sono al massimo nella stanza del boss. */
    static final int ROOM_GOBLIN_CAP = 4;

    private final EnemyCatalog catalog;
    private final Optional<CeilingFall> ceilingFall;
    private final TreeMap<BehaviorKey, EnemyBehavior> behaviors = new TreeMap<>();
    private final ActionRules actions;
    private final RangedRules ranged;
    private final JumpRules jumps;

    /**
     * Registra le strategie.
     *
     * @throws IllegalArgumentException se una chiave è registrata due volte o se un nemico del
     *     catalogo non ha una strategia
     */
    public EnemyTurnRunner(
            Collection<BehaviorBinding> bindings,
            EnemyCatalog catalog,
            ActionRules actions,
            RangedRules ranged,
            JumpRules jumps) {
        this(bindings, catalog, actions, ranged, jumps, Optional.empty());
    }

    /**
     * Come l'altro costruttore, e se {@code ceilingFall} c'è, quando il boss atterra a volte cadono
     * dei nemici dal soffitto.
     */
    public EnemyTurnRunner(
            Collection<BehaviorBinding> bindings,
            EnemyCatalog catalog,
            ActionRules actions,
            RangedRules ranged,
            JumpRules jumps,
            Optional<CeilingFall> ceilingFall) {
        for (BehaviorBinding binding : bindings) {
            if (behaviors.putIfAbsent(binding.key(), binding.behavior()) != null) {
                throw new IllegalArgumentException("Due strategie per " + binding.key().value());
            }
        }
        for (EnemyDefinition definition : catalog.all()) {
            if (!behaviors.containsKey(definition.behavior())) {
                throw new IllegalArgumentException(
                        "Nessuna strategia "
                                + definition.behavior().value()
                                + " per "
                                + definition.kind().value());
            }
        }
        this.catalog = catalog;
        this.ceilingFall = ceilingFall;
        this.actions = actions;
        this.ranged = ranged;
        this.jumps = jumps;
    }

    /**
     * Fa agire tutti i nemici vivi.
     *
     * @return gli eventi, nell'ordine in cui sono accaduti
     * @throws IllegalStateException se un nemico ha una strategia che non è registrata
     */
    public List<GameEvent> run(Run run) {
        return run(run, 1);
    }

    /**
     * Fa agire tutti i nemici vivi.
     *
     * @param enemyLevel il livello dei nemici nuovi che cadono dal soffitto: quello del piano
     * @return gli eventi, nell'ordine in cui sono accaduti
     * @throws IllegalStateException se un nemico ha una strategia che non è registrata
     */
    public List<GameEvent> run(Run run, int enemyLevel) {
        Floor floor = run.floor();
        List<GameEvent> events = new ArrayList<>();
        for (Enemy enemy : floor.enemies()) {
            if (!floor.player().isAlive()) {
                break;
            }
            if (!enemy.isAlive()) {
                continue;
            }
            EnemyBehavior behavior = behaviors.get(enemy.behavior());
            if (behavior == null) {
                throw new IllegalStateException(
                        "Nessuna strategia " + enemy.behavior().value() + " per " + enemy.id());
            }
            // Chi è più veloce, come un mercante arrabbiato, fa più azioni nello stesso turno.
            for (int action = 0;
                    action < enemy.definition().speed()
                            && enemy.isAlive()
                            && floor.player().isAlive();
                    action++) {
                EnemyIntent intent = behavior.decide(enemy, run.view(), run.gameplay());
                boolean retreating = false;
                switch (intent) {
                    case StepIntent(Direction direction) ->
                            events.addAll(
                                    actions.step(floor, enemy.id(), direction, run.gameplay())
                                            .events());
                    case RetreatIntent(Direction direction) -> {
                        events.addAll(
                                actions.step(floor, enemy.id(), direction, run.gameplay())
                                        .events());
                        retreating = true;
                    }
                    case RangedAttackIntent(Direction direction) ->
                            events.addAll(
                                    ranged.shoot(
                                            floor,
                                            enemy.id(),
                                            direction,
                                            enemy.range(),
                                            RangedRules.ARROW,
                                            enemy.stats().attack(),
                                            run.gameplay()));
                    case ChargeIntent() -> events.add(floor.chargeJump(enemy.id()));
                    case JumpIntent(Position anchor) ->
                            events.add(floor.announceJump(enemy.id(), anchor));
                    case LandIntent() -> {
                        List<GameEvent> landing = jumps.land(floor, enemy.id());
                        events.addAll(landing);
                        if (landing.stream().anyMatch(Landed.class::isInstance)) {
                            events.addAll(dropGoblins(run, enemy, enemyLevel));
                        }
                    }
                    case RestIntent() -> floor.rest(enemy.id());
                    case WaitIntent() -> {}
                }
                if (enemy.isAlive()) {
                    floor.recordRetreat(enemy.id(), retreating);
                }
            }
        }
        return List.copyOf(events);
    }

    /**
     * Quando il boss atterra, con il 30% di probabilità dal soffitto cadono dei goblin nella sua
     * stanza: fino a due, ma mai oltre quattro vivi insieme, su celle libere e non adiacenti al
     * player. Il tiro è quello del gioco, non quello della generazione.
     */
    private List<GameEvent> dropGoblins(Run run, Enemy slime, int enemyLevel) {
        Floor floor = run.floor();
        if (!ceilingFall.isPresent() || !slime.kind().equals(ceilingFall.get().boss())) {
            return List.of();
        }
        Kind kind = ceilingFall.get().reinforcement();
        Optional<EnemyDefinition> goblin =
                catalog.all().stream().filter(d -> d.kind().equals(kind)).findFirst();
        if (!goblin.isPresent() || !floor.player().isAlive()) {
            return List.of();
        }
        RandomSource random = run.gameplay();
        if (random.nextInt(100) >= FALL_CHANCE) {
            return List.of();
        }
        int alive = 0;
        for (Enemy other : floor.enemies()) {
            if (other.kind().equals(kind)
                    && floor.bossRoom().contains(floor.positionOf(other.id()))) {
                alive++;
            }
        }
        int count = Math.min(FALL_MAX, ROOM_GOBLIN_CAP - alive);
        Position playerAt = floor.positionOf(floor.playerId());
        List<Position> cells = new ArrayList<>();
        for (Position cell : floor.bossRoom().positions()) {
            if (floor.isFree(cell) && !cell.equals(playerAt) && !cell.isAdjacentTo(playerAt)) {
                cells.add(cell);
            }
        }
        List<GameEvent> fallen = new ArrayList<>();
        for (int i = 0; i < count && !cells.isEmpty(); i++) {
            Position at = cells.remove(random.nextInt(cells.size()));
            Enemy fresh = new Enemy(run.ids().next(), goblin.get(), enemyLevel);
            fallen.add(floor.spawnEnemy(fresh, at));
        }
        return fallen;
    }
}
