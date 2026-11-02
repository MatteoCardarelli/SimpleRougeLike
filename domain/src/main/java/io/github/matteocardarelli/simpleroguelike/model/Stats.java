package io.github.matteocardarelli.simpleroguelike.model;

/**
 * Le statistiche di un attore, oppure l'aumento di tutte a ogni livello. Nessun valore è negativo.
 *
 * @param maxHealth la salute massima
 * @param attack il danno di un colpo, prima della difesa di chi lo riceve
 * @param defense quanto danno toglie a ogni colpo ricevuto
 * @param accuracy quanto alza la probabilità di colpire
 * @param evasion quanto abbassa la probabilità di essere colpito
 */
public record Stats(int maxHealth, int attack, int defense, int accuracy, int evasion) {

    public Stats {
        if (maxHealth < 0 || attack < 0 || defense < 0 || accuracy < 0 || evasion < 0) {
            throw new IllegalArgumentException(
                    "Statistiche negative: maxHealth="
                            + maxHealth
                            + ", attack="
                            + attack
                            + ", defense="
                            + defense
                            + ", accuracy="
                            + accuracy
                            + ", evasion="
                            + evasion);
        }
    }

    /** Solo salute e attacco: difesa, precisione e schivata a 0. */
    public Stats(int maxHealth, int attack) {
        this(maxHealth, attack, 0, 0, 0);
    }

    public Stats plus(Stats bonus) {
        return new Stats(
                Math.addExact(maxHealth, bonus.maxHealth),
                Math.addExact(attack, bonus.attack),
                Math.addExact(defense, bonus.defense),
                Math.addExact(accuracy, bonus.accuracy),
                Math.addExact(evasion, bonus.evasion));
    }
}
