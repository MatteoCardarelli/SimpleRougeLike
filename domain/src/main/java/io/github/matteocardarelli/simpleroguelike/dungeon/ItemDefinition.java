package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.Kind;

/**
 * Un tipo di oggetto, condiviso da tutti gli oggetti di quel tipo.
 *
 * @param consumable se l'oggetto sparisce dopo l'uso
 * @param spawnWeight quanto spesso compare sul piano rispetto agli altri tipi; 0 vuol dire mai
 */
public record ItemDefinition(Kind kind, ItemEffect effect, boolean consumable, int spawnWeight) {

    public ItemDefinition {
        if (spawnWeight < 0) {
            throw new IllegalArgumentException("Peso di comparsa negativo: " + spawnWeight);
        }
    }
}
