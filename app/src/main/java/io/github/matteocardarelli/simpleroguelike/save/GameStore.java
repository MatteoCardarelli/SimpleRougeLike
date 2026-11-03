package io.github.matteocardarelli.simpleroguelike.save;

import java.util.Optional;

/** Dove sta la partita salvata tra un avvio e l'altro dell'app: una sola, quella in corso. */
public interface GameStore {

    /** La partita salvata, se c'è e il file si legge. */
    Optional<SavedGame> load();

    /** Sostituisce la partita salvata: il prossimo {@link #load()} restituisce questa. */
    void save(SavedGame game);

    /** Cancella la partita salvata, se c'è. */
    void clear();
}
