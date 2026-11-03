package io.github.matteocardarelli.simpleroguelike.command;

import io.github.matteocardarelli.simpleroguelike.dungeon.Floor;
import io.github.matteocardarelli.simpleroguelike.dungeon.Item;
import io.github.matteocardarelli.simpleroguelike.event.GameEvent;
import io.github.matteocardarelli.simpleroguelike.run.Run;
import java.util.List;
import java.util.Optional;

/**
 * Prende la merce su cui sta il player. Il turno non passa: scegliere se prendere qualcosa non è
 * un'azione di combattimento. Se lì non c'è più quella merce non succede niente.
 */
public final class TakeWareHandler implements CommandHandler<TakeWareCommand> {

    @Override
    public Class<TakeWareCommand> commandType() {
        return TakeWareCommand.class;
    }

    @Override
    public CommandOutcome handle(TakeWareCommand command, Run run) {
        Floor floor = run.floor();
        Optional<Item> ware = floor.wareAt(floor.positionOf(floor.playerId()));
        if (!ware.isPresent() || !ware.get().id().equals(command.itemId())) {
            return CommandOutcome.none();
        }
        Optional<GameEvent> taken = floor.takeWare(floor.playerId());
        return new CommandOutcome(taken.<List<GameEvent>>map(List::of).orElse(List.of()), false);
    }
}
