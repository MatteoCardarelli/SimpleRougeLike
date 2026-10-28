package com.github.matteoCarda.simplerougelike.service;

import static org.junit.Assert.*;

import com.github.matteoCarda.simplerougelike.model.entity.Player;
import org.junit.Before;
import org.junit.Test;

public class PlayerServiceTest {

    private PlayerService playerService;
    private Player player;

    @Before
    public void setUp() {
        playerService = new PlayerService();
        player = new Player(0, 0); // Livello 1, 0/100 XP, 100/100 HP, 10 Atk
    }

    @Test
    public void addExperience_IncreasesExperience_WithoutLevelUp() {
        playerService.addExperience(player, 50);
        assertEquals(50, player.getExperience());
        assertEquals(1, player.getLevel());
    }

    @Test
    public void addExperience_TriggersLevelUp_AndAddsHealthToCurrent() {
        // Arrange: Il giocatore ha perso vita prima di salire di livello.
        // Partiamo da 60 di vita su 100.
        player.setHealth(60);

        // Act: Aggiungiamo 120 XP, abbastanza per un level-up.
        playerService.addExperience(player, 120);

        // Assert: Verifichiamo lo stato dopo il level-up secondo la nuova regola.
        assertEquals(2, player.getLevel()); // Livello corretto
        assertEquals(20, player.getExperience()); // Esperienza rimanente corretta

        // --- VERIFICHE DELLA SALUTE CORRETTE ---
        // La vita massima deve essere aumentata di 20 (100 -> 120).
        assertEquals(120, player.getMaxHealth());
        // La salute attuale deve essere aumentata di 20 (60 -> 80).
        assertEquals(80, player.getHealth());

        assertEquals(12.0, player.getAttackPower(), 0.0); // Attacco corretto
        assertTrue(
                "Il requisito per il prossimo livello deve essere aumentato",
                player.getExperienceToNextLevel() > 100);
    }

    @Test
    public void addExperience_TriggersMultipleLevelUps() {
        // Arrange: Partiamo da 50 di vita.
        player.setHealth(50);
        // Level-up 1 (a lvl 2): +20 HP -> 70 / 120
        // Level-up 2 (a lvl 3): +20 HP -> 90 / 140

        // Act: Aggiungiamo 260 XP (abbastanza per due level-up).
        playerService.addExperience(player, 260);

        // Assert
        assertEquals(3, player.getLevel());
        assertEquals(10, player.getExperience());

        // Verifichiamo la salute dopo DUE level-up
        assertEquals(140, player.getMaxHealth()); // 100 + 20 + 20
        assertEquals(90, player.getHealth()); // 50 + 20 + 20
    }

    @Test
    public void addExperience_DoesNothing_IfPlayerIsNull() {
        try {
            playerService.addExperience(null, 50);
        } catch (Exception e) {
            fail("Il metodo non dovrebbe lanciare un'eccezione se il giocatore è null.");
        }
    }
}
