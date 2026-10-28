package com.github.matteoCarda.simplerougelike.service;

import com.github.matteoCarda.simplerougelike.model.entity.Character;
import com.github.matteoCarda.simplerougelike.model.entity.Enemy;
import com.github.matteoCarda.simplerougelike.model.entity.Player;

/** Gestisce la logica di combattimento. Isola le regole di attacco e applicazione del danno. */
public class CombatService {

    private final CharacterService characterService;
    private final PlayerService playerService;

    /**
     * Costruttore.
     *
     * @param characterService Istanza di CharacterService per la manipolazione dello stato dei
     *     personaggi.
     * @param playerService Istanza di PlayerService per la gestione dell'esperienza.
     */
    public CombatService(CharacterService characterService, PlayerService playerService) {
        this.characterService = characterService;
        this.playerService = playerService;
    }

    /**
     * Esegue un attacco da un'entità (attacker) a un'altra (target). Applica il danno e, se il
     * target muore, conferisce esperienza all'attacker (se è un player).
     *
     * @param attacker L'entità che attacca.
     * @param target L'entità che subisce l'attacco.
     * @return true se il target è morto dopo l'attacco, false altrimenti.
     */
    public boolean performAttack(Character attacker, Character target) {
        if (attacker == null
                || target == null
                || !characterService.isAlive(attacker)
                || !characterService.isAlive(target)) {
            return false;
        }

        double damage = attacker.getAttackPower();
        characterService.takeDamage(target, damage);

        System.out.println(
                attacker.getClass().getSimpleName()
                        + " attacca "
                        + target.getClass().getSimpleName()
                        + " per "
                        + damage
                        + " danni. Vita rimanente: "
                        + target.getHealth());

        boolean targetDied = !characterService.isAlive(target);

        // Se il bersaglio è morto ed era un nemico attaccato da un giocatore, dai l'XP.
        if (targetDied && attacker instanceof Player && target instanceof Enemy) {
            playerService.addExperience((Player) attacker, ((Enemy) target).getExperienceValue());
            System.out.println(
                    "Hai guadagnato "
                            + ((Enemy) target).getExperienceValue()
                            + " punti esperienza!");
        }

        return targetDied;
    }
}
