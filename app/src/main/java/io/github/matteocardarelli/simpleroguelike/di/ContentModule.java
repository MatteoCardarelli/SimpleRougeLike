package io.github.matteocardarelli.simpleroguelike.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.github.matteocardarelli.simpleroguelike.BuildConfig;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.content.StandardEnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardItemCatalog;
import io.github.matteocardarelli.simpleroguelike.content.StandardPlayer;
import io.github.matteocardarelli.simpleroguelike.dungeon.EnemyCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.ItemCatalog;
import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.feedback.BuildInfo;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.game.SeedSource;
import java.io.File;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.inject.Singleton;

/**
 * Configurazione, regole, contenuti e loro presentazione: valori e servizi senza stato, che vivono
 * quanto l'app. Le regole stanno qui e non con la sessione perché un catalogo può averne bisogno,
 * per esempio un effetto di danno. I metodi sono statici: i test li chiamano direttamente.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class ContentModule {

    private ContentModule() {}

    @Provides
    static GameConfig gameConfig() {
        return GameConfig.standard();
    }

    /** Ogni livello dà 20 di salute e 2 di attacco, e la soglia successiva cresce del 50%. */
    @Provides
    @Singleton
    static LevelingRules levelingRules() {
        return new LevelingRules(20, 2, 150);
    }

    @Provides
    @Singleton
    static DamageRules damageRules(LevelingRules leveling) {
        return new DamageRules(leveling);
    }

    @Provides
    @Singleton
    static CombatRules combatRules(DamageRules damage) {
        return new CombatRules(damage);
    }

    @Provides
    @Singleton
    static ActionRules actionRules(CombatRules combat) {
        return new ActionRules(combat);
    }

    @Provides
    @Singleton
    static ItemCatalog itemCatalog() {
        return new StandardItemCatalog();
    }

    @Provides
    @Singleton
    static EnemyCatalog enemyCatalog() {
        return new StandardEnemyCatalog();
    }

    @Provides
    static PlayerDefinition playerDefinition() {
        return StandardPlayer.DEFINITION;
    }

    /** Il nome di ogni contenuto dei cataloghi: un contenuto nuovo aggiunge qui la sua riga. */
    @Provides
    @Singleton
    static ContentPresentation contentPresentation() {
        Map<Kind, Integer> names = new LinkedHashMap<>();
        names.put(StandardItemCatalog.POTION, R.string.kind_potion_name);
        names.put(StandardEnemyCatalog.GOBLIN, R.string.kind_goblin_name);
        return new ContentPresentation(names);
    }

    /** Il seed lo sceglie l'app: quello fisso della configurazione, se c'è, altrimenti a caso. */
    @Provides
    static SeedSource seedSource(GameConfig config) {
        if (config.fixedSeed().isPresent()) {
            long seed = config.fixedSeed().getAsLong();
            return () -> seed;
        }
        Random random = new Random();
        return random::nextLong;
    }

    @Provides
    static BuildInfo buildInfo() {
        return new BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.GIT_COMMIT);
    }

    @Provides
    static Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Le note di collaudo, nella memoria privata dell'app. */
    @Provides
    @Singleton
    static FeedbackStore feedbackStore(@ApplicationContext Context context) {
        return new FeedbackStore(new File(context.getFilesDir(), "feedback"));
    }
}
