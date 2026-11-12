package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.generation.FloorSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * La configurazione di una partita.
 *
 * @param floorSpecs i piani della partita, dal primo all'ultimo: si scende con le scale e si vince
 *     sull'ultimo
 * @param visionRadius fin dove vede il player, in celle, raggio compreso
 * @param fixedSeed il seed di ogni partita nuova; vuoto per un seed casuale a ogni partita
 */
public record GameConfig(List<FloorSpec> floorSpecs, int visionRadius, OptionalLong fixedSeed) {

    /** Quanti piani ha una partita. */
    public static final int FLOORS = 5;

    /** La probabilità, in percentuale, di un negozio su un piano: circa uno ogni tre o quattro. */
    public static final int SHOP_CHANCE = 30;

    /** Ogni quanti piani c'è il piano del boss: con cinque piani, è l'ultimo. */
    public static final int BOSS_EVERY = 5;

    public GameConfig {
        floorSpecs = List.copyOf(floorSpecs);
        if (floorSpecs.isEmpty()) {
            throw new IllegalArgumentException("Una partita ha almeno un piano");
        }
    }

    /**
     * Cinque piani 50×50, campo visivo di 8 celle. Ogni piano ha 10 oggetti, 6 porte di cui 2
     * chiuse a chiave, 3 scrigni e un cancello con la sua leva; i nemici partono da 14 e crescono
     * di 3 a ogni piano. Tutti tranne l'ultimo hanno le scale; ogni quinto piano ha la stanza del
     * boss, con la sua porta e la sua chiave, e niente negozio. I nemici possono nascere in
     * qualunque cella diversa da quella del player, come nel gioco di prima.
     */
    public static GameConfig standard() {
        List<FloorSpec> floors = new ArrayList<>();
        for (int depth = 1; depth <= FLOORS; depth++) {
            floors.add(
                    new FloorSpec(
                            50,
                            50,
                            14 + 3 * (depth - 1),
                            10,
                            1,
                            10,
                            6,
                            3,
                            1,
                            depth < FLOORS ? 1 : 0,
                            2,
                            depth % BOSS_EVERY == 0 ? 1 : 0,
                            depth % BOSS_EVERY == 0 ? 0 : SHOP_CHANCE,
                            depth));
        }
        return new GameConfig(floors, 8, OptionalLong.empty());
    }
}
