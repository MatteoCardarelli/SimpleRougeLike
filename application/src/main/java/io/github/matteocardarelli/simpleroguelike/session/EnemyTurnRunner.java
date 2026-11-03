package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyIntent;
import io.github.matteocardarelli.simpleroguelike.ai.JumpIntent;
import io.github.matteocardarelli.simpleroguelike.ai.LandIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RangedAttackIntent;
import io.github.matteocardarelli.simpleroguelike.ai.RestIntent;
import io.github.matteocardarelli.simpleroguelike.ai.StepIntent;
import io.github.matteocardarelli.simpleroguelike.ai.WaitIntent;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.BehaviorKey;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.JumpRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * Il turno dei nemici: ognuno, in ordine di id, decide un'intenzione con la sua strategia e la
 * esegue con la stessa regola del player. Si ferma appena il player muore.
 *
 * <p>È l'unico punto che interpreta le intenzioni, con uno {@code switch} esaustivo: un'intenzione
 * nuova non compila finché non viene gestita qui.
 */
public final class EnemyTurnRunner {

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
                switch (intent) {
                    case StepIntent(Direction direction) ->
                            events.addAll(
                                    actions.step(floor, enemy.id(), direction, run.gameplay())
                                            .events());
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
                    case JumpIntent(Position anchor) ->
                            events.add(floor.announceJump(enemy.id(), anchor));
                    case LandIntent() -> events.addAll(jumps.land(floor, enemy.id()));
                    case RestIntent() -> floor.rest(enemy.id());
                    case WaitIntent() -> {}
                }
            }
        }
        return List.copyOf(events);
    }
}
