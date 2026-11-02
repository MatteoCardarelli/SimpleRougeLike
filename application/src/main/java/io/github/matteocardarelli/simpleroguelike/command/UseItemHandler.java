package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.Optional;

/**
 * Usa un oggetto dell'inventario, e il turno passa. Un oggetto che non è nell'inventario non fa
 * niente: l'interfaccia può aver mostrato una lista vecchia. Un oggetto da equipaggiare, da
 * lanciare o una chiave non si usano: si equipaggia con {@link EquipCommand}, si lancia con {@link
 * ThrowCommand}, una chiave la consuma la porta.
 */
public final class UseItemHandler implements CommandHandler<UseItemCommand> {

    @Override
    public Class<UseItemCommand> commandType() {
        return UseItemCommand.class;
    }

    @Override
    public CommandOutcome handle(UseItemCommand command, Run run) {
        Floor floor = run.floor();
        Optional<Item> item = floor.player().inventory().find(command.itemId());
        if (!item.isPresent() || !item.get().definition().usable()) {
            return CommandOutcome.none();
        }
        return new CommandOutcome(
                floor.useItem(floor.playerId(), command.itemId(), run.gameplay()), true);
    }
}
