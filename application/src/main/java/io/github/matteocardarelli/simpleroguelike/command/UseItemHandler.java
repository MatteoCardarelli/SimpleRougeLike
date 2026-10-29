package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.run.Run;

/**
 * Usa un oggetto dell'inventario, e il turno passa. Un oggetto che non è nell'inventario non fa
 * niente: l'interfaccia può aver mostrato una lista vecchia.
 */
public final class UseItemHandler implements CommandHandler<UseItemCommand> {

    @Override
    public Class<UseItemCommand> commandType() {
        return UseItemCommand.class;
    }

    @Override
    public CommandOutcome handle(UseItemCommand command, Run run) {
        Floor floor = run.floor();
        if (!floor.player().inventory().find(command.itemId()).isPresent()) {
            return CommandOutcome.none();
        }
        return new CommandOutcome(
                floor.useItem(floor.playerId(), command.itemId(), run.gameplay()), true);
    }
}
