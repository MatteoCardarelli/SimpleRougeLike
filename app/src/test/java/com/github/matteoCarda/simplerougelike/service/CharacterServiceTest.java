package com.github.matteoCarda.simplerougelike.service;

import static org.junit.Assert.*;

import com.github.matteoCarda.simplerougelike.model.entity.Player;
import org.junit.Before;
import org.junit.Test;

/**
 * Questa è la classe di test per la classe CharacterService. Verifica che la logica di gestione
 * della salute funzioni come previsto.
 */
public class CharacterServiceTest {

    private CharacterService characterService;
    private Player player;

    /**
     * Il metodo con @Before viene eseguito PRIMA di ogni singolo test. È perfetto per creare uno
     * stato di partenza pulito e consistente.
     */
    @Before
    public void setUp() {
        characterService = new CharacterService();
        player = new Player(0, 0);
    }

    /**
     * @Test identifica questo metodo come un caso di test. Il nome descrive cosa stiamo
     * verificando.
     */
    @Test
    public void takeDamage_ReducesHealthCorrectly() {
        characterService.takeDamage(player, 30.0);
        assertEquals(70.0, player.getHealth(), 0.0);
    }

    @Test
    public void heal_IncreasesHealthCorrectly() {
        player.setHealth(50);
        characterService.heal(player, 20);
        assertEquals(70.0, player.getHealth(), 0.0);
    }

    @Test
    public void heal_HealthDoesNotExceedMaxHealth() {
        characterService.heal(player, 50);
        assertEquals(100.0, player.getHealth(), 0.0);
    }

    @Test
    public void takeDamage_HealthDoesNotGoBelowZero() {
        characterService.takeDamage(player, 120.0);
        assertEquals(0.0, player.getHealth(), 0.0);
    }
}
