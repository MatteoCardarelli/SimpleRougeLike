package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Salute massima e attacco di un attore, oppure l'aumento di entrambi a ogni livello. Nessuno dei
 * due valori è negativo.
 */
public record Stats(int maxHealth, int attack) {

    public Stats {
        if (maxHealth < 0 || attack < 0) {
            throw new IllegalArgumentException(
                    "Statistiche negative: maxHealth=" + maxHealth + ", attack=" + attack);
        }
    }

    public Stats plus(Stats bonus) {
        return new Stats(
                Math.addExact(maxHealth, bonus.maxHealth), Math.addExact(attack, bonus.attack));
    }
}
