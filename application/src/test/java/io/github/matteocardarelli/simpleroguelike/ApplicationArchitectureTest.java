package io.github.matteocardarelli.simpleroguelike;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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
import io.github.matteocardarelli.simpleroguelike.ai.StepIntent;
import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.EnemyTurnRunner;
import io.github.matteocardarelli.simpleroguelike.state.GameState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.jspecify.annotations.NullMarked;

/** Le regole d'architettura di :application, verificate sul bytecode del codice di produzione. */
@AnalyzeClasses(
        packages = "io.github.matteocardarelli.simpleroguelike",
        importOptions = ApplicationArchitectureTest.OnlyThisModule.class)
class ApplicationArchitectureTest {

    private static final String BASE = "io.github.matteocardarelli.simpleroguelike";
    private static final String[] KERNEL = {"model", "event", "random"};

    /** Solo le classi di produzione di questo modulo, non quelle di :domain né dei test. */
    static final class OnlyThisModule implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("application")
                    && new ImportOption.DoNotIncludeTests().includes(location);
        }
    }

    @ArchTest
    static final ArchRule noCycles =
            slices().matching(BASE + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("un ciclo tra package rende impossibile capire un pezzo da solo");

    @ArchTest
    static final ArchRule snapshotUsesOnlyModel =
            onlyDependOn(
                    "snapshot",
                    "lo snapshot va all'interfaccia: con solo valori di base non le dà accesso al"
                            + " piano, agli attori o alle regole",
                    false,
                    "model");

    @ArchTest
    static final ArchRule commandUsesOnlyRunRulesAndDungeon =
            onlyDependOn(
                    "command",
                    "un handler agisce sulla partita con le regole del dominio, senza conoscere"
                            + " fasi, sessione o snapshot",
                    true,
                    "run",
                    "rules",
                    "dungeon");

    @ArchTest
    static final ArchRule stateUsesOnlyCommandSnapshotAndDungeon =
            onlyDependOn(
                    "state",
                    "una fase decide solo quali comandi accetta e quale fase viene dopo",
                    true,
                    "command",
                    "snapshot",
                    "dungeon");

    @ArchTest
    static final ArchRule sessionUsesTheWholeDomainButNoAdapter =
            onlyDependOn(
                    "session",
                    "la sessione mette insieme il turno usando le porte, mai gli adapter: le"
                            + " librerie restano sostituibili",
                    true,
                    "command",
                    "state",
                    "snapshot",
                    "run",
                    "generation",
                    "ai",
                    "rules",
                    "port",
                    "dungeon");

    @ArchTest
    static final ArchRule noFrameworks =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "android..", "androidx..", "squidpony..", "dagger..", "javax.inject..")
                    .because(
                            "l'applicazione è Java puro: i turni si provano sulla JVM, e le"
                                    + " classi ricevono tutto dal costruttore senza framework");

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
                            "comandi ed eventi si gestiscono con un registro per classe, fasi e"
                                    + " attori con i loro metodi, le intenzioni con uno switch"
                                    + " esaustivo: un instanceof andrebbe aggiornato a ogni tipo"
                                    + " nuovo, e il compilatore non lo segnalerebbe");

    @ArchTest
    static final ArchRule intentsInterpretedOnlyByEnemyTurnRunner =
            noClasses()
                    .that()
                    .doNotBelongToAnyOf(EnemyTurnRunner.class)
                    .should()
                    .callMethod(StepIntent.class, "direction")
                    .because(
                            "un'intenzione si esegue in un solo punto: interpretarla altrove"
                                    + " duplicherebbe la regola che EnemyTurnRunner applica");

    @ArchTest
    static final ArchRule turnLifecycleOnlyFromSession =
            noClasses()
                    .that()
                    .resideOutsideOfPackage(BASE + ".session..")
                    .should()
                    .callMethod(Floor.class, "updateVisibility", CellSet.class)
                    .orShould()
                    .callMethod(Floor.class, "removeDead")
                    .because(
                            "aggiornare le celle visibili e togliere i morti sono passi della fine"
                                    + " del turno, nell'ordine che solo la sessione conosce");

    @ArchTest
    static final ArchRule damageOnlyThroughRules =
            noClasses()
                    .should()
                    .callMethod(Floor.class, "damage", EntityId.class, int.class)
                    .because(
                            "fuori dal dominio il danno passa da DamageRules, che assegna anche"
                                    + " l'esperienza");

    @ArchTest
    static final ArchRule noHiddenRandomness =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .belongToAnyOf(Random.class)
                    .orShould()
                    .callMethod(Math.class, "random")
                    .orShould()
                    .callMethod(System.class, "nanoTime")
                    .orShould()
                    .callMethod(System.class, "currentTimeMillis")
                    .because(
                            "lo stesso seed e gli stessi comandi devono dare la stessa partita: il"
                                    + " caso arriva solo dal flusso della partita");

    @ArchTest
    static final ArchRule noHashBasedCollections =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .belongToAnyOf(HashMap.class, HashSet.class)
                    .because(
                            "l'ordine di iterazione delle collezioni basate sugli hash cambia tra"
                                    + " esecuzioni e tra JVM e Android");

    @ArchTest
    static final ArchRule noConsoleOutput =
            noClasses()
                    .should()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because("quello che succede nel turno lo raccontano gli eventi");

    @ArchTest
    static final ArchRule noMutableStaticState =
            fields().that()
                    .areStatic()
                    .should()
                    .beFinal()
                    .because(
                            "uno stato globale modificabile sopravvive da una partita all'altra:"
                                    + " lo stato di una partita vive solo nella sessione");

    @ArchTest
    static final ArchRule snapshotIsRecordsAndEnums =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".snapshot")
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(beRecordOrEnum())
                    .because("lo snapshot è una fotografia immutabile");

    @ArchTest
    static final ArchRule everyPackageIsNullMarked =
            classes()
                    .should(resideInANullMarkedPackage())
                    .because(
                            "con @NullMarked un tipo senza annotazione non è mai null, e NullAway"
                                    + " lo verifica durante la compilazione");

    private static ArchRule onlyDependOn(
            String packageName, String reason, boolean withKernel, String... allowed) {
        List<String> packages = new ArrayList<>(List.of("java..", "org.jspecify.annotations.."));
        packages.add(BASE + "." + packageName + "..");
        for (String other : allowed) {
            packages.add(BASE + "." + other + "..");
        }
        if (withKernel) {
            for (String kernel : KERNEL) {
                packages.add(BASE + "." + kernel + "..");
            }
        }
        return classes()
                .that()
                .resideInAPackage(BASE + "." + packageName + "..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(packages.toArray(String[]::new))
                .because(reason);
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

    private static ArchCondition<JavaClass> beRecordOrEnum() {
        return new ArchCondition<>("be a record or an enum") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean partOfEnum =
                        javaClass.getEnclosingClass().map(JavaClass::isEnum).orElse(false);
                if (!javaClass.isRecord() && !javaClass.isEnum() && !partOfEnum) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getName() + " non è un record né un enum"));
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
