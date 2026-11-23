package io.github.matteocardarelli.simpleroguelike.ui.sound;

import io.github.matteocardarelli.simpleroguelike.event.Attacked;
import io.github.matteocardarelli.simpleroguelike.event.ChestOpened;
import io.github.matteocardarelli.simpleroguelike.event.CoinsPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.Damaged;
import io.github.matteocardarelli.simpleroguelike.event.Died;
import io.github.matteocardarelli.simpleroguelike.event.DoorOpened;
import io.github.matteocardarelli.simpleroguelike.event.DoorUnlocked;
import io.github.matteocardarelli.simpleroguelike.event.EnemyFell;
import io.github.matteocardarelli.simpleroguelike.event.Fired;
import io.github.matteocardarelli.simpleroguelike.event.FloorChanged;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.event.ItemPickedUp;
import io.github.matteocardarelli.simpleroguelike.event.ItemUsed;
import io.github.matteocardarelli.simpleroguelike.event.JumpAnnounced;
import io.github.matteocardarelli.simpleroguelike.event.JumpCharged;
import io.github.matteocardarelli.simpleroguelike.event.Landed;
import io.github.matteocardarelli.simpleroguelike.event.LeverPulled;
import io.github.matteocardarelli.simpleroguelike.event.Moved;
import io.github.matteocardarelli.simpleroguelike.event.Poisoned;
import io.github.matteocardarelli.simpleroguelike.event.TrapTriggered;
import io.github.matteocardarelli.simpleroguelike.event.WareTaken;
import io.github.matteocardarelli.simpleroguelike.event.WaresPaid;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Quale suono ha ogni evento: un registro per classe di evento, come il formattatore del registro
 * dei messaggi. Un evento senza voce è muto, e ogni evento ha al più un suono. I passi dei nemici
 * non suonano: sarebbero troppi.
 */
public final class SoundMap {

    /** Il suono di un evento; {@code player} è l'id del giocatore. */
    private interface Rule<E extends GameEvent> {
        Optional<Sound> apply(E event, EntityId player);
    }

    private static final Set<Kind> DRINKS =
            Set.of(Kind.of("potion"), Kind.of("greater_potion"), Kind.of("antidote"));

    private final Map<Class<? extends GameEvent>, Rule<GameEvent>> rules = new LinkedHashMap<>();

    public SoundMap() {
        register(Moved.class, (e, player) -> only(e.actor().equals(player), Sound.STEP));
        // Il colpo che subisci si sente con il danno: qui suona solo il tuo.
        register(Attacked.class, (e, player) -> only(e.attacker().equals(player), Sound.HIT));
        register(Damaged.class, (e, player) -> only(e.target().equals(player), Sound.HURT));
        register(
                Died.class,
                (e, player) -> Optional.of(e.actor().equals(player) ? Sound.DEFEAT : Sound.DEATH));
        register(DoorOpened.class, (e, player) -> Optional.of(Sound.DOOR));
        register(DoorUnlocked.class, (e, player) -> Optional.of(Sound.DOOR));
        register(LeverPulled.class, (e, player) -> Optional.of(Sound.LEVER));
        register(ChestOpened.class, (e, player) -> Optional.of(Sound.CHEST));
        register(CoinsPickedUp.class, (e, player) -> Optional.of(Sound.COINS));
        register(WaresPaid.class, (e, player) -> Optional.of(Sound.COINS));
        register(ItemPickedUp.class, (e, player) -> Optional.of(Sound.PICKUP));
        register(WareTaken.class, (e, player) -> Optional.of(Sound.PICKUP));
        register(ItemUsed.class, (e, player) -> only(DRINKS.contains(e.kind()), Sound.POTION));
        register(Fired.class, (e, player) -> Optional.of(Sound.THROW));
        register(JumpCharged.class, (e, player) -> Optional.of(Sound.CHARGE));
        register(JumpAnnounced.class, (e, player) -> Optional.of(Sound.JUMP));
        register(Landed.class, (e, player) -> Optional.of(Sound.LAND));
        register(EnemyFell.class, (e, player) -> Optional.of(Sound.FALL));
        register(FloorChanged.class, (e, player) -> Optional.of(Sound.STAIRS));
        register(TrapTriggered.class, (e, player) -> Optional.of(Sound.TRAP));
        register(Poisoned.class, (e, player) -> Optional.of(Sound.POISON));
    }

    /**
     * Il suono dell'evento.
     *
     * @return vuoto se l'evento è muto
     */
    public Optional<Sound> soundFor(GameEvent event, EntityId player) {
        Rule<GameEvent> rule = rules.get(event.getClass());
        return rule == null ? Optional.empty() : rule.apply(event, player);
    }

    private <E extends GameEvent> void register(Class<E> type, Rule<E> rule) {
        rules.put(type, (event, player) -> rule.apply(type.cast(event), player));
    }

    private static Optional<Sound> only(boolean condition, Sound sound) {
        return condition ? Optional.of(sound) : Optional.empty();
    }
}
