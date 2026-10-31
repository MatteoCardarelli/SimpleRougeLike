package io.github.matteocardarelli.simpleroguelike.ui.game;

import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import java.util.List;
import java.util.Optional;

/**
 * Tutto quello che l'Activity disegna: l'ultimo risultato della partita e lo stato
 * dell'interfaccia, che vive nel ViewModel e sopravvive alla rotazione.
 *
 * @param selectedItem l'oggetto dell'inventario di cui si vede la scheda
 * @param log gli ultimi eventi visibili della partita, dal più vecchio: il registro dei messaggi
 * @param previousGameLost Android ha chiuso l'app durante una partita, che è andata persa: va detto
 *     al giocatore finché non lo conferma
 * @param logVisible il registro dei messaggi si vede: un'impostazione del giocatore
 */
public record GameUiState(
        TurnResult result,
        boolean inventoryOpen,
        Optional<EntityId> selectedItem,
        List<GameEvent> log,
        boolean previousGameLost,
        boolean logVisible) {

    public GameUiState {
        log = List.copyOf(log);
    }
}
