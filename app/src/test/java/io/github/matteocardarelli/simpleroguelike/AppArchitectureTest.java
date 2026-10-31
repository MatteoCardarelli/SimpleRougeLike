package io.github.matteocardarelli.simpleroguelike;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.matteocardarelli.simpleroguelike.ai.EnemyIntent;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.command.CommandHandler;
import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.state.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/** Le regole d'architettura di :app, verificate sul bytecode del codice di produzione. */
@AnalyzeClasses(
        packages = AppArchitectureTest.BASE,
        importOptions = AppArchitectureTest.OnlyThisModule.class)
class AppArchitectureTest {

    static final String BASE = "io.github.matteocardarelli.simpleroguelike";

    /**
     * Solo le classi di produzione di questo modulo, che i test ricevono in un jar sotto {@code
     * app/build}: non quelle degli altri moduli, né i test, né la classe R generata da Android, che
     * nei test ha campi statici non final.
     */
    static final class OnlyThisModule implements ImportOption {
        private static final Pattern R_CLASS = Pattern.compile(".*/R(\\$[^/]*)?\\.class");

        @Override
        public boolean includes(Location location) {
            return location.contains("/app/build/")
                    && !location.contains("UnitTest")
                    && !location.matches(R_CLASS);
        }
    }

    @ArchTest
    static final ArchRule noMutableStaticState =
            fields().that()
                    // Hilt genera queste classi con campi statici non final che servono solo a
                    // R8 per conservare le chiavi dei ViewModel: non sono stato del gioco.
                    .areDeclaredInClassesThat()
                    .haveSimpleNameNotEndingWith("_LazyMapKey")
                    .and()
                    .areStatic()
                    .should()
                    .beFinal()
                    .because(
                            "un campo statico modificabile vive quanto il processo: è così che il"
                                    + " vecchio GameController teneva in memoria un'Activity"
                                    + " distrutta, e una partita deve vivere solo nel suo"
                                    + " ViewModel");

    @ArchTest
    static final ArchRule onlyDiKnowsConcreteParts =
            noClasses()
                    .that()
                    .resideInAPackage(BASE + "..")
                    .and()
                    .resideOutsideOfPackage(BASE + ".di..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            BASE + ".squidlib..",
                            BASE + ".pathfinding..",
                            BASE + ".content..",
                            BASE + ".generation..",
                            BASE + ".ai..",
                            BASE + ".rules..",
                            BASE + ".run..")
                    .orShould()
                    .dependOnClassesThat()
                    .implement(CommandHandler.class)
                    .because(
                            "adapter, contenuti, regole e handler si collegano in un solo posto:"
                                    + " l'interfaccia parla solo con la sessione, così una libreria"
                                    + " o una regola si sostituisce senza toccarla");

