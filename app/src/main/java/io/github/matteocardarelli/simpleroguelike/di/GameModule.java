package io.github.matteocardarelli.simpleroguelike.di;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import io.github.matteocardarelli.simpleroguelike.ai.AggroBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.ai.ChaseBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.KiteBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.MerchantBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.SlimeBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.SurroundBehavior;
import io.github.matteocardarelli.simpleroguelike.ai.WanderBehavior;
import io.github.matteocardarelli.simpleroguelike.command.CommandDispatcher;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.command.EquipHandler;
import io.github.matteocardarelli.simpleroguelike.command.MoveHandler;
import io.github.matteocardarelli.simpleroguelike.command.TakeWareHandler;
import io.github.matteocardarelli.simpleroguelike.command.ThrowHandler;
import io.github.matteocardarelli.simpleroguelike.command.UnequipHandler;
import io.github.matteocardarelli.simpleroguelike.command.UseItemHandler;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.generation.FloorFactory;
import io.github.matteocardarelli.simpleroguelike.generation.FloorPopulator;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSource;
import io.github.matteocardarelli.simpleroguelike.pathfinding.BreadthFirstPathfinder;
import io.github.matteocardarelli.simpleroguelike.port.FieldOfView;
import io.github.matteocardarelli.simpleroguelike.port.Pathfinder;
import io.github.matteocardarelli.simpleroguelike.port.TerrainGenerator;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.JumpRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.session.EnemyTurnRunner;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.LootHook;
import io.github.matteocardarelli.simpleroguelike.session.SaveCodec;
import io.github.matteocardarelli.simpleroguelike.session.TurnHook;
import io.github.matteocardarelli.simpleroguelike.squidlib.SquidFieldOfView;
import io.github.matteocardarelli.simpleroguelike.squidlib.SquidTerrainGenerator;
import java.util.Set;

/**
 * Adapter, strategie dei nemici, handler, hook e sessione: servizi senza stato di partita, che
 * vivono quanto il ViewModel. Lo stato di una partita sta nella sessione, che lo ricrea a ogni
 * partita nuova. I metodi sono statici: i test costruiscono la stessa sessione chiamandoli.
 *
 * <p>Un'estensione si aggiunge con un metodo nuovo: una strategia con {@code @IntoSet} che
 * restituisce un {@link BehaviorBinding}, un comando con il suo handler, una regola di fine turno.
 */
@Module
@InstallIn(ViewModelComponent.class)
public abstract class GameModule {

    private GameModule() {}

    @Provides
    static TerrainGenerator terrainGenerator() {
        return new SquidTerrainGenerator();
    }

    @Provides
    static Pathfinder pathfinder() {
        return new BreadthFirstPathfinder();
    }

    @Provides
    static FieldOfView fieldOfView() {
        return new SquidFieldOfView();
    }

    @Provides
    static FloorSource floorSource(
            TerrainGenerator terrain,
            ItemCatalog items,
            EnemyCatalog enemies,
            PlayerDefinition player) {
        return new FloorFactory(terrain, new FloorPopulator(items, enemies, player));
    }

    /** Il goblin insegue il player quando è abbastanza vicino, altrimenti vaga. */
    @Provides
    @IntoSet
    static BehaviorBinding chaseOrWander(Pathfinder pathfinder) {
        return new BehaviorBinding(
                StandardEnemyCatalog.CHASE_OR_WANDER,
                new AggroBehavior(new ChaseBehavior(pathfinder), new WanderBehavior()));
    }

    /**
     * L'arciere tira quando ha il player a tiro, scappa se è vicino, e altrimenti lo insegue; se è
     * lontano, vaga.
     */
    @Provides
    @IntoSet
    static BehaviorBinding keepDistance(Pathfinder pathfinder) {
        return new BehaviorBinding(
                StandardEnemyCatalog.KEEP_DISTANCE,
                new AggroBehavior(
                        new KiteBehavior(new ChaseBehavior(pathfinder)), new WanderBehavior()));
    }

    /** I lupi accerchiano il player con il resto del branco; se è lontano, vagano. */
    @Provides
    @IntoSet
    static BehaviorBinding pack(Pathfinder pathfinder) {
        return new BehaviorBinding(
                StandardEnemyCatalog.PACK,
                new AggroBehavior(new SurroundBehavior(pathfinder), new WanderBehavior()));
    }

    /** Il mercante sta al banco; derubato attacca da vicino e da lontano e insegue. */
    @Provides
    @IntoSet
    static BehaviorBinding merchant(Pathfinder pathfinder) {
        return new BehaviorBinding(
                StandardEnemyCatalog.MERCHANT_BEHAVIOR,
                new MerchantBehavior(new ChaseBehavior(pathfinder)));
    }

    @Provides
    @IntoSet
    static CommandHandler<?> takeWareHandler() {
        return new TakeWareHandler();
    }

    /** Lo slime gigante salta sul player e lo schiaccia, poi riposa un turno. */
    @Provides
    @IntoSet
    static BehaviorBinding jumpAndCrush() {
        return new BehaviorBinding(StandardEnemyCatalog.JUMP_AND_CRUSH, new SlimeBehavior());
    }

    @Provides
    @IntoSet
    static CommandHandler<?> moveHandler(ActionRules actions) {
        return new MoveHandler(actions);
    }

    @Provides
    @IntoSet
    static CommandHandler<?> useItemHandler() {
        return new UseItemHandler();
    }

    @Provides
    @IntoSet
    static CommandHandler<?> throwHandler(RangedRules ranged) {
        return new ThrowHandler(ranged);
    }

    @Provides
    @IntoSet
    static CommandHandler<?> equipHandler() {
        return new EquipHandler();
    }

    @Provides
    @IntoSet
    static CommandHandler<?> unequipHandler() {
        return new UnequipHandler();
    }

    /** Le regole di fine turno; l'insieme vuoto è ammesso. */
    @Multibinds
    abstract Set<TurnHook> hooks();

    /** Il bottino dei nemici morti. */
    @Provides
    @IntoSet
    static TurnHook lootHook(ItemCatalog items) {
        return new LootHook(items);
    }

    @Provides
    static EnemyTurnRunner enemyTurnRunner(
            Set<BehaviorBinding> behaviors,
            EnemyCatalog enemies,
            ActionRules actions,
            RangedRules ranged,
            JumpRules jumps) {
        return new EnemyTurnRunner(behaviors, enemies, actions, ranged, jumps);
    }

    @Provides
    static CommandDispatcher commandDispatcher(Set<CommandHandler<?>> handlers) {
        return new CommandDispatcher(handlers);
    }

    /** Il formato del salvataggio, che ritrova oggetti e nemici nei cataloghi. */
    @Provides
    static SaveCodec saveCodec(ItemCatalog items, EnemyCatalog enemies) {
        return new SaveCodec(items, enemies);
    }

    @Provides
    @ViewModelScoped
    static GameSession gameSession(
            FloorSource floors,
            CommandDispatcher commands,
            EnemyTurnRunner enemies,
            FieldOfView fov,
            Set<TurnHook> hooks,
            GameConfig config) {
        return new GameSession(
                floors, commands, enemies, fov, hooks, config.floorSpecs(), config.visionRadius());
    }
}
