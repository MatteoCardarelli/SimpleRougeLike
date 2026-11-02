package io.github.matteocardarelli.simpleroguelike.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventLogWriterTest {

    private static final EntityId A = new EntityId(1);
    private static final EntityId B = new EntityId(7);
    private static final Kind GOBLIN = Kind.of("goblin");
    private static final Position P = new Position(3, 4);
    private static final Position Q = new Position(4, 4);

    private final EventLogWriter writer = new EventLogWriter();

    @Test
    void line_eachEvent_hasItsFixedFormat() {
        assertThat(writer.lines(12, everyEvent()))
                .containsExactly(
                        "t12 Moved actor=1 kind=goblin from=3,4 to=4,4",
                        "t12 Attacked attacker=1 target=7 from=3,4 at=4,4",
                        "t12 Missed attacker=1 target=7 from=3,4 at=4,4",
                        "t12 Damaged target=7 at=3,4 amount=5 hp=20",
                        "t12 Died actor=7 kind=goblin at=3,4",
                        "t12 Healed target=1 at=3,4 amount=25",
                        "t12 ExperienceGained at=3,4 amount=25",
                        "t12 LeveledUp at=3,4 level=2",
                        "t12 ItemPickedUp item=7 kind=goblin at=3,4",
                        "t12 InventoryFull item=7 kind=goblin at=3,4",
                        "t12 ItemUsed item=7 kind=goblin at=3,4",
                        "t12 ItemEquipped item=7 kind=goblin at=3,4",
                        "t12 ItemUnequipped item=7 kind=goblin at=3,4");
    }

    @Test
    void line_unknownEvent_isRejected() {
        GameEvent unknown = List::of;

        assertThatThrownBy(() -> writer.line(1, unknown))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyGameEventInTheDomain_hasAFormat() {
        List<String> implementations =
                new ClassFileImporter()
                                .withImportOption(new ImportOption.DoNotIncludeTests())
                                .importPackages("io.github.matteocardarelli.simpleroguelike")
                                .stream()
                                .filter(c -> c.isAssignableTo(GameEvent.class) && !c.isInterface())
                                .filter(c -> !c.getName().contains("Test"))
                                .map(JavaClass::getName)
                                .toList();

        assertThat(writer.formattedTypes())
                .extracting(Class::getName)
                .containsExactlyInAnyOrderElementsOf(implementations);
    }

    @Test
    void where_eachEvent_namesItsCells() {
        assertThat(everyEvent())
                .extracting(GameEvent::where)
                .containsExactly(
                        List.of(P, Q),
                        List.of(P, Q),
                        List.of(P, Q),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P),
                        List.of(P));
    }

    private static List<GameEvent> everyEvent() {
        return List.of(
                new Moved(A, GOBLIN, P, Q),
                new Attacked(A, B, P, Q),
                new Missed(A, B, P, Q),
                new Damaged(B, P, 5, 20),
                new Died(B, GOBLIN, P),
                new Healed(A, P, 25),
                new ExperienceGained(P, 25),
                new LeveledUp(P, 2),
                new ItemPickedUp(B, GOBLIN, P),
                new InventoryFull(B, GOBLIN, P),
                new ItemUsed(B, GOBLIN, P),
                new ItemEquipped(B, GOBLIN, P),
                new ItemUnequipped(B, GOBLIN, P));
    }
}