    @ArchTest
    static final ArchRule feedbackUsesOnlyCommandSnapshotAndSession =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".feedback..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "org.jspecify.annotations..",
                            BASE + ".feedback..",
                            BASE + ".model..",
                            BASE + ".event..",
                            BASE + ".random..",
                            BASE + ".command..",
                            BASE + ".snapshot..",
                            BASE + ".session..")
                    .because(
                            "le note di collaudo si scrivono e si rigiocano anche sulla JVM: niente"
                                    + " Android, e della partita solo comandi, fotografia e"
                                    + " risultati");

    @ArchTest
    static final ArchRule uiGameUsesSessionCommandSnapshotAndTheUi =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".ui.game..")
                    .should()
                    .onlyDependOnClassesThat(
                            resideInAnyPackage(
                                            "java..",
                                            "android..",
                                            "androidx..",
                                            "com.google.android.material..",
                                            "dagger..",
                                            "javax.inject..",
                                            "org.jspecify.annotations..",
                                            BASE + ".ui..",
                                            BASE + ".session..",
                                            BASE + ".command..",
                                            BASE + ".snapshot..",
                                            BASE + ".feedback..",
                                            BASE + ".settings..",
                                            BASE + ".databinding..",
                                            BASE + ".model..",
                                            BASE + ".event..")
                                    .or(theRClass()))
                    .because(
                            "l'Activity e il ViewModel parlano con la sessione e con il resto"
                                    + " dell'interfaccia: adapter, regole e contenuti li collega"
                                    + " solo di");

    @ArchTest
    static final ArchRule uiInputUsesOnlyCommands =
            onlyDependOn(
                    "ui.input",
                    "un gesto diventa un comando: l'input non conosce la partita né Android",
                    "command");

    @ArchTest
    static final ArchRule uiRenderUsesOnlySnapshotPlaybackAndContent =
            onlyDependOn(
                    "ui.render",
                    "il disegno legge la fotografia della partita, già filtrata dalla nebbia di"
                            + " guerra, e niente altro",
                    "snapshot",
                    "ui.playback",
                    "ui.content");

    @ArchTest
    static final ArchRule uiPlaybackUsesSessionCommandSnapshotAndInput =
            onlyDependOn(
                    "ui.playback",
                    "la riproduzione mostra i risultati della sessione e blocca l'input, senza"
                            + " conoscere regole né contenuti",
                    "session",
                    "command",
                    "snapshot",
                    "ui.input");

    @ArchTest
    static final ArchRule uiInventoryUsesOnlySnapshotAndContent =
            onlyDependOn(
                    "ui.inventory",
                    "l'inventario mostra gli oggetti dello snapshot e passa le scelte all'Activity:"
                            + " non conosce il ViewModel",
                    "snapshot",
                    "ui.content",
                    "ui.hud");

    @ArchTest
    static final ArchRule uiHudUsesOnlySnapshot =
            onlyDependOn(
                    "ui.hud",
                    "le barre mostrano il player della fotografia della partita, e niente altro",
                    "snapshot");

    @ArchTest
    static final ArchRule uiSettingsUsesOnlySettings =
            onlyDependOn(
                    "ui.settings",
                    "la finestra delle impostazioni mostra e raccoglie le scelte: chi le salva è il"
                            + " ViewModel, attraverso SettingsHost e l'Activity",
                    "settings");

    @ArchTest
    static final ArchRule settingsUsesOnlyJavaAndPreferences =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".settings..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "android.content..",
                            "org.jspecify.annotations..",
                            BASE + ".settings..")
                    .because(
                            "le impostazioni sono dati del giocatore: niente partita né interfaccia,"
                                    + " e di Android solo SharedPreferences, nel suo adapter");

    @ArchTest
    static final ArchRule uiLogUsesOnlyContent =
            onlyDependOn(
                    "ui.log",
                    "il testo di un evento dipende solo dall'evento e dal nome dei contenuti",
                    "ui.content");

    @ArchTest
    static final ArchRule uiContentUsesOnlyTheKernel =
            onlyDependOn(
                    "ui.content",
                    "nome e aspetto dipendono solo dalla chiave del contenuto: i valori li sceglie"
                            + " di, insieme ai cataloghi");

    @ArchTest
    static final ArchRule uiFeedbackUsesOnlyAndroidAndTheKernel =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".ui.feedback..")
                    .should()
                    .onlyDependOnClassesThat(
                            resideInAnyPackage(
                                            "java..",
                                            "android..",
                                            "androidx..",
                                            "com.google.android.material..",
                                            "org.jspecify.annotations..",
                                            BASE + ".ui.feedback..",
                                            BASE + ".model..",
                                            BASE + ".event..")
                                    .or(theRClass()))
                    .because(
                            "la finestra della nota raccoglie solo un testo: chi lo salva con la"
                                    + " partita è l'Activity, attraverso FeedbackHost");

    @ArchTest
    static final ArchRule noInstanceofOnHierarchies =
            classes()
                    .should(
                            notCheckInstanceofOf(
                                    GameEvent.class,
                                    Command.class,
                                    GameState.class,
                                    Actor.class,
                                    EnemyIntent.class))
                    .because(
                            "comandi ed eventi si gestiscono con un registro per classe: un"
                                    + " instanceof andrebbe aggiornato a ogni tipo nuovo, e il"
                                    + " compilatore non lo segnalerebbe");

    @ArchTest
    static final ArchRule noConsoleOutput =
            noClasses()
                    .should()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because(
                            "sul telefono la console non la legge nessuno: quello che succede nel"
                                    + " turno lo raccontano gli eventi");

    @ArchTest
    static final ArchRule everyNewPackageIsNullMarked =
            classes()
                    .that()
                    .resideInAPackage(BASE + "..")
                    // Le classi che genera Android: la configurazione della build e View Binding.
                    .and()
                    .resideOutsideOfPackage(BASE + ".databinding..")
                    .and()
                    .doNotHaveSimpleName("BuildConfig")
                    .should(resideInANullMarkedPackage())
                    .because("con @NullMarked un tipo senza annotazione non è mai null");

    /**
     * Un package di {@code ui} che usa solo Android, il kernel dell'interfaccia ({@code model} ed
     * {@code event}), i package indicati e le risorse.
     */
    private static ArchRule onlyDependOn(String packageName, String reason, String... allowed) {
        List<String> packages =
                new ArrayList<>(
                        List.of(
                                "java..",
                                "android..",
                                "androidx..",
                                "com.google.android.material..",
                                "org.jspecify.annotations..",
                                BASE + "." + packageName + "..",
                                BASE + ".model..",
                                BASE + ".event..",
                                BASE + ".databinding.."));
        for (String other : allowed) {
            packages.add(BASE + "." + other + "..");
        }
        return classes()
                .that()
                .resideInAPackage(BASE + "." + packageName + "..")
                .should()
                .onlyDependOnClassesThat(
                        resideInAnyPackage(packages.toArray(String[]::new)).or(theRClass()))
                .because(reason);
    }

    /** Le risorse generate da Android, che stanno nel package dell'app. */
    private static DescribedPredicate<JavaClass> theRClass() {
        return DescribedPredicate.describe(
                "the generated R class", type -> type.getName().matches(".*\\.R(\\$.*)?"));
    }

    private static ArchCondition<JavaClass> notCheckInstanceofOf(Class<?>... hierarchies) {
        return new ArchCondition<>("not check instanceof on closed or open hierarchies") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
                    for (InstanceofCheck check : codeUnit.getInstanceofChecks()) {
                        for (Class<?> hierarchy : hierarchies) {
                            if (check.getRawType().isAssignableTo(hierarchy)) {
                                events.add(
                                        SimpleConditionEvent.violated(
                                                check,
                                                "instanceof "
                                                        + check.getRawType().getSimpleName()
                                                        + " in "
                                                        + check.getSourceCodeLocation()));
                            }
                        }
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> resideInANullMarkedPackage() {
        return new ArchCondition<>("reside in a package annotated with @NullMarked") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!javaClass.getPackage().isAnnotatedWith(NullMarked.class)) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getPackageName()
                                            + " non ha un package-info con @NullMarked"));
                }
            }
        };
    }
}
