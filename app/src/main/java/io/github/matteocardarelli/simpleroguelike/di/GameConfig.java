package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import java.util.OptionalLong;

/**
 * La configurazione di una partita.
 *
 * @param visionRadius fin dove vede il player, in celle, raggio compreso
 * @param fixedSeed il seed di ogni partita nuova; vuoto per un seed casuale a ogni partita
 */
public record GameConfig(FloorSpec floorSpec, int visionRadius, OptionalLong fixedSeed) {

    /**
     * I valori del gioco di prima del refactoring: piano 50×50 con 20 nemici e 10 oggetti, campo
     * visivo di 8 celle. I nemici possono nascere in qualunque cella diversa da quella del player,
     * come prima.
     */
    public static GameConfig standard() {
        return new GameConfig(new FloorSpec(50, 50, 20, 10, 1, 10), 8, OptionalLong.empty());
    }
}
