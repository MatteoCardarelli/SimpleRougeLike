package io.github.matteocardarelli.simpleroguelike.event;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Scrive gli eventi una riga per volta, con i campi in un ordine fisso: {@code t12 Damaged target=7
 * at=3,4 amount=5 hp=20}. Serve al test golden, che confronta il registro di una partita sulla JVM
 * e sul telefono. Non usa {@code toString()} dei record, che su Android genera il compilatore D8 e
 * potrebbe non coincidere con quello della JVM, né il nome della classe, che R8 può cambiare.
 */
public final class EventLogWriter {

    private final Map<Class<? extends GameEvent>, Function<GameEvent, String>> formats =
            new LinkedHashMap<>();

    public EventLogWriter() {
        register(
                Moved.class,
                e ->
                        "Moved actor="
                                + id(e.actor())
                                + " kind="
                                + e.kind().value()
                                + " from="
                                + at(e.from())
                                + " to="
                                + at(e.to()));
        register(
                Attacked.class,
                e ->
                        "Attacked attacker="
                                + id(e.attacker())
                                + " target="
                                + id(e.target())
                                + " from="
                                + at(e.attackerAt())
                                + " at="
                                + at(e.targetAt()));
        register(
                Fired.class,
                e ->
                        "Fired shooter="
                                + id(e.shooter())
                                + " kind="
                                + e.shooterKind().value()
                                + " projectile="
                                + e.projectile().value()
                                + " from="
                                + at(e.from())
                                + " path="
                                + path(e.path()));
        register(
                Missed.class,
                e ->
                        "Missed attacker="
                                + id(e.attacker())
                                + " target="
                                + id(e.target())
                                + " from="
                                + at(e.attackerAt())
                                + " at="
                                + at(e.targetAt()));
        register(
                Damaged.class,
                e ->
                        "Damaged target="
                                + id(e.target())
                                + " at="
                                + at(e.at())
                                + " amount="
                                + e.amount()
                                + " hp="
                                + e.remainingHealth());
        register(
                Died.class,
                e ->
                        "Died actor="
                                + id(e.actor())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(
                Healed.class,
                e ->
                        "Healed target="
                                + id(e.target())
                                + " at="
                                + at(e.at())
                                + " amount="
                                + e.amount());
        register(
                ExperienceGained.class,
                e -> "ExperienceGained at=" + at(e.at()) + " amount=" + e.amount());
        register(LeveledUp.class, e -> "LeveledUp at=" + at(e.at()) + " level=" + e.newLevel());
        register(
                ItemPickedUp.class,
                e ->
                        "ItemPickedUp item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(
                InventoryFull.class,
                e ->
                        "InventoryFull item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(
                ItemUsed.class,
                e ->
                        "ItemUsed item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(
                ItemEquipped.class,
                e ->
                        "ItemEquipped item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(
                ItemUnequipped.class,
                e ->
                        "ItemUnequipped item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(DoorOpened.class, e -> "DoorOpened at=" + at(e.at()));
        register(DoorUnlocked.class, e -> "DoorUnlocked at=" + at(e.at()));
        register(DoorLocked.class, e -> "DoorLocked at=" + at(e.at()));
        register(
                ItemDropped.class,
                e ->
                        "ItemDropped item="
                                + id(e.item())
                                + " kind="
                                + e.kind().value()
                                + " at="
                                + at(e.at()));
        register(FloorChanged.class, e -> "FloorChanged depth=" + e.depth());
        register(
                LeverPulled.class,
                e -> "LeverPulled at=" + at(e.at()) + " opened=" + path(e.opened()));
        register(
                ChestOpened.class,
                e ->
                        "ChestOpened at="
                                + at(e.at())
                                + " contents="
                                + e.contents().map(Kind::value).orElse("-"));
    }

    /**
     * Una riga per un evento.
     *
     * @throws IllegalArgumentException se l'evento non ha un formato
     */
    public String line(int turn, GameEvent event) {
        Function<GameEvent, String> format = formats.get(event.getClass());
        if (format == null) {
            throw new IllegalArgumentException("Nessun formato per " + event.getClass().getName());
        }
        return "t" + turn + " " + format.apply(event);
    }

    /** Le righe degli eventi di un turno, nello stesso ordine. */
    public List<String> lines(int turn, List<GameEvent> events) {
        List<String> lines = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            lines.add(line(turn, event));
        }
        return List.copyOf(lines);
    }

    /** I tipi di evento che hanno un formato, nell'ordine di registrazione. */
    public List<Class<? extends GameEvent>> formattedTypes() {
        return List.copyOf(formats.keySet());
    }

    private <E extends GameEvent> void register(Class<E> type, Function<E, String> format) {
        formats.put(type, event -> format.apply(type.cast(event)));
    }

    private static String id(EntityId id) {
        return Integer.toString(id.value());
    }

    private static String at(Position position) {
        return position.x() + "," + position.y();
    }

    private static String path(List<Position> cells) {
        List<String> parts = new ArrayList<>(cells.size());
        for (Position cell : cells) {
            parts.add(at(cell));
        }
        return parts.isEmpty() ? "-" : String.join(";", parts);
    }
}
