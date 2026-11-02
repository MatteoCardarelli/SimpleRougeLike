package io.github.matteocardarelli.simpleroguelike.ui.input;

import io.github.matteocardarelli.simpleroguelike.command.Command;
import io.github.matteocardarelli.simpleroguelike.model.Direction;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;

/** Le azioni che il giocatore chiede all'interfaccia. */
public interface PlayerInput {

    /** Un comando di gioco, per esempio un passo. */
    void submit(Command command);

    /** Uno swipe sulla mappa: un passo, oppure il lancio se si sta mirando. */
    void swipe(Direction direction);

    /** Apre l'inventario se è chiuso, lo chiude se è aperto. */
    void toggleInventory();

    /** Usa un oggetto dell'inventario: consuma il turno e lascia l'inventario aperto. */
    void chooseItem(EntityId item);
}
