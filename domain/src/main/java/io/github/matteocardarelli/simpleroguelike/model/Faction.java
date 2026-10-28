package io.github.matteocardarelli.simpleroguelike.model;

/** Lo schieramento di un attore. Due attori sono ostili se hanno fazioni diverse. */
public enum Faction {
    PLAYER,
    MONSTERS;

    public boolean isHostileTo(Faction other) {
        return this != other;
    }
}
