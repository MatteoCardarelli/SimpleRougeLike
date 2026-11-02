package io.github.matteocardarelli.simpleroguelike.session;

import io.github.matteocardarelli.simpleroguelike.ai.AggroBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.ChaseBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.KiteBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.WanderBehavior;
import io.github.matteocardarelli.simpleroguelike.command.CommandDispatcher;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.command.MoveHandler;
import io.github.matteocardarelli.simpleroguelike.command.UseItemHandler;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSource;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.port.OpenFieldOfView;
import io.github.matteocardarelli.simpleroguelike.port.StraightLinePathfinder;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import java.util.ArrayList;
import java.util.List;

/** Sessioni costruite con le regole vere e i fake del dominio, senza mock. */
final class Sessions {

    static final LevelingRules LEVELING = new LevelingRules(new Stats(20, 2), 150);
    static final DamageRules DAMAGE = new DamageRules(LEVELING);
    static final ActionRules ACTIONS = new ActionRules(new CombatRules(DAMAGE));
    static final RangedRules RANGED = new RangedRules(DAMAGE);
    static final FloorSpec SPEC = new FloorSpec(15, 10, 3, 2, 3, 1);

    private Sessions() {}

    static GameSession session(FloorSource floors) {
        return session(floors, 20, List.of(), List.of());
    }

    static GameSession session(
            FloorSource floors,
            int visionRadius,
            List<TurnHook> hooks,
            List<CommandHandler<?>> extraHandlers) {
        List<CommandHandler<?>> handlers = new ArrayList<>();
        handlers.add(new MoveHandler(ACTIONS));
        handlers.add(new UseItemHandler());
        handlers.addAll(extraHandlers);
        return new GameSession(
                floors,
                new CommandDispatcher(handlers),
                enemyTurnRunner(),
                new OpenFieldOfView(),
                hooks,
                SPEC,
                visionRadius);
    }

    static EnemyTurnRunner enemyTurnRunner() {
        BehaviorBinding chaseOrWander =
                new BehaviorBinding(
                        StandardEnemyCatalog.CHASE_OR_WANDER,
                        new AggroBehavior(
                                new ChaseBehavior(new StraightLinePathfinder()),
                                new WanderBehavior()));
        BehaviorBinding keepDistance =
                new BehaviorBinding(
                        StandardEnemyCatalog.KEEP_DISTANCE,
                        new AggroBehavior(
                                new KiteBehavior(new ChaseBehavior(new StraightLinePathfinder())),
                                new WanderBehavior()));
        return new EnemyTurnRunner(
                List.of(chaseOrWander, keepDistance), new StandardEnemyCatalog(), ACTIONS, RANGED);
    }
}
