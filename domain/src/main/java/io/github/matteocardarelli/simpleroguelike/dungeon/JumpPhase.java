package io.github.matteocardarelli.simpleroguelike.dungeon;

/**
 * Dove si trova un nemico che salta nel suo ciclo di quattro turni: fermo, carica, in aria,
 * atterraggio. Gli altri nemici restano sempre {@link #RESTING}.
 */
public enum JumpPhase {
    /** A terra, appena atterrato o in attesa del player: il prossimo turno resta fermo. */
    RESTING,
    /** A terra, dopo il turno fermo: il prossimo turno si carica. */
    READY,
    /** A terra, si vede che sta per saltare: il prossimo turno si alza in aria. */
    CHARGING,
    /**
     * In aria sopra la sua ombra: non occupa celle, non si colpisce e il prossimo turno atterra.
     */
    AIRBORNE
}
