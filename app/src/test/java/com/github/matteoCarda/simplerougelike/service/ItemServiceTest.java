package com.github.matteoCarda.simplerougelike.service;

import static org.mockito.Mockito.*;

import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.model.entity.PotionItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ItemServiceTest {

    // --- Mock delle dipendenze ---
    // ItemService dipende da CharacterService per aggiungere oggetti all'inventario.
    @Mock private CharacterService mockCharacterService;

    // --- Classe sotto test ---
    // Crea un'istanza reale di ItemService e inietta il mockCharacterService.
    @InjectMocks private ItemService itemService;

    // --- Oggetti di gioco ---
    private Player player;
    private PotionItem potion;

    @Before
    public void setUp() {
        // Inizializza @Mock e @InjectMocks
        MockitoAnnotations.openMocks(this);

        // Creiamo istanze reali per il giocatore e l'oggetto
        player = new Player(0, 0);
        potion = new PotionItem(1, 1);
    }

    @Test
    public void onPickup_AddsItemToPlayerInventory() {
        // Arrange
        // Non sono necessarie istruzioni 'when' qui, perché vogliamo solo
        // verificare che un metodo del nostro mock venga chiamato.

        // Act
        // Eseguiamo l'azione di raccogliere la pozione.
        itemService.onPickup(potion, player);

        // Assert
        // Verifichiamo che il metodo `takeItem` del nostro `mockCharacterService`
        // sia stato chiamato esattamente una volta, passando la pozione e il giocatore corretti.
        // Questo conferma che ItemService delega correttamente la responsabilità.
        verify(mockCharacterService, times(1)).takeItem(player, potion);
    }

    @Test
    public void onPickup_DoesNotHealPlayer() {
        // Arrange
        // Lo stato di partenza è un giocatore con 100 HP.

        // Act
        // Eseguiamo l'azione di raccogliere la pozione.
        itemService.onPickup(potion, player);

        // Assert
        // Verifichiamo che il metodo `heal` del nostro `mockCharacterService`
        // NON sia MAI stato chiamato. Questo è un test importante per garantire
        // che la raccolta di una pozione non la consumi immediatamente.
        // `never()` è l'opposto di `times(1)`.
        verify(mockCharacterService, never()).heal(any(Player.class), anyDouble());
    }
}
