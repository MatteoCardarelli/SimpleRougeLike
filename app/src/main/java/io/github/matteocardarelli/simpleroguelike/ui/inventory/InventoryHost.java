package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;

/**
 * Chi ospita {@link InventorySheet}: l'Activity. Lo sheet non conosce il ViewModel, gli chiede cosa
 * mostrare e gli passa le scelte del giocatore.
 */
public interface InventoryHost {

    ContentPresentation content();

    /** Cosa mostrare adesso, per esempio quando lo sheet viene ricreato dopo una rotazione. */
    InventoryContent currentInventory();

    /** Un tocco su una casella: la seleziona e ne mostra la scheda, senza usarla. */
    void onItemSelected(EntityId item);

    /** Il pulsante "Usa": usa l'oggetto selezionato; l'inventario resta aperto. */
    void onUseSelected();

    /** Il pulsante "Lascia": lascia a terra l'oggetto selezionato; l'inventario resta aperto. */
    void onDropSelected();

    /** Un doppio tap sopra lo sheet: lo chiude, come lo apre il doppio tap sulla mappa. */
    void onInventoryToggled();

    /** Indietro, un tocco fuori o uno swipe in basso: lo chiude senza usare niente. */
    void onInventoryCancelled();
}
