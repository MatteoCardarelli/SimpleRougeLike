package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CombatServiceTest {

    // --- Novità di Mockito ---

    // @Mock crea oggetti finti (mock) di CharacterService e PlayerService.
    // I mock NON eseguono la vera logica delle classi: registrano le chiamate
    // che ricevono e rispondono quello che decidiamo noi con when(...).
    @Mock
    private CharacterService mockCharacterService;

    // CombatService riceve anche PlayerService nel costruttore:
    // senza questo mock, @InjectMocks gli passerebbe null.
    @Mock
    private PlayerService mockPlayerService;

    // @InjectMocks crea un'istanza REALE di CombatService e cerca di
    // iniettare automaticamente tutti gli oggetti annotati con @Mock
    // nei suoi campi corrispondenti (nel nostro caso, nel costruttore).
    @InjectMocks
    private CombatService combatService;

    // --- Oggetti di gioco standard ---
    private Player player;
    private Enemy enemy;

    @Before
    public void setUp() {
        // Questa riga è FONDAMENTALE. Inizializza tutti gli oggetti annotati con @Mock e @InjectMocks.
        MockitoAnnotations.openMocks(this);

        // Creiamo istanze reali per il giocatore e il nemico
        player = new Player(0, 0); // 100 HP, 10 Attack
        enemy = new Enemy(1, 1);   // 30 HP, 5 Attack

        when(mockCharacterService.isAlive(enemy)).thenReturn(true);
        when(mockCharacterService.isAlive(player)).thenReturn(true);
    }

    @Test
    public void performAttack_AttackerDamagesTarget() {
        // Arrange
        // Qui non dobbiamo fare nulla di speciale con Mockito, perché vogliamo solo
        // verificare che un metodo del nostro mock venga chiamato.
        double expectedDamage = player.getAttackPower(); // Ci aspettiamo 10 di danno

        // Act
        // Eseguiamo l'attacco dal giocatore al nemico
        combatService.performAttack(player, enemy);

        // Assert
        // Questa è la verifica più importante con Mockito:
        // verify(<oggetto_mock>).<metodo_che_doveva_essere_chiamato>(<con_quali_argomenti>);
        // Stiamo verificando che il metodo `takeDamage` del nostro `mockCharacterService`
        // sia stato chiamato ESATTAMENTE UNA VOLTA con il nemico come bersaglio e 10.0 come danno.
        verify(mockCharacterService, times(1)).takeDamage(enemy, expectedDamage);
    }

    @Test
    public void performAttack_ReturnsTrueIfTargetDies() {
        // Arrange
        // Qui istruiamo il nostro mock.
        // "QUANDO (when) viene chiamato `isAlive` sul nostro mockCharacterService
        //  con il nemico come argomento, ALLORA (thenReturn) deve restituire false."
        // Stiamo simulando lo scenario in cui il nemico muore dopo l'attacco.
        when(mockCharacterService.isAlive(enemy))
                .thenReturn(true)
                .thenReturn(false);


        // Act
        // Eseguiamo l'attacco
        boolean targetDied = combatService.performAttack(player, enemy);

        // Assert
        // Verifichiamo che il metodo `performAttack` abbia restituito true,
        // come ci aspettiamo quando il bersaglio muore.
        assertTrue(targetDied);
    }

    @Test
    public void performAttack_ReturnsFalseIfTargetSurvives() {
        // Arrange
        // Scenario opposto: simuliamo che il nemico sopravviva.

        // Act
        boolean targetDied = combatService.performAttack(player, enemy);

        // Assert
        // Verifichiamo che il metodo abbia restituito false.
        assertFalse(targetDied);
    }

    @Test
    public void performAttack_whenPlayerKillsEnemy_grantsExperience() {
        // Arrange
        // Il nemico è vivo al primo controllo e morto al secondo:
        // simuliamo che il colpo del player lo uccida.
        when(mockCharacterService.isAlive(enemy))
                .thenReturn(true)
                .thenReturn(false);

        // Act
        combatService.performAttack(player, enemy);

        // Assert
        // CombatService deve dare l'esperienza una sola volta, al player,
        // e nella quantità prevista dal nemico. Usiamo getExperienceValue()
        // invece di scrivere 25, così il test resta valido se il valore cambia.
        // Non controlliamo l'esperienza del player: PlayerService è un mock,
        // e la sua logica è già provata in PlayerServiceTest.
        verify(mockPlayerService, times(1)).addExperience(player, enemy.getExperienceValue());
    }

    @Test
    public void performAttack_whenEnemySurvives_grantsNoExperience() {
        // Arrange
        // Niente da preparare: setUp lascia il nemico vivo anche dopo il colpo.

        // Act
        combatService.performAttack(player, enemy);

        // Assert
        // Se il nemico sopravvive, addExperience non deve essere chiamato,
        // qualunque siano il player e la quantità di esperienza.
        verify(mockPlayerService, never()).addExperience(any(Player.class), anyInt());
    }
}
