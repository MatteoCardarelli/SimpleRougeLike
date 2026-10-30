package io.github.matteocardarelli.simpleroguelike.ui.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.di.TestContent;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventMessageFormatterTest {

    private static final EntityId PLAYER = new EntityId(1);
    private static final EntityId GOBLIN = new EntityId(2);
    private static final Position AT = new Position(3, 4);

    /** Sulla JVM le risorse non ci sono: il testo è l'id della stringa con gli argomenti. */
    private static final Texts TEXTS =
            new Texts() {
                @Override
                public String text(int id, Object... args) {
                    return id + Arrays.toString(args);
                }

                @Override
                public String quantity(int id, int count, Object... args) {
                    return id + "#" + count + Arrays.toString(args);
                }
            };

    private final EventMessageFormatter formatter =
            new EventMessageFormatter(TEXTS, TestContent.presentation());

    /** Un evento nuovo senza formato non darebbe errore a runtime: lo si scopre qui. */
    @Test
    void everyEvent_hasAFormatOrIsMuted() {
        List<String> events = new ArrayList<>();
        for (JavaClass type :
                new ClassFileImporter().importPackages(GameEvent.class.getPackageName())) {
            if (type.isAssignableTo(GameEvent.class) && !type.isInterface()) {
                events.add(type.getName());
            }
        }
        List<String> handled = new ArrayList<>();
        for (Class<? extends GameEvent> type : formatter.handledTypes()) {
            handled.add(type.getName());
        }

        assertThat(events).isNotEmpty();
        assertThat(handled).containsExactlyInAnyOrderElementsOf(events);
    }

    @Test
    void format_distinguishesThePlayerFromTheOthers() {
        assertThat(formatter.format(new Damaged(PLAYER, AT, 5, 95), PLAYER))
                .contains(R.plurals.log_damaged_player + "#5[5]");
        assertThat(formatter.format(new Damaged(GOBLIN, AT, 10, 20), PLAYER))
                .contains(R.plurals.log_damaged_enemy + "#10[10]");
        assertThat(formatter.format(new Died(PLAYER, Kind.of("player"), AT), PLAYER))
                .contains(R.string.log_died_player + "[]");
    }

    @Test
    void format_namesTheKindInTheLanguageOfThePhone() {
        String potion = R.string.kind_potion_name + "[]";

        assertThat(
                        formatter.format(
                                new ItemPickedUp(new EntityId(7), Kind.of("potion"), AT), PLAYER))
                .contains(R.string.log_item_picked_up + "[" + potion + "]");
    }

    @Test
    void format_mutedEvents_giveNothing() {
        assertThat(formatter.format(new Moved(PLAYER, Kind.of("player"), AT, AT), PLAYER))
                .isEmpty();
        assertThat(formatter.format(new Attacked(PLAYER, GOBLIN, AT, AT), PLAYER)).isEmpty();
    }

    @Test
    void format_anEventWithoutAFormat_throws() {
        GameEvent unknown = () -> List.of();

        assertThatThrownBy(() -> formatter.format(unknown, PLAYER))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
