package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Kind;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Optional;

/**
 * Un oggetto fisso del piano: una porta, un cancello, una leva, uno scrigno o le scale. Sta in una
 * cella e cambia solo aprendosi, una volta sola, con i metodi di {@link Floor}. La posizione non è
 * qui: la tiene il piano.
 */
public final class Fixture {

    private final FixtureType type;
    private final List<Position> linked;
    private Optional<Item> contents;
    private boolean open;

    private Fixture(FixtureType type, List<Position> linked, Optional<Item> contents) {
        this.type = type;
        this.linked = List.copyOf(linked);
        this.contents = contents;
    }

    public static Fixture door() {
        return new Fixture(FixtureType.DOOR, List.of(), Optional.empty());
    }

    public static Fixture lockedDoor() {
        return new Fixture(FixtureType.LOCKED_DOOR, List.of(), Optional.empty());
    }

    public static Fixture bossDoor() {
        return new Fixture(FixtureType.BOSS_DOOR, List.of(), Optional.empty());
    }

    public static Fixture gate() {
        return new Fixture(FixtureType.GATE, List.of(), Optional.empty());
    }

    /** Una leva che apre i cancelli nelle celle {@code gates}. */
    public static Fixture lever(List<Position> gates) {
        return new Fixture(FixtureType.LEVER, gates, Optional.empty());
    }

    /** Le scale per il piano successivo. */
    public static Fixture stairs() {
        return new Fixture(FixtureType.STAIRS, List.of(), Optional.empty());
    }

    /** Uno scrigno con dentro {@code contents}, o vuoto. */
    public static Fixture chest(Optional<Item> contents) {
        return new Fixture(FixtureType.CHEST, List.of(), contents);
    }

    public FixtureType type() {
        return type;
    }

    public Kind kind() {
        return type.kind();
    }

    /** Aperta, tirata o svuotato: vale per sempre. */
    public boolean isOpen() {
        return open;
    }

    public boolean blocksMovement() {
        return type.blocksWhileClosed() && !open;
    }

    public boolean blocksSight() {
        return type.opaqueWhileClosed() && !open;
    }

    /** Le celle dei cancelli che una leva apre. */
    public List<Position> linked() {
        return linked;
    }

    /** Quello che contiene uno scrigno ancora chiuso. */
    public Optional<Item> contents() {
        return contents;
    }

    /** Rimette lo stato aperto salvato, senza toccare il contenuto; solo per riprendere. */
    void restoreOpen() {
        open = true;
    }

    /** Apre l'oggetto e restituisce quello che conteneva. */
    Optional<Item> open() {
        open = true;
        Optional<Item> taken = contents;
        contents = Optional.empty();
        return taken;
    }
}
