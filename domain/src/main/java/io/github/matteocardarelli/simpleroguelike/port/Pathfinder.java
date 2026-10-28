package io.github.matteocardarelli.simpleroguelike.port;

import io.github.matteocardarelli.simpleroguelike.dungeon.TileType;
import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.Optional;

/**
 * Trova il primo passo, in 4 direzioni, da una cella verso un'altra.
 *
 * <p>Contratto: il passo porta sempre in una cella dentro il terreno, attraversabile e non
 * bloccata, oppure nella cella di arrivo; se la cella di arrivo è adiacente, il passo va lì. Il
 * risultato dipende solo dagli argomenti, anche tra percorsi equivalenti: due istanze diverse, o la
 * stessa dopo altre chiamate, scelgono lo stesso passo.
 */
public interface Pathfinder {

    /**
     * Il primo passo da {@code from} verso {@code to}.
     *
     * @param blocked le celle occupate da altri, che il percorso deve evitare; quali siano lo
     *     decide chi chiama
     * @return vuoto se {@code from} e {@code to} coincidono o se non c'è un passo possibile
     */
    Optional<Direction> nextStep(
            Grid<TileType> terrain, CellSet blocked, Position from, Position to);
}
