package io.github.matteocardarelli.simpleroguelike.ui.game;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.session.TurnResult;
import java.util.Optional;

/**
 * Tutto quello che l'Activity disegna: l'ultimo risultato della partita e lo stato
 * dell'interfaccia, che vive nel ViewModel e sopravvive alla rotazione.
 *
 * @param selectedItem l'oggetto dell'inventario di cui si vede la scheda
 * @param previousGameLost Android ha chiuso l'app durante una partita, che è andata persa: va detto
 *     al giocatore finché non lo conferma
 */
public record GameUiState(
        TurnResult result,
        boolean inventoryOpen,
        Optional<EntityId> selectedItem,
        boolean previousGameLost) {}
