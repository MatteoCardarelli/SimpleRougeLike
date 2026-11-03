package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Lo schieramento di un attore. Due attori sono ostili se hanno fazioni diverse, tranne i neutrali,
 * come un mercante, che non sono ostili a nessuno.
 */
public enum Faction {
    PLAYER,
    MONSTERS,
    NEUTRAL;

    public boolean isHostileTo(Faction other) {
        return this != other && this != NEUTRAL && other != NEUTRAL;
    }
}
