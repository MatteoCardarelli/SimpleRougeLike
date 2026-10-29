package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.ai.AggroBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.ChaseBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.WanderBehavior;
import io.github.matteocardarelli.simpleroguelike.command.CommandDispatcher;
import io.github.matteocardarelli.simpleroguelike.command.MoveHandler;
import io.github.matteocardarelli.simpleroguelike.command.UseItemHandler;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorPopulator;
import io.github.matteocardarelli.simpleroguelike.pathfinding.BreadthFirstPathfinder;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.session.EnemyTurnRunner;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.squidlib.SquidFieldOfView;
import io.github.matteocardarelli.simpleroguelike.squidlib.SquidTerrainGenerator;
import java.util.List;

/**
 * Costruisce a mano una {@link GameSession} con gli adapter, le regole e i contenuti veri. Quando
 * arriverà Hilt, lo stesso collegamento lo faranno i suoi moduli, un metodo per oggetto.
 */
public final class GameSessionFactory {

    private GameSessionFactory() {}

    /** Una sessione nuova, ancora senza partita. */
    public static GameSession create(GameConfig config) {
        // Ogni livello dà 20 di salute e 2 di attacco, e la soglia successiva cresce del 50%,
        // come nel gioco di prima.
        LevelingRules leveling = new LevelingRules(20, 2, 150);
        ActionRules actions = new ActionRules(new CombatRules(new DamageRules(leveling)));
        StandardEnemyCatalog enemyCatalog = new StandardEnemyCatalog();

        FloorFactory floors =
                new FloorFactory(
                        new SquidTerrainGenerator(),
                        new FloorPopulator(
                                new StandardItemCatalog(),
                                enemyCatalog,
                                StandardPlayer.DEFINITION));
        BehaviorBinding chaseOrWander =
                new BehaviorBinding(
                        StandardEnemyCatalog.CHASE_OR_WANDER,
                        new AggroBehavior(
                                new ChaseBehavior(new BreadthFirstPathfinder()),
                                new WanderBehavior()));
        CommandDispatcher commands =
                new CommandDispatcher(List.of(new MoveHandler(actions), new UseItemHandler()));

        return new GameSession(
                floors,
                commands,
                new EnemyTurnRunner(List.of(chaseOrWander), enemyCatalog, actions),
                new SquidFieldOfView(),
                List.of(),
                config.floorSpec(),
                config.visionRadius());
    }
}
