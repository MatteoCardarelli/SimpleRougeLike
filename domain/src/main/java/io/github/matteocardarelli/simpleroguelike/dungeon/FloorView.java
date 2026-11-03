package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.CellSet;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.model.Grid;
import io.github.matteocardarelli.simpleroguelike.model.Position;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Il piano in sola lettura: chi lo riceve può guardare ma non spostare, colpire o curare nessuno.
 *
 * <p>Un attore morto non esiste per il piano: {@link #actorAt}, {@link #isFree} ed {@link #enemies}
 * lo ignorano anche prima che venga rimosso. Restano validi solo {@link #positionOf}, {@link
 * #actor} ed {@link #enemy}, per sapere dove e chi è morto.
 */
public interface FloorView {

    Grid<TileType> terrain();

    EntityId playerId();

    /** Il player, anche se è morto. */
    Player player();

    /** I nemici vivi, in ordine di id: l'ordine in cui agiscono. */
    List<Enemy> enemies();

    /** Un attore per id, anche morto, finché non viene rimosso dal piano. */
    Optional<Actor> actor(EntityId id);

    /** Un nemico per id, anche morto, finché non viene rimosso dal piano. */
    Optional<Enemy> enemy(EntityId id);

    /**
     * La cella di un attore, anche morto, finché non viene rimosso dal piano.
     *
     * @throws IllegalArgumentException se l'attore non è sul piano
     */
    Position positionOf(EntityId id);

    /** Le celle occupate da un attore: la sua impronta a partire dalla sua posizione. */
    List<Position> cellsOf(EntityId id);

    /**
     * Vero se l'attore può avere la cella di riferimento in {@code anchor}: tutta la sua impronta
     * attraversabile e senza altri attori.
     */
    boolean canOccupy(EntityId id, Position anchor);

    /** Come {@link #canOccupy}, ma il player sotto l'impronta non conta: verrà spinto via. */
    boolean canLand(EntityId id, Position anchor);

    /** Una cella di {@code defender} accanto a una cella di {@code attacker}, se si toccano. */
    Optional<Position> contactCell(EntityId attacker, EntityId defender);

    /** Il boss del piano, vivo o sconfitto, se il piano ne ha uno. */
    Optional<EntityId> bossId();

    /** Vero se il piano ha un boss ed è morto. */
    boolean isBossDefeated();

    /** L'attore vivo nella cella, se c'è. */
    Optional<Actor> actorAt(Position position);

    Optional<Item> itemAt(Position position);

    /** Gli oggetti a terra, nell'ordine in cui sono stati messi sul piano. */
    Map<Position, Item> itemsOnGround();

    /**
     * Vero se la cella è dentro il piano, il suo terreno si può attraversare e nessun oggetto fisso
     * chiuso la blocca.
     */
    boolean isWalkable(Position position);

    Optional<Fixture> fixtureAt(Position position);

    /** Gli oggetti fissi, nell'ordine in cui sono stati messi sul piano. */
    Map<Position, Fixture> fixtures();

    /**
     * Il terreno come lo vede chi cammina: le celle bloccate da un oggetto fisso chiuso sono muri.
     * È la griglia per cercare un percorso.
     */
    Grid<TileType> movementGrid();

    /**
     * Il terreno come lo vede lo sguardo: le porte chiuse sono muri. È la griglia per il campo
     * visivo.
     */
    Grid<TileType> sightGrid();

    /** Vero se la cella si può attraversare e non c'è un attore vivo. */
    boolean isFree(Position position);

    /** Le celle che il player vede adesso. */
    CellSet visible();

    /** Tutte le celle che il player ha visto almeno una volta, comprese quelle visibili adesso. */
    CellSet explored();

    boolean isExplored(Position position);
}
