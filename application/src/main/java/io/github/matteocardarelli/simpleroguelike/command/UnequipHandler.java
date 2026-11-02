package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.run.Run;

/**
 * Toglie un oggetto equipaggiato. Come equipaggiare, non fa passare il turno; un oggetto che non è
 * nell'inventario non fa niente.
 */
public final class UnequipHandler implements CommandHandler<UnequipCommand> {

    @Override
    public Class<UnequipCommand> commandType() {
        return UnequipCommand.class;
    }

    @Override
    public CommandOutcome handle(UnequipCommand command, Run run) {
        Floor floor = run.floor();
        if (!floor.player().inventory().find(command.itemId()).isPresent()) {
            return CommandOutcome.none();
        }
        return new CommandOutcome(floor.unequip(floor.playerId(), command.itemId()), false);
    }
}
