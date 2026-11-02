package io.github.matteocardarelli.simpleroguelike.ui.log;

import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.ExperienceGained;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.Healed;
import io.github.matteocardarelli.simpleroguelike.event.InventoryFull;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.LeveledUp;
import io.github.matteocardarelli.simpleroguelike.event.Missed;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
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
