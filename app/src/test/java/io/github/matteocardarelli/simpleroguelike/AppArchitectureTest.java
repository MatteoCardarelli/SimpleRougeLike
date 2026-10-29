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
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * Le regole d'architettura di :app, verificate sul bytecode del codice di produzione. Il motore di
 * prima del refactoring non è più usato dall'app e ne è escluso finché non viene rimosso.
 */
@AnalyzeClasses(
        packages = {AppArchitectureTest.BASE, AppArchitectureTest.OLD},
        importOptions = AppArchitectureTest.OnlyThisModule.class)
class AppArchitectureTest {

    static final String BASE = "io.github.matteocardarelli.simpleroguelike";
    static final String OLD = "com.github.matteoCarda.simplerougelike";

    /** Il motore di prima del refactoring: classi che l'app non usa più. */
    private static final String[] OLD_ENGINE = {
        OLD + ".controller..", OLD + ".model..", OLD + ".service..", OLD + ".util.."
    };

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
                    .areDeclaredInClassesThat()
                    .resideOutsideOfPackages(OLD_ENGINE)
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
                    .that()
                    .resideOutsideOfPackages(OLD_ENGINE)
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
                    .that()
                    .resideOutsideOfPackages(OLD_ENGINE)
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
                    .should(resideInANullMarkedPackage())
                    .because("con @NullMarked un tipo senza annotazione non è mai null");

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
