package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.run.Run;

/**
 * Lascia a terra un oggetto dell'inventario. Il turno non passa, come per equipaggiare: anche un
 * rifiuto, per esempio sopra le scale, non costa niente. Un oggetto che non c'è più non fa niente:
 * l'interfaccia può aver mostrato una lista vecchia.
 */
public final class DropItemHandler implements CommandHandler<DropItemCommand> {

    @Override
    public Class<DropItemCommand> commandType() {
        return DropItemCommand.class;
    }

    @Override
    public CommandOutcome handle(DropItemCommand command, Run run) {
        Floor floor = run.floor();
        if (!floor.player().inventory().find(command.itemId()).isPresent()) {
            return CommandOutcome.none();
        }
        return new CommandOutcome(floor.drop(floor.playerId(), command.itemId()), false);
    }
}
