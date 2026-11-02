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
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.PlayerDefinition;
import io.github.matteocardarelli.simpleroguelike.feedback.BuildInfo;
import io.github.matteocardarelli.simpleroguelike.feedback.FeedbackStore;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Stats;
import io.github.matteocardarelli.simpleroguelike.rules.ActionRules;
import io.github.matteocardarelli.simpleroguelike.rules.CombatRules;
import io.github.matteocardarelli.simpleroguelike.rules.DamageRules;
import io.github.matteocardarelli.simpleroguelike.rules.LevelingRules;
import io.github.matteocardarelli.simpleroguelike.rules.RangedRules;
import io.github.matteocardarelli.simpleroguelike.settings.PreferencesSettingsStore;
import io.github.matteocardarelli.simpleroguelike.settings.SettingsStore;
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

    /**
     * Ogni livello dà 20 di salute, 2 di attacco, 1 di difesa, 2 di precisione e 1 di schivata, e
     * la soglia successiva cresce del 50%.
     */
    @Provides
    @Singleton
    static LevelingRules levelingRules() {
        return new LevelingRules(new Stats(20, 2, 1, 2, 1), 150);
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
    static RangedRules rangedRules(DamageRules damage) {
        return new RangedRules(damage);
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

    /**
     * Nome, descrizione e colore del player e di ogni contenuto dei cataloghi: un contenuto nuovo
     * aggiunge qui la sua riga.
     */
    @Provides
    @Singleton
    static ContentPresentation contentPresentation() {
        Map<Kind, ContentPresentation.Style> styles = new LinkedHashMap<>();
        styles.put(
                Player.KIND,
                new ContentPresentation.Style(
                        R.string.kind_player_name,
                        R.string.kind_player_description,
                        R.color.kind_player));
        styles.put(
                StandardItemCatalog.POTION,
                new ContentPresentation.Style(
                        R.string.kind_potion_name,
                        R.string.kind_potion_description,
                        R.color.kind_potion));
        styles.put(
                StandardItemCatalog.GREATER_POTION,
                new ContentPresentation.Style(
                        R.string.kind_greater_potion_name,
                        R.string.kind_greater_potion_description,
                        R.color.kind_greater_potion));
        styles.put(
                StandardItemCatalog.SWORD,
                new ContentPresentation.Style(
                        R.string.kind_sword_name,
                        R.string.kind_sword_description,
                        R.color.kind_sword));
        styles.put(
                StandardItemCatalog.AXE,
                new ContentPresentation.Style(
                        R.string.kind_axe_name, R.string.kind_axe_description, R.color.kind_axe));
        styles.put(
                StandardItemCatalog.LEATHER_ARMOR,
                new ContentPresentation.Style(
                        R.string.kind_leather_armor_name,
                        R.string.kind_leather_armor_description,
                        R.color.kind_leather_armor));
        styles.put(
                StandardItemCatalog.CHAIN_MAIL,
                new ContentPresentation.Style(
                        R.string.kind_chain_mail_name,
                        R.string.kind_chain_mail_description,
                        R.color.kind_chain_mail));
        styles.put(
                StandardItemCatalog.RING_OF_ACCURACY,
                new ContentPresentation.Style(
                        R.string.kind_ring_of_accuracy_name,
                        R.string.kind_ring_of_accuracy_description,
                        R.color.kind_ring_of_accuracy));
        styles.put(
                StandardItemCatalog.RING_OF_VIGOR,
                new ContentPresentation.Style(
                        R.string.kind_ring_of_vigor_name,
                        R.string.kind_ring_of_vigor_description,
                        R.color.kind_ring_of_vigor));
        styles.put(
                StandardEnemyCatalog.GOBLIN,
                new ContentPresentation.Style(
                        R.string.kind_goblin_name,
                        R.string.kind_goblin_description,
                        R.color.kind_goblin));
        styles.put(
                StandardItemCatalog.THROWING_KNIFE,
                new ContentPresentation.Style(
                        R.string.kind_throwing_knife_name,
                        R.string.kind_throwing_knife_description,
                        R.color.kind_throwing_knife));
        styles.put(
                StandardEnemyCatalog.ARCHER,
                new ContentPresentation.Style(
                        R.string.kind_archer_name,
                        R.string.kind_archer_description,
                        R.color.kind_archer));
        return new ContentPresentation(styles);
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

    /** Le impostazioni del giocatore, che restano dopo la chiusura dell'app. */
    @Provides
    @Singleton
    static SettingsStore settingsStore(@ApplicationContext Context context) {
        return new PreferencesSettingsStore(context);
    }
}
