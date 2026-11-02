package io.github.matteocardarelli.simpleroguelike.dungeon;

/**
 * Come colpisce un oggetto lanciato: il danno, prima della difesa del bersaglio, e quante celle
 * percorre al massimo in linea retta.
 */
public record Throwing(int damage, int range) {

    public Throwing {
        if (damage <= 0 || range <= 0) {
            throw new IllegalArgumentException(
                    "Danno e portata devono essere positivi: " + damage + ", " + range);
        }
    }
}
