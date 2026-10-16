package com.github.matteoCarda.simplerougelike.behavior.item;

import com.github.matteoCarda.simplerougelike.model.entity.Item;
import com.github.matteoCarda.simplerougelike.model.entity.PotionItem;
import com.github.matteoCarda.simplerougelike.model.entity.Player;
import com.github.matteoCarda.simplerougelike.service.CharacterService;

public class HealBehavior implements ItemUseBehavior {

    public static final HealBehavior INSTANCE = new HealBehavior();

    private HealBehavior() {}

    @Override
    public boolean use(Item item, Player player, CharacterService characterService) {
        // Assicuriamoci che questo comportamento sia applicato al tipo di oggetto corretto
        if (item instanceof PotionItem) {
            PotionItem potion = (PotionItem) item;
            characterService.heal(player, potion.getHealingAmount());
            return true;
        }
        return false;
    }
}
    