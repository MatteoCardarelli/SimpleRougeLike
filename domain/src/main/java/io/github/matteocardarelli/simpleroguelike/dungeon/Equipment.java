package io.github.matteocardarelli.simpleroguelike.dungeon;

import io.github.matteocardarelli.simpleroguelike.model.EquipmentSlot;
import io.github.matteocardarelli.simpleroguelike.model.Stats;

/**
 * Come si equipaggia un oggetto: lo spazio che occupa e quanto aggiunge alle statistiche del player
 * finché lo porta.
 */
public record Equipment(EquipmentSlot slot, Stats bonus) {}
