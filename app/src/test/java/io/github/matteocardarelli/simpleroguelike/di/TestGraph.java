package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.ai.BehaviorBinding;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.generation.FloorSource;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.session.GameSession;
import io.github.matteocardarelli.simpleroguelike.session.TurnHook;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * La stessa sessione che costruisce Hilt, dai metodi statici di {@link ContentModule} e {@link
 * GameModule}: i test sulla JVM giocano con gli adapter, le regole e i contenuti veri. Un metodo
 * {@code @IntoSet} nuovo nei moduli va aggiunto anche qui; i test di coerenza lo ricordano.
 */
public final class TestGraph {

    private TestGraph() {}

    /** Una sessione con i piani generati, come nell'app. */
    public static GameSession session(GameConfig config) {
        return session(
                config,
                GameModule.floorSource(
                        GameModule.terrainGenerator(),
                        ContentModule.itemCatalog(),
                        ContentModule.enemyCatalog(),
                        ContentModule.playerDefinition()));
    }

    /** Una sessione con i piani di {@code floors}, per esempio un piano disegnato. */
    public static GameSession session(GameConfig config, FloorSource floors) {
        ActionRules actions = actionRules();
        return GameModule.gameSession(
                floors,
                GameModule.commandDispatcher(commandHandlers(actions)),
                GameModule.enemyTurnRunner(behaviors(), ContentModule.enemyCatalog(), actions),
                GameModule.fieldOfView(),
                Set.<TurnHook>of(),
                config);
    }

    public static ActionRules actionRules() {
        return ContentModule.actionRules(
                ContentModule.combatRules(
                        ContentModule.damageRules(ContentModule.levelingRules())));
    }

    /** Gli handler che Hilt raccoglie con {@code @IntoSet}. */
    public static Set<CommandHandler<?>> commandHandlers(ActionRules actions) {
        Set<CommandHandler<?>> handlers = new LinkedHashSet<>();
        handlers.add(GameModule.moveHandler(actions));
        handlers.add(GameModule.useItemHandler());
        return handlers;
    }

    /** Le strategie dei nemici che Hilt raccoglie con {@code @IntoSet}. */
    public static Set<BehaviorBinding> behaviors() {
        Set<BehaviorBinding> behaviors = new LinkedHashSet<>();
        behaviors.add(GameModule.chaseOrWander(GameModule.pathfinder()));
        return behaviors;
    }
}
