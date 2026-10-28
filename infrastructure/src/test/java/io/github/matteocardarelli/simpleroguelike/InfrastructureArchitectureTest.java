package io.github.matteocardarelli.simpleroguelike;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.jspecify.annotations.NullMarked;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidmath.IRNG;

/**
 * Le regole d'architettura di :infrastructure, verificate sul bytecode del codice di produzione.
 */
@AnalyzeClasses(
        packages = "io.github.matteocardarelli.simpleroguelike",
        importOptions = InfrastructureArchitectureTest.OnlyThisModule.class)
class InfrastructureArchitectureTest {

    private static final String BASE = "io.github.matteocardarelli.simpleroguelike";

    /** Solo le classi di produzione di questo modulo, non quelle di :domain né dei test. */
    static final class OnlyThisModule implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return location.contains("infrastructure")
                    && new ImportOption.DoNotIncludeTests().includes(location);
        }
    }

    @ArchTest
    static final ArchRule noCycles =
            slices().matching(BASE + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because(
                            "un ciclo tra package rende impossibile sostituire un adapter da solo");

    @ArchTest
    static final ArchRule squidLibOnlyInItsPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage(BASE + ".squidlib..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("squidpony..")
                    .because(
                            "SquidLib non è più mantenuta: tenerla in un solo package permette di"
                                    + " sostituirla toccando solo i suoi adapter");

    @ArchTest
    static final ArchRule adaptersSeeOnlyPortsTileTypeAndValues =
            classes()
                    .should()
                    .onlyDependOnClassesThat(
                            resideInAnyPackage(
                                            "java..",
                                            "org.jspecify.annotations..",
                                            "squidpony..",
                                            BASE + ".squidlib..",
                                            BASE + ".pathfinding..",
                                            BASE + ".port..",
                                            BASE + ".model..",
                                            BASE + ".random..")
                                    .or(equivalentTo(TileType.class)))
                    .because(
                            "un adapter riceve e restituisce valori: se vedesse il piano o gli"
                                    + " attori, cambiare libreria vorrebbe dire toccare le regole");

    @ArchTest
    static final ArchRule noDijkstraMap =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .belongToAnyOf(DijkstraMap.class)
                    .because(
                            "DijkstraMap sceglie a caso tra percorsi equivalenti, anche tra due"
                                    + " chiamate identiche: il percorso lo trova"
                                    + " BreadthFirstPathfinder, deterministico per costruzione");

    @ArchTest
    static final ArchRule noSquidLibConstructorsWithHiddenRandomness =
            noClasses()
                    .should()
                    .callConstructorWhere(createsItsOwnRandomness())
                    .because(
                            "questi costruttori di SquidLib prendono il seed da Math.random(): la"
                                    + " stessa partita darebbe mappe diverse. Si passa sempre un"
                                    + " generatore con il seed preso dal flusso della partita");

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
                            "lo stesso seed deve dare la stessa partita: il caso arriva solo dal"
                                    + " flusso passato come argomento");

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
                    .because("gli adapter non scrivono log: restituiscono valori");

    @ArchTest
    static final ArchRule noMutableStaticState =
            fields().that()
                    .areStatic()
                    .should()
                    .beFinal()
                    .because("uno stato globale modificabile sopravvive da una partita all'altra")
                    // Oggi gli adapter non hanno campi statici: la regola vale per quelli futuri.
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule everyPackageIsNullMarked =
            classes()
                    .should(resideInANullMarkedPackage())
                    .because(
                            "con @NullMarked un tipo senza annotazione non è mai null, e NullAway"
                                    + " lo verifica durante la compilazione");

    private static DescribedPredicate<JavaConstructorCall> createsItsOwnRandomness() {
        return DescribedPredicate.describe(
                "un costruttore di SquidLib che crea da solo un generatore casuale",
                call -> {
                    JavaClass owner = call.getTarget().getOwner();
                    int parameters = call.getTarget().getRawParameterTypes().size();
                    boolean rngWithoutSeed = owner.isAssignableTo(IRNG.class) && parameters == 0;
                    boolean dungeonWithoutRng =
                            owner.isEquivalentTo(DungeonGenerator.class) && parameters < 3;
                    return rngWithoutSeed || dungeonWithoutRng;
                });
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
