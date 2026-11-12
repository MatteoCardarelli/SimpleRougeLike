package io.github.matteocardarelli.simpleroguelike.ui.log;

import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.DoorLocked;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.FloorChanged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.InventoryFull;
import io.github.matteocardarelli.simpleroguelike.event.ItemDropped;
import io.github.matteocardarelli.simpleroguelike.event.ItemEquipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUnequipped;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.JumpCharged;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.LeveledUp;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.event.MerchantAngered;
import io.github.matteocardarelli.simpleroguelike.event.MerchantGreeted;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.event.NotEnoughCoins;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Il testo di ogni evento per il registro dei messaggi. È un registro per classe di evento, non una
 * catena di {@code instanceof}: un evento nuovo si registra qui con il suo formato, oppure si
 * dichiara muto. Gli eventi che si vedono già sulla mappa, come un passo o un attacco, sono muti.
 */
public final class EventMessageFormatter {

    /**
     * Il testo di un evento; {@code player} è l'id del giocatore, per distinguere "tu" dagli altri.
     */
    private interface Format {
        String apply(GameEvent event, EntityId player);
    }

    private final Texts texts;
    private final ContentPresentation content;
    private final Map<Class<? extends GameEvent>, Format> formats = new LinkedHashMap<>();
    private final Set<Class<? extends GameEvent>> muted = new LinkedHashSet<>();

    public EventMessageFormatter(Texts texts, ContentPresentation content) {
        this.texts = texts;
        this.content = content;
        muted.add(Moved.class);
        muted.add(Attacked.class);
        register(
                Fired.class,
                (e, player) ->
                        e.shooter().equals(player)
                                ? texts.text(R.string.log_fired_player, name(e.projectile()))
                                : texts.text(R.string.log_fired_enemy, name(e.shooterKind())));
        register(
                Missed.class,
                (e, player) ->
                        e.attacker().equals(player)
                                ? texts.text(R.string.log_missed_by_player)
                                : texts.text(R.string.log_missed_player));
        register(
                Damaged.class,
                (e, player) ->
                        e.target().equals(player)
                                ? amount(R.plurals.log_damaged_player, e.amount())
                                : amount(R.plurals.log_damaged_enemy, e.amount()));
        register(
                Died.class,
                (e, player) ->
                        e.actor().equals(player)
                                ? texts.text(R.string.log_died_player)
                                : texts.text(R.string.log_died, name(e.kind())));
        register(
                Healed.class,
                (e, player) ->
                        e.target().equals(player)
                                ? amount(R.plurals.log_healed_player, e.amount())
                                : amount(R.plurals.log_healed_enemy, e.amount()));
        register(
                ExperienceGained.class,
                (e, player) -> amount(R.plurals.log_experience, e.amount()));
        register(LeveledUp.class, (e, player) -> texts.text(R.string.log_leveled_up, e.newLevel()));
        register(
                ItemPickedUp.class,
                (e, player) -> texts.text(R.string.log_item_picked_up, name(e.kind())));
        register(
                InventoryFull.class,
                (e, player) -> texts.text(R.string.log_inventory_full, name(e.kind())));
        register(ItemUsed.class, (e, player) -> texts.text(R.string.log_item_used, name(e.kind())));
        register(DoorOpened.class, (e, player) -> texts.text(R.string.log_door_opened));
        register(CoinsPickedUp.class, (e, player) -> texts.text(R.string.log_coins, e.amount()));
        register(
                WareTaken.class,
                (e, player) -> texts.text(R.string.log_ware_taken, name(e.kind()), e.price()));
        register(WaresPaid.class, (e, player) -> texts.text(R.string.log_wares_paid, e.amount()));
        register(
                NotEnoughCoins.class,
                (e, player) -> texts.text(R.string.log_not_enough_coins, e.bill(), e.coins()));
        register(MerchantGreeted.class, (e, player) -> texts.text(R.string.log_merchant_greeted));
        register(MerchantAngered.class, (e, player) -> texts.text(R.string.log_merchant_angered));
        register(
                JumpCharged.class,
                (e, player) -> texts.text(R.string.log_jump_charged, name(e.kind())));
        register(JumpAnnounced.class, (e, player) -> texts.text(R.string.log_jump_announced));
        register(Landed.class, (e, player) -> texts.text(R.string.log_landed, name(e.kind())));
        register(DoorUnlocked.class, (e, player) -> texts.text(R.string.log_door_unlocked));
        register(DoorLocked.class, (e, player) -> texts.text(R.string.log_door_locked));
        register(
                ItemDropped.class,
                (e, player) -> texts.text(R.string.log_item_dropped, name(e.kind())));
        register(
                FloorChanged.class,
                (e, player) -> texts.text(R.string.log_floor_changed, e.depth()));
        register(LeverPulled.class, (e, player) -> texts.text(R.string.log_lever_pulled));
        register(
                ChestOpened.class,
                (e, player) ->
                        e.contents()
                                .map(kind -> texts.text(R.string.log_chest_opened, name(kind)))
                                .orElseGet(() -> texts.text(R.string.log_chest_empty)));
        register(
                ItemEquipped.class,
                (e, player) -> texts.text(R.string.log_item_equipped, name(e.kind())));
        register(
                ItemUnequipped.class,
                (e, player) -> texts.text(R.string.log_item_unequipped, name(e.kind())));
    }

    /**
     * Il testo dell'evento.
     *
     * @return vuoto se l'evento è muto
     * @throws IllegalArgumentException se l'evento non ha un formato e non è muto: un evento nuovo
     *     dimenticato
     */
    public Optional<String> format(GameEvent event, EntityId player) {
        if (muted.contains(event.getClass())) {
            return Optional.empty();
        }
        Format format = formats.get(event.getClass());
        if (format == null) {
            throw new IllegalArgumentException("Nessun formato per " + event.getClass().getName());
        }
        return Optional.of(format.apply(event, player));
    }

    /** Le classi di evento con un formato o dichiarate mute. */
    public Set<Class<? extends GameEvent>> handledTypes() {
        Set<Class<? extends GameEvent>> handled = new LinkedHashSet<>(formats.keySet());
        handled.addAll(muted);
        return Collections.unmodifiableSet(handled);
    }

    private <E extends GameEvent> void register(Class<E> type, TypedFormat<E> format) {
        formats.put(type, (event, player) -> format.apply(type.cast(event), player));
    }

    private interface TypedFormat<E extends GameEvent> {
        String apply(E event, EntityId player);
    }

    private String amount(int plural, int amount) {
        return texts.quantity(plural, amount, amount);
    }

    private String name(Kind kind) {
        return texts.text(content.nameRes(kind));
    }
}
