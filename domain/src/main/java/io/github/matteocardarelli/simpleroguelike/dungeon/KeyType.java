package io.github.matteocardarelli.simpleroguelike.dungeon;

/** Che porte apre una chiave. */
public enum KeyType {
    /** Apre una porta chiusa a chiave qualunque, e si consuma. */
    COMMON,
    /** Apre solo la porta del boss. */
    BOSS
}
