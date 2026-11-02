package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Kind;

/**
 * I tipi di oggetti fissi del piano, con le loro regole: se bloccano il passaggio e la vista finché
 * sono chiusi, e dove stanno.
 */
public enum FixtureType {
    /** Si apre urtandola; chiusa blocca passaggio e vista. */
    DOOR("door", true, true, true),
    /** Lo apre la sua leva; chiuso blocca il passaggio ma non la vista. */
    GATE("gate", true, false, true),
    /** Sta nel muro: si tira urtandola, una volta sola, e apre i suoi cancelli. */
    LEVER("lever", false, false, false),
    /** Si apre urtandolo e lascia a terra quello che contiene; chiuso blocca il passaggio. */
    CHEST("chest", true, false, true);

    private final String kind;
    private final boolean blocksWhileClosed;
    private final boolean opaqueWhileClosed;
    private final boolean onFloor;

    FixtureType(
            String kind, boolean blocksWhileClosed, boolean opaqueWhileClosed, boolean onFloor) {
        this.kind = kind;
        this.blocksWhileClosed = blocksWhileClosed;
        this.opaqueWhileClosed = opaqueWhileClosed;
        this.onFloor = onFloor;
    }

    /** Il tipo per l'interfaccia, che sceglie nome e aspetto. */
    public Kind kind() {
        return Kind.of(kind);
    }

    public boolean blocksWhileClosed() {
        return blocksWhileClosed;
    }

    public boolean opaqueWhileClosed() {
        return opaqueWhileClosed;
    }

    /** Vero se sta su una cella di pavimento; falso se sta nel muro, come la leva. */
    public boolean onFloor() {
        return onFloor;
    }
}
