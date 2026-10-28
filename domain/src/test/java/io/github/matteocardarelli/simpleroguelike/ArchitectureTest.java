package io.github.matteocardarelli.simpleroguelike;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
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
import io.github.matteocardarelli.simpleroguelike.dungeon.Actor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Enemy;
import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Inventory;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.dungeon.Player;
import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.jspecify.annotations.NullMarked;

/** Le regole d'architettura di :domain, verificate sul bytecode del codice di produzione. */
@AnalyzeClasses(
        packages = "io.github.matteocardarelli.simpleroguelike",
        importOptions = ArchitectureTest.OnlyProductionCode.class)
class ArchitectureTest {

    private static final String BASE = "io.github.matteocardarelli.simpleroguelike";
    private static final String[] JAVA = {"java..", "org.jspecify.annotations.."};

    /** Solo le classi di src/main: test e testFixtures possono fare quello che serve ai test. */
    static final class OnlyProductionCode implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return new ImportOption.DoNotIncludeTests().includes(location)
                    && !location.contains("testFixtures")
                    && !location.contains("test-fixtures");
        }
    }

    @ArchTest
    static final ArchRule noCycles =
            slices().matching(BASE + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because(
                            "un ciclo tra package rende impossibile capire o sostituire un pezzo"
                                    + " senza toccare gli altri");

    @ArchTest
    static final ArchRule modelUsesNothing =
            onlyDependOn("model", "i valori di base devono restare usabili da ogni package");

    @ArchTest
    static final ArchRule randomUsesNothing =
            onlyDependOn("random", "le sorgenti casuali sono di base e non conoscono il gioco");

    @ArchTest
    static final ArchRule eventUsesOnlyModel =
            onlyDependOn(
                    "event",
                    "gli eventi descrivono fatti con valori semplici, senza riferimenti ad attori"
                            + " o al piano che si possono modificare",
                    "model");

    @ArchTest
    static final ArchRule dungeonUsesOnlyTheKernel =
            onlyDependOn(
                    "dungeon",
                    "il piano sta al centro: regole, effetti e contenuti lo usano, non il"
                            + " contrario",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule rulesUseOnlyDungeonAndKernel =
            onlyDependOn(
                    "rules",
                    "le regole lavorano sul piano e non dipendono dai contenuti concreti",
                    "dungeon",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule effectsUseOnlyDungeonRulesAndKernel =
            onlyDependOn(
                    "effect",
                    "un effetto modifica il piano con i suoi metodi, e passa dalle regole per il"
                            + " danno",
                    "dungeon",
                    "rules",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule contentUsesOnlyDungeonEffectsRulesAndKernel =
            onlyDependOn(
                    "content",
                    "i cataloghi mettono insieme definizioni, effetti e regole, e nessuno dipende"
                            + " da loro dentro il dominio",
                    "dungeon",
                    "effect",
                    "rules",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule portsUseOnlyTileTypeAndKernel =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".port..")
                    .should()
                    .onlyDependOnClassesThat(
                            resideInAnyPackage(
                                            "java..",
                                            "org.jspecify.annotations..",
                                            BASE + ".port..",
                                            BASE + ".model..",
                                            BASE + ".random..")
                                    .or(equivalentTo(TileType.class)))
                    .because(
                            "le porte scambiano solo valori: un adapter non deve vedere il piano né"
                                    + " gli attori, così cambiare libreria non tocca le regole");

    @ArchTest
    static final ArchRule generationUsesOnlyPortsDungeonAndKernel =
            onlyDependOn(
                    "generation",
                    "la generazione chiede il terreno a una porta e popola il piano, senza"
                            + " conoscere regole o contenuti concreti",
                    "port",
                    "dungeon",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule aiUsesOnlyPortsDungeonAndKernel =
            onlyDependOn(
                    "ai",
                    "le strategie dei nemici decidono e basta: non conoscono le regole che"
                            + " eseguono le loro intenzioni",
                    "port",
                    "dungeon",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule runUsesOnlyGenerationDungeonAndKernel =
            onlyDependOn(
                    "run",
                    "la partita tiene lo stato e crea il piano, le regole del turno stanno"
                            + " altrove",
                    "generation",
                    "dungeon",
                    "model",
                    "event",
                    "random");

    @ArchTest
    static final ArchRule aiSeesOnlyTheReadOnlyFloor =
            noClasses()
                    .that()
                    .resideInAnyPackage(BASE + ".ai..", BASE + ".port..")
                    .should()
                    .dependOnClassesThat()
                    .belongToAnyOf(Floor.class)
                    .because(
                            "le strategie ricevono FloorView, di sola lettura: una strategia che"
                                    + " sposta o colpisce da sola aggirerebbe le regole");

    @ArchTest
    static final ArchRule intentsInterpretedOnlyBySession =
            noClasses()
                    .that()
                    .resideOutsideOfPackage(BASE + ".session..")
                    .should()
                    .callMethod(StepIntent.class, "direction")
                    .because(
                            "un'intenzione si esegue in un solo punto, con uno switch esaustivo che"
                                    + " il compilatore controlla: interpretarla altrove"
                                    + " duplicherebbe la regola");

    @ArchTest
    static final ArchRule noFrameworks =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "android..", "androidx..", "squidpony..", "dagger..", "javax.inject..")
                    .because(
                            "il dominio è Java puro: si prova sulla JVM in pochi millisecondi e"
                                    + " sopravvive a un cambio di libreria o di framework");

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
                            "lo stesso seed deve dare la stessa partita: il caso arriva solo da una"
                                    + " RandomSource passata come argomento, mai dall'orologio o"
                                    + " da un generatore nascosto");

    @ArchTest
    static final ArchRule noHashBasedCollections =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .belongToAnyOf(HashMap.class, HashSet.class)
                    .orShould()
                    .callMethodWhere(hashBasedFactory())
                    .because(
                            "l'ordine di iterazione delle collezioni basate sugli hash cambia tra"
                                    + " un'esecuzione e l'altra e tra JVM e Android, e"
                                    + " renderebbe la partita non riproducibile: si usano List,"
                                    + " LinkedHashMap, TreeMap, EnumMap e CellSet");

    @ArchTest
    static final ArchRule noInstanceofOnPolymorphicHierarchies =
            classes()
                    .should(notCheckInstanceofOf(GameEvent.class, Actor.class, EnemyIntent.class))
                    .because(
                            "gli eventi si interpretano con un registro per classe, gli attori con"
                                    + " i loro metodi e le intenzioni con uno switch esaustivo: un"
                                    + " instanceof andrebbe aggiornato a ogni tipo nuovo, e il"
                                    + " compilatore non lo segnalerebbe");

    @ArchTest
    static final ArchRule damageOnlyFromDungeonAndRules =
            noClasses()
                    .that()
                    .resideOutsideOfPackages(BASE + ".dungeon..", BASE + ".rules..")
                    .should()
                    .callMethod(Floor.class, "damage", EntityId.class, int.class)
                    .because(
                            "fuori dal piano il danno passa da DamageRules, che assegna anche"
                                    + " l'esperienza: un danno inflitto direttamente la"
                                    + " dimenticherebbe");

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
                                    + " del turno, che solo la sessione esegue e nell'ordine"
                                    + " giusto");

    @ArchTest
    static final ArchRule entitiesAreReadOnlyOutsideDungeon =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .belongToAnyOf(
                            Actor.class, Player.class, Enemy.class, Inventory.class, Item.class)
                    .and()
                    .arePublic()
                    .should()
                    .notHaveRawReturnType(void.class)
                    .andShould()
                    .haveNameNotStartingWith("set")
                    .because(
                            "fuori dal package dungeon un'entità si può solo leggere: ogni modifica"
                                    + " passa da Floor, che controlla le invarianti del piano");

    @ArchTest
    static final ArchRule noConsoleOutput =
            noClasses()
                    .should()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because(
                            "quello che succede nel gioco lo raccontano gli eventi, non la console");

    @ArchTest
    static final ArchRule noMutableStaticState =
            fields().that()
                    .areStatic()
                    .should()
                    .beFinal()
                    .because(
                            "uno stato globale modificabile sopravvive da una partita all'altra e"
                                    + " tiene in vita oggetti che dovrebbero sparire, come"
                                    + " l'Activity nel codice di prima del refactoring");

    @ArchTest
    static final ArchRule modelIsImmutableValues =
            classes()
                    .that()
                    .resideInAPackage(BASE + ".model")
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(beRecordOrEnumOr(CellSet.class))
                    .because("model contiene solo valori immutabili");

    @ArchTest
    static final ArchRule eventsAndDefinitionsAreRecords =
            classes()
                    .that()
                    .implement(GameEvent.class)
                    .or()
                    .implement(EnemyIntent.class)
                    .or()
                    .haveSimpleNameEndingWith("Definition")
                    .should(beRecordOrEnumOr())
                    .because(
                            "eventi, intenzioni e definizioni sono condivisi tra turni e oggetti"
                                    + " diversi: devono essere immutabili");

    @ArchTest
    static final ArchRule everyPackageIsNullMarked =
            classes()
                    .should(resideInANullMarkedPackage())
                    .because(
                            "con @NullMarked un tipo senza annotazione non è mai null, e NullAway"
                                    + " lo verifica durante la compilazione");

    private static ArchRule onlyDependOn(String packageName, String reason, String... allowed) {
        List<String> packages = new ArrayList<>(List.of(JAVA));
        packages.add(BASE + "." + packageName + "..");
        for (String other : allowed) {
            packages.add(BASE + "." + other + "..");
        }
        return classes()
                .that()
                .resideInAPackage(BASE + "." + packageName + "..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(packages.toArray(String[]::new))
                .because(reason);
    }

    private static DescribedPredicate<JavaMethodCall> hashBasedFactory() {
        return DescribedPredicate.describe(
                "Set.of, Map.of, i loro copyOf o un Collector che crea insiemi o mappe",
                call -> {
                    MethodCallTarget target = call.getTarget();
                    String owner = target.getOwner().getName();
                    String name = target.getName();
                    boolean setOrMap =
                            (owner.equals("java.util.Set") || owner.equals("java.util.Map"))
                                    && (name.equals("of")
                                            || name.equals("copyOf")
                                            || name.equals("ofEntries"));
                    boolean collector =
                            owner.equals("java.util.stream.Collectors")
                                    && (name.startsWith("toSet")
                                            || name.startsWith("toMap")
                                            || name.startsWith("toUnmodifiableSet")
                                            || name.startsWith("toUnmodifiableMap")
                                            || name.startsWith("groupingBy")
                                            || name.startsWith("partitioningBy"));
                    return setOrMap || collector;
                });
    }

    private static ArchCondition<JavaClass> notCheckInstanceofOf(Class<?>... hierarchies) {
        return new ArchCondition<>("not check instanceof on GameEvent or Actor") {
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

    private static ArchCondition<JavaClass> beRecordOrEnumOr(Class<?>... allowed) {
        return new ArchCondition<>("be a record or an enum") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean ok =
                        javaClass.isRecord()
                                || javaClass.isEnum()
                                || List.of(allowed).stream().anyMatch(javaClass::isEquivalentTo)
                                || isPartOfAnEnum(javaClass);
                if (!ok) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getName() + " non è un record né un enum"));
                }
            }
        };
    }

    private static boolean isPartOfAnEnum(JavaClass javaClass) {
        return javaClass.getEnclosingClass().map(JavaClass::isEnum).orElse(false);
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
