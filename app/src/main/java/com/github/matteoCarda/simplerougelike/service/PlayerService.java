package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Player;

/** Servizio per gestire la logica specifica del giocatore, come la progressione di livello. */
public class PlayerService {

    /**
     * Aggiunge esperienza al giocatore e gestisce il passaggio di livello.
     *
     * @param player Il giocatore che guadagna esperienza.
     * @param experienceGained La quantità di esperienza guadagnata.
     */
    public void addExperience(Player player, int experienceGained) {
        if (player == null) return;

        int newExperience = player.getExperience() + experienceGained;

        // Controlla se il giocatore è salito di livello (potrebbe salire anche di più livelli in un
        // colpo solo)
        while (newExperience >= player.getExperienceToNextLevel()) {
            newExperience -= player.getExperienceToNextLevel();
            levelUp(player);
        }

        player.setExperience(newExperience);
        System.out.println("Hai guadagnato " + experienceGained + " punti esperienza!");
    }

    /**
     * Gestisce la logica del passaggio di livello.
     *
     * @param player Il giocatore che sale di livello.
     */
    private void levelUp(Player player) {
        // Aumenta il livello
        player.setLevel(player.getLevel() + 1);

        // Aumenta la salute massima e la salute attuale dello stesso importo
        player.setMaxHealth(player.getMaxHealth() + 20);
        player.setHealth(player.getHealth() + 20);

        // Aumenta la potenza d'attacco
        player.setAttackPower(player.getAttackPower() + 2);

        // Aumenta l'esperienza necessaria per il prossimo livello
        int newRequirement = (int) (player.getExperienceToNextLevel() * 1.5); // Es. aumenta del 50%
        player.setExperienceToNextLevel(newRequirement);
        System.out.println("LEVEL UP! Sei al livello " + player.getLevel());
    }
}
