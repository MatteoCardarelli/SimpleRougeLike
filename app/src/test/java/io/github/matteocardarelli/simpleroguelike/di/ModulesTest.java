package io.github.matteocardarelli.simpleroguelike.di;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemDefinition;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.game.SeedSource;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * La coerenza tra i moduli e i contenuti, senza riflessione sulle annotazioni: un contenuto o un
 * comando nuovo senza la sua riga in un modulo fa fallire uno di questi test, invece che l'app.
 */
class ModulesTest {

    @Test
    void everyKindOfTheCatalogs_hasANameToShow() {
        ContentPresentation content = ContentModule.contentPresentation();
        List<Kind> kinds = new ArrayList<>(List.of(Player.KIND));
        for (ItemDefinition item : ContentModule.itemCatalog().all()) {
            kinds.add(item.kind());
        }
        for (EnemyDefinition enemy : ContentModule.enemyCatalog().all()) {
            kinds.add(enemy.kind());
        }

        assertThat(content.kinds()).containsAll(kinds);
    }

    @Test
    void everyCommand_hasAHandler() {
        List<String> commands = new ArrayList<>();
        for (JavaClass type :
                new ClassFileImporter().importPackages(Command.class.getPackageName())) {
            if (type.isAssignableTo(Command.class) && !type.isInterface()) {
                commands.add(type.getName());
            }
        }
        List<String> handled = new ArrayList<>();
        for (CommandHandler<?> handler : TestGraph.commandHandlers(TestGraph.actionRules())) {
            handled.add(handler.commandType().getName());
        }

        assertThat(commands).isNotEmpty();
        assertThat(handled).containsExactlyInAnyOrderElementsOf(commands);
    }

    /** Il costruttore della sessione controlla che ogni nemico del catalogo abbia una strategia. */
    @Test
    void session_buildsWithTheBehaviorsOfTheModule() {
        assertThat(TestGraph.session(GameConfig.standard()).newGame(1).snapshot().turn())
                .isEqualTo(1);
    }

    @Test
    void seedSource_withAFixedSeed_givesAlwaysThatSeed() {
        GameConfig fixed =
                new GameConfig(
                        GameConfig.standard().floorSpec(),
                        GameConfig.standard().visionRadius(),
                        OptionalLong.of(42));

        SeedSource seeds = ContentModule.seedSource(fixed);

        assertThat(seeds.nextSeed()).isEqualTo(42);
        assertThat(seeds.nextSeed()).isEqualTo(42);
    }
}
