package io.github.matteocardarelli.simpleroguelike.ui.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.di.TestContent;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.DoorLocked;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.DropRefused;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.FloorChanged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.ItemDropped;
import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemPutDown;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.JumpCharged;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.event.MerchantAngered;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.event.NotEnoughCoins;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
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
    void format_missed_saysWhoMissed() {
        assertThat(formatter.format(new Missed(PLAYER, GOBLIN, AT, AT), PLAYER))
                .contains(R.string.log_missed_by_player + "[]");
        assertThat(formatter.format(new Missed(GOBLIN, PLAYER, AT, AT), PLAYER))
                .contains(R.string.log_missed_player + "[]");
    }

    @Test
    void format_fired_byThePlayer_namesTheProjectile_byAnEnemy_namesTheShooter() {
        Kind knife = Kind.of("throwing_knife");
        Kind archer = Kind.of("archer");

        assertThat(
                        formatter.format(
                                new Fired(PLAYER, Kind.of("player"), knife, AT, List.of(AT)),
                                PLAYER))
                .contains(
                        R.string.log_fired_player
                                + "["
                                + R.string.kind_throwing_knife_name
                                + "[]]");
        assertThat(
                        formatter.format(
                                new Fired(GOBLIN, archer, Kind.of("arrow"), AT, List.of(AT)),
                                PLAYER))
                .contains(R.string.log_fired_enemy + "[" + R.string.kind_archer_name + "[]]");
    }

    @Test
    void format_fixtures_sayWhatHappened() {
        assertThat(formatter.format(new DoorOpened(AT), PLAYER))
                .contains(R.string.log_door_opened + "[]");
        assertThat(formatter.format(new DoorLocked(AT), PLAYER))
                .contains(R.string.log_door_locked + "[]");
        assertThat(formatter.format(new DoorUnlocked(AT), PLAYER))
                .contains(R.string.log_door_unlocked + "[]");
        assertThat(
                        formatter.format(
                                new ItemDropped(new EntityId(9), Kind.of("potion"), AT), PLAYER))
                .contains(R.string.log_item_dropped + "[" + R.string.kind_potion_name + "[]]");
        assertThat(
                        formatter.format(
                                new JumpCharged(GOBLIN, Kind.of("giant_slime"), List.of(AT)),
                                PLAYER))
                .contains(R.string.log_jump_charged + "[" + R.string.kind_giant_slime_name + "[]]");
        assertThat(formatter.format(new JumpAnnounced(GOBLIN, List.of(AT)), PLAYER))
                .contains(R.string.log_jump_announced + "[]");
        assertThat(formatter.format(new Landed(GOBLIN, Kind.of("giant_slime"), AT, AT), PLAYER))
                .contains(R.string.log_landed + "[" + R.string.kind_giant_slime_name + "[]]");
        assertThat(formatter.format(new CoinsPickedUp(15, AT), PLAYER))
                .contains(R.string.log_coins + "[15]");
        assertThat(formatter.format(new WaresPaid(60, AT), PLAYER))
                .contains(R.string.log_wares_paid + "[60]");
        assertThat(formatter.format(new NotEnoughCoins(60, 10, AT), PLAYER))
                .contains(R.string.log_not_enough_coins + "[60, 10]");
        assertThat(formatter.format(new MerchantAngered(GOBLIN, AT), PLAYER))
                .contains(R.string.log_merchant_angered + "[]");
        assertThat(formatter.format(new FloorChanged(3), PLAYER))
                .contains(R.string.log_floor_changed + "[3]");
        assertThat(formatter.format(new LeverPulled(AT, List.of(AT)), PLAYER))
                .contains(R.string.log_lever_pulled + "[]");
        assertThat(
                        formatter.format(
                                new ChestOpened(AT, java.util.Optional.of(Kind.of("potion"))),
                                PLAYER))
                .contains(R.string.log_chest_opened + "[" + R.string.kind_potion_name + "[]]");
        assertThat(formatter.format(new ChestOpened(AT, java.util.Optional.empty()), PLAYER))
                .contains(R.string.log_chest_empty + "[]");
    }

    @Test
    void format_equipment_namesTheItem() {
        String sword = R.string.kind_sword_name + "[]";
        Kind kind = Kind.of("sword");

        assertThat(formatter.format(new ItemEquipped(new EntityId(7), kind, AT), PLAYER))
                .contains(R.string.log_item_equipped + "[" + sword + "]");
        assertThat(formatter.format(new ItemUnequipped(new EntityId(7), kind, AT), PLAYER))
                .contains(R.string.log_item_unequipped + "[" + sword + "]");
    }

    @Test
    void format_dropAndRefusal_nameTheItemAndTheReason() {
        Kind kind = Kind.of("sword");

        assertThat(formatter.format(new ItemPutDown(new EntityId(7), kind, AT), PLAYER))
                .contains(R.string.log_item_put_down + "[" + R.string.kind_sword_name + "[]]");
        assertThat(
                        formatter.format(
                                new DropRefused(
                                        new EntityId(7), kind, AT, DropRefused.Reason.FIXTURE),
                                PLAYER))
                .contains(R.string.log_drop_refused_fixture + "[]");
        assertThat(
                        formatter.format(
                                new DropRefused(
                                        new EntityId(7), kind, AT, DropRefused.Reason.UNPAID),
                                PLAYER))
                .contains(R.string.log_drop_refused_unpaid + "[]");
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
